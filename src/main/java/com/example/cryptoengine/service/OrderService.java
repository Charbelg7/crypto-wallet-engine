package com.example.cryptoengine.service;

import com.example.cryptoengine.application.dto.OrderResponse;
import com.example.cryptoengine.application.dto.PlaceOrderRequest;
import com.example.cryptoengine.application.mapper.OrderMapper;
import com.example.cryptoengine.domain.OrderStatus;
import com.example.cryptoengine.domain.OrderType;
import com.example.cryptoengine.domain.entity.Order;
import com.example.cryptoengine.domain.entity.Trade;
import com.example.cryptoengine.domain.event.OrderPlacedEvent;
import com.example.cryptoengine.infrastructure.kafka.KafkaEventProducer;
import com.example.cryptoengine.infrastructure.repository.OrderRepository;
import com.example.cryptoengine.infrastructure.repository.TradeRepository;
import com.example.cryptoengine.matching.MatchingEngine;
import com.example.cryptoengine.matching.OrderBookManager;
import com.example.cryptoengine.risk.RiskEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for order management and execution.
 * Coordinates risk checks, matching engine, and balance updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final MatchingEngine matchingEngine;
    private final RiskEngine riskEngine;
    private final OrderBookManager orderBookManager;
    private final OrderMapper orderMapper;
    private final TradeExecutionService tradeExecutionService;
    private final KafkaEventProducer eventProducer;

    /**
     * Places a new order.
     * Performs risk checks, saves order, matches against order book, and executes trades.
     * 
     * @param userId User ID placing the order
     * @param request Order request
     * @return Created order response
     */
    @Transactional
    public OrderResponse placeOrder(Long userId, PlaceOrderRequest request) {
        // Check idempotency
        if (request.idempotencyKey() != null && !request.idempotencyKey().isEmpty()) {
            orderRepository.findByIdempotencyKey(request.idempotencyKey())
                .ifPresent(existing -> {
                    log.info("Duplicate order request detected, returning existing order: {}", existing.getId());
                    throw new IllegalArgumentException("Order with this idempotency key already exists");
                });
        }

        // Validate MARKET orders don't have price
        if (request.type() == OrderType.MARKET && request.price() != null) {
            throw new IllegalArgumentException("MARKET orders cannot have a price");
        }

        // Validate LIMIT orders have price
        if (request.type() == OrderType.LIMIT && request.price() == null) {
            throw new IllegalArgumentException("LIMIT orders must have a price");
        }

        // Create order entity
        Order order = Order.builder()
            .userId(userId)
            .type(request.type())
            .side(request.side())
            .baseCurrency(request.baseCurrency())
            .quoteCurrency(request.quoteCurrency())
            .price(request.price())
            .quantity(request.quantity())
            .filledQuantity(java.math.BigDecimal.ZERO)
            .status(OrderStatus.OPEN)
            .idempotencyKey(request.idempotencyKey())
            .build();

        // Risk validation
        riskEngine.validateOrder(order);

        // Reserve balance (withdraw quote currency for BUY, base currency for SELL)
        tradeExecutionService.reserveBalanceForOrder(order);

        // Save order
        order = orderRepository.save(order);
        log.info("Placed order {}: {} {} {} @ {} (user {})", 
            order.getId(), order.getSide(), order.getQuantity(), 
            order.getBaseCurrency(), order.getPrice(), userId);

        // Publish OrderPlacedEvent
        OrderPlacedEvent placedEvent = OrderPlacedEvent.builder()
            .eventId(UUID.randomUUID())
            .timestamp(java.time.LocalDateTime.now())
            .orderId(order.getId())
            .userId(order.getUserId())
            .type(order.getType())
            .side(order.getSide())
            .baseCurrency(order.getBaseCurrency())
            .quoteCurrency(order.getQuoteCurrency())
            .price(order.getPrice())
            .quantity(order.getQuantity())
            .symbol(order.getSymbol())
            .build();
        eventProducer.publishOrderPlaced(placedEvent);

        // Match order against order book
        List<Trade> trades = matchingEngine.matchOrder(order);

        // Execute trades and update balances
        if (!trades.isEmpty()) {
            for (Trade trade : trades) {
                trade = tradeRepository.save(trade);
                tradeExecutionService.executeTrade(trade, order);
            }
            
            // Refresh order from DB to get updated status
            order = orderRepository.findById(order.getId())
                .orElseThrow(() -> new IllegalStateException("Order not found after execution"));
        }

        return orderMapper.toResponse(order);
    }

    /**
     * Cancels an open order.
     */
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!order.canCancel()) {
            throw new IllegalStateException("Order cannot be cancelled in status: " + order.getStatus());
        }

        // Release reserved balance
        tradeExecutionService.releaseReservedBalance(order);

        // Remove from order book
        orderBookManager.getOrderBook(order.getSymbol()).removeOrder(order);

        // Cancel order
        order.cancel();
        order = orderRepository.save(order);

        log.info("Cancelled order {} (user {})", orderId, userId);
        return orderMapper.toResponse(order);
    }

    /**
     * Gets order by ID.
     */
    public OrderResponse getOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        return orderMapper.toResponse(order);
    }

    /**
     * Gets all orders for a user.
     */
    public List<OrderResponse> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId).stream()
            .map(orderMapper::toResponse)
            .collect(Collectors.toList());
    }
}
