package com.example.cryptoengine.service;

import com.example.cryptoengine.application.dto.OrderResponse;
import com.example.cryptoengine.application.dto.PlaceOrderRequest;
import com.example.cryptoengine.application.mapper.OrderMapper;
import com.example.cryptoengine.domain.Currency;
import com.example.cryptoengine.domain.OrderSide;
import com.example.cryptoengine.domain.OrderStatus;
import com.example.cryptoengine.domain.OrderType;
import com.example.cryptoengine.domain.entity.Order;
import com.example.cryptoengine.domain.entity.Trade;
import com.example.cryptoengine.infrastructure.kafka.KafkaEventProducer;
import com.example.cryptoengine.infrastructure.repository.OrderRepository;
import com.example.cryptoengine.infrastructure.repository.TradeRepository;
import com.example.cryptoengine.matching.MatchingEngine;
import com.example.cryptoengine.matching.OrderBookManager;
import com.example.cryptoengine.risk.RiskEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private MatchingEngine matchingEngine;

    @Mock
    private RiskEngine riskEngine;

    @Mock
    private OrderBookManager orderBookManager;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private TradeExecutionService tradeExecutionService;

    @Mock
    private KafkaEventProducer eventProducer;

    @InjectMocks
    private OrderService orderService;

    private Long userId;
    private PlaceOrderRequest limitBuyRequest;
    private PlaceOrderRequest marketBuyRequest;
    private Order savedOrder;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        userId = 1L;
        
        limitBuyRequest = new PlaceOrderRequest(
            OrderType.LIMIT,
            OrderSide.BUY,
            Currency.BTC,
            Currency.USDT,
            new BigDecimal("50000"),
            new BigDecimal("0.1"),
            null
        );
        
        marketBuyRequest = new PlaceOrderRequest(
            OrderType.MARKET,
            OrderSide.BUY,
            Currency.BTC,
            Currency.USDT,
            null,
            new BigDecimal("0.1"),
            null
        );
        
        savedOrder = Order.builder()
            .id(1L)
            .userId(userId)
            .type(OrderType.LIMIT)
            .side(OrderSide.BUY)
            .baseCurrency(Currency.BTC)
            .quoteCurrency(Currency.USDT)
            .price(new BigDecimal("50000"))
            .quantity(new BigDecimal("0.1"))
            .filledQuantity(BigDecimal.ZERO)
            .status(OrderStatus.OPEN)
            .build();
        
        orderResponse = new OrderResponse(
            1L, userId, OrderType.LIMIT, OrderSide.BUY,
            Currency.BTC, Currency.USDT, "BTC/USDT",
            new BigDecimal("50000"), new BigDecimal("0.1"),
            BigDecimal.ZERO, new BigDecimal("0.1"),
            OrderStatus.OPEN, null, null
        );
    }

    @Test
    void shouldPlaceLimitOrderSuccessfully() {
        // Given
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        doNothing().when(riskEngine).validateOrder(any());
        doNothing().when(tradeExecutionService).reserveBalanceForOrder(any());
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(matchingEngine.matchOrder(any(Order.class))).thenReturn(Collections.emptyList());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);

        // When
        OrderResponse result = orderService.placeOrder(userId, limitBuyRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.type()).isEqualTo(OrderType.LIMIT);
        assertThat(result.status()).isEqualTo(OrderStatus.OPEN);
        
        verify(riskEngine).validateOrder(any(Order.class));
        verify(tradeExecutionService).reserveBalanceForOrder(any(Order.class));
        verify(orderRepository).save(any(Order.class));
        verify(matchingEngine).matchOrder(any(Order.class));
        verify(eventProducer).publishOrderPlaced(any());
    }

    @Test
    void shouldRejectMarketOrderWithPrice() {
        // Given
        PlaceOrderRequest invalidRequest = new PlaceOrderRequest(
            OrderType.MARKET,
            OrderSide.BUY,
            Currency.BTC,
            Currency.USDT,
            new BigDecimal("50000"), // Price should be null for MARKET
            new BigDecimal("0.1"),
            null
        );

        // When/Then
        assertThatThrownBy(() -> orderService.placeOrder(userId, invalidRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("MARKET orders cannot have a price");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldRejectLimitOrderWithoutPrice() {
        // Given
        PlaceOrderRequest invalidRequest = new PlaceOrderRequest(
            OrderType.LIMIT,
            OrderSide.BUY,
            Currency.BTC,
            Currency.USDT,
            null, // Price required for LIMIT
            new BigDecimal("0.1"),
            null
        );

        // When/Then
        assertThatThrownBy(() -> orderService.placeOrder(userId, invalidRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("LIMIT orders must have a price");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldHandleIdempotencyKey() {
        // Given
        String idempotencyKey = "test-key-123";
        PlaceOrderRequest requestWithKey = new PlaceOrderRequest(
            OrderType.LIMIT,
            OrderSide.BUY,
            Currency.BTC,
            Currency.USDT,
            new BigDecimal("50000"),
            new BigDecimal("0.1"),
            idempotencyKey
        );
        
        when(orderRepository.findByIdempotencyKey(idempotencyKey))
            .thenReturn(Optional.of(savedOrder));

        // When/Then
        assertThatThrownBy(() -> orderService.placeOrder(userId, requestWithKey))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        verify(orderRepository).findByIdempotencyKey(idempotencyKey);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldExecuteTradesWhenMatched() {
        // Given
        Trade trade = Trade.builder()
            .id(1L)
            .orderIdBuy(1L)
            .orderIdSell(2L)
            .price(new BigDecimal("50000"))
            .quantity(new BigDecimal("0.1"))
            .baseCurrency("BTC")
            .quoteCurrency("USDT")
            .build();
        
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        doNothing().when(riskEngine).validateOrder(any());
        doNothing().when(tradeExecutionService).reserveBalanceForOrder(any());
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(matchingEngine.matchOrder(any(Order.class))).thenReturn(List.of(trade));
        when(tradeRepository.save(any(Trade.class))).thenReturn(trade);
        doNothing().when(tradeExecutionService).executeTrade(any(Trade.class), any(Order.class));
        
        Order filledOrder = Order.builder()
            .id(1L)
            .userId(userId)
            .type(OrderType.LIMIT)
            .side(OrderSide.BUY)
            .baseCurrency(Currency.BTC)
            .quoteCurrency(Currency.USDT)
            .price(new BigDecimal("50000"))
            .quantity(new BigDecimal("0.1"))
            .filledQuantity(new BigDecimal("0.1"))
            .status(OrderStatus.FILLED)
            .build();
        
        lenient().when(orderRepository.findById(1L)).thenReturn(Optional.of(filledOrder));
        lenient().when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);

        // When
        OrderResponse result = orderService.placeOrder(userId, limitBuyRequest);

        // Then
        assertThat(result).isNotNull();
        verify(tradeRepository).save(any(Trade.class));
        verify(tradeExecutionService).executeTrade(any(Trade.class), any(Order.class));
    }

    @Test
    void shouldCancelOrderSuccessfully() {
        // Given
        com.example.cryptoengine.matching.OrderBook mockOrderBook = mock(com.example.cryptoengine.matching.OrderBook.class);
        when(orderRepository.findByIdAndUserId(1L, userId))
            .thenReturn(Optional.of(savedOrder));
        doNothing().when(tradeExecutionService).releaseReservedBalance(any(Order.class));
        when(orderBookManager.getOrderBook(anyString())).thenReturn(mockOrderBook);
        doNothing().when(mockOrderBook).removeOrder(any(Order.class));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);

        // When
        OrderResponse result = orderService.cancelOrder(userId, 1L);

        // Then
        assertThat(result).isNotNull();
        verify(tradeExecutionService).releaseReservedBalance(savedOrder);
        verify(orderRepository).save(savedOrder);
        
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shouldThrowExceptionWhenCancellingFilledOrder() {
        // Given
        Order filledOrder = Order.builder()
            .id(1L)
            .userId(userId)
            .status(OrderStatus.FILLED)
            .build();
        
        when(orderRepository.findByIdAndUserId(1L, userId))
            .thenReturn(Optional.of(filledOrder));

        // When/Then
        assertThatThrownBy(() -> orderService.cancelOrder(userId, 1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cannot be cancelled");

        verify(orderRepository, never()).save(any());
    }
}
