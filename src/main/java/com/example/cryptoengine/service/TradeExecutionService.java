package com.example.cryptoengine.service;

import com.example.cryptoengine.domain.Currency;
import com.example.cryptoengine.domain.OrderSide;
import com.example.cryptoengine.domain.entity.Order;
import com.example.cryptoengine.domain.entity.Trade;
import com.example.cryptoengine.domain.entity.Wallet;
import com.example.cryptoengine.domain.event.BalanceUpdatedEvent;
import com.example.cryptoengine.domain.event.OrderMatchedEvent;
import com.example.cryptoengine.domain.event.TradeExecutedEvent;
import com.example.cryptoengine.infrastructure.kafka.KafkaEventProducer;
import com.example.cryptoengine.infrastructure.repository.OrderRepository;
import com.example.cryptoengine.infrastructure.repository.WalletRepository;
import com.example.cryptoengine.matching.OrderBookManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service responsible for executing trades and updating balances atomically.
 * Ensures balance consistency and order state updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeExecutionService {

    private final WalletRepository walletRepository;
    private final OrderRepository orderRepository;
    private final OrderBookManager orderBookManager;
    private final KafkaEventProducer eventProducer;

    /**
     * Reserves balance for an order (locks funds).
     * For BUY: reserves quote currency
     * For SELL: reserves base currency
     */
    @Transactional
    public void reserveBalanceForOrder(Order order) {
        Currency currency;
        BigDecimal amount;

        if (order.getSide() == OrderSide.BUY) {
            currency = order.getQuoteCurrency();
            if (order.getType() == com.example.cryptoengine.domain.OrderType.MARKET) {
                // For market orders, reserve maximum possible (will be adjusted on execution)
                // Use a conservative estimate
                amount = order.getQuantity().multiply(new BigDecimal("1000000")); // Very high estimate
            } else {
                amount = order.getQuantity().multiply(order.getPrice());
            }
        } else {
            currency = order.getBaseCurrency();
            amount = order.getQuantity();
        }

        Wallet wallet = walletRepository.findByUserIdAndCurrency(order.getUserId(), currency)
            .orElseThrow(() -> new IllegalStateException(
                String.format("Wallet not found: user %d, currency %s", order.getUserId(), currency)));

        wallet.withdraw(amount);
        walletRepository.save(wallet);

        log.debug("Reserved {} {} for order {}", amount, currency, order.getId());
    }

    /**
     * Releases reserved balance when order is cancelled.
     */
    @Transactional
    public void releaseReservedBalance(Order order) {
        Currency currency;
        BigDecimal amount;

        if (order.getSide() == OrderSide.BUY) {
            currency = order.getQuoteCurrency();
            BigDecimal remainingQuantity = order.getRemainingQuantity();
            if (order.getType() == com.example.cryptoengine.domain.OrderType.MARKET) {
                // For market orders, we need to calculate actual reserved amount
                // This is simplified - in production, we'd track reserved amounts separately
                amount = order.getQuantity().multiply(new BigDecimal("1000000"));
            } else {
                amount = remainingQuantity.multiply(order.getPrice());
            }
        } else {
            currency = order.getBaseCurrency();
            amount = order.getRemainingQuantity();
        }

        Wallet wallet = walletRepository.findByUserIdAndCurrency(order.getUserId(), currency)
            .orElseThrow(() -> new IllegalStateException(
                String.format("Wallet not found: user %d, currency %s", order.getUserId(), currency)));

        wallet.deposit(amount);
        walletRepository.save(wallet);

        log.debug("Released {} {} for cancelled order {}", amount, currency, order.getId());
    }

    /**
     * Executes a trade and updates balances for both parties.
     * Atomic operation ensuring consistency.
     */
    @Transactional
    public void executeTrade(Trade trade, Order currentOrder) {
        // Get both orders
        Order buyOrder = trade.getOrderIdBuy().equals(currentOrder.getId()) 
            ? currentOrder 
            : orderRepository.findById(trade.getOrderIdBuy())
                .orElseThrow(() -> new IllegalStateException("Buy order not found: " + trade.getOrderIdBuy()));

        Order sellOrder = trade.getOrderIdSell().equals(currentOrder.getId())
            ? currentOrder
            : orderRepository.findById(trade.getOrderIdSell())
                .orElseThrow(() -> new IllegalStateException("Sell order not found: " + trade.getOrderIdSell()));

        // Update order fills
        buyOrder.fill(trade.getQuantity());
        sellOrder.fill(trade.getQuantity());

        // Publish OrderMatchedEvent for both orders
        publishOrderMatchedEvent(buyOrder, trade.getQuantity(), trade.getPrice());
        publishOrderMatchedEvent(sellOrder, trade.getQuantity(), trade.getPrice());

        // Update balances atomically
        // Buyer: receives base currency, pays quote currency
        updateBalanceForTrade(buyOrder.getUserId(), 
            Currency.valueOf(trade.getBaseCurrency()), 
            trade.getQuantity(), 
            true);

        BigDecimal quoteAmount = trade.getQuantity().multiply(trade.getPrice());
        updateBalanceForTrade(buyOrder.getUserId(),
            Currency.valueOf(trade.getQuoteCurrency()),
            quoteAmount,
            false);

        // Seller: receives quote currency, pays base currency
        updateBalanceForTrade(sellOrder.getUserId(),
            Currency.valueOf(trade.getBaseCurrency()),
            trade.getQuantity(),
            false);

        updateBalanceForTrade(sellOrder.getUserId(),
            Currency.valueOf(trade.getQuoteCurrency()),
            quoteAmount,
            true);

        // Save orders
        orderRepository.save(buyOrder);
        orderRepository.save(sellOrder);

        // Update order book if orders are still open
        String symbol = trade.getBaseCurrency() + "/" + trade.getQuoteCurrency();
        if (buyOrder.getStatus() != com.example.cryptoengine.domain.OrderStatus.FILLED &&
            buyOrder.getStatus() != com.example.cryptoengine.domain.OrderStatus.CANCELLED) {
            orderBookManager.getOrderBook(symbol).updateOrderQuantity(buyOrder);
        } else {
            orderBookManager.getOrderBook(symbol).removeOrder(buyOrder);
        }

        if (sellOrder.getStatus() != com.example.cryptoengine.domain.OrderStatus.FILLED &&
            sellOrder.getStatus() != com.example.cryptoengine.domain.OrderStatus.CANCELLED) {
            orderBookManager.getOrderBook(symbol).updateOrderQuantity(sellOrder);
        } else {
            orderBookManager.getOrderBook(symbol).removeOrder(sellOrder);
        }

        log.info("Executed trade {}: {} {} @ {} (buy: {}, sell: {})",
            trade.getId(), trade.getQuantity(), trade.getBaseCurrency(), 
            trade.getPrice(), buyOrder.getId(), sellOrder.getId());

        // Publish TradeExecutedEvent
        TradeExecutedEvent tradeEvent = TradeExecutedEvent.builder()
            .eventId(UUID.randomUUID())
            .timestamp(java.time.LocalDateTime.now())
            .tradeId(trade.getId())
            .orderIdBuy(trade.getOrderIdBuy())
            .orderIdSell(trade.getOrderIdSell())
            .price(trade.getPrice())
            .quantity(trade.getQuantity())
            .baseCurrency(trade.getBaseCurrency())
            .quoteCurrency(trade.getQuoteCurrency())
            .symbol(trade.getBaseCurrency() + "/" + trade.getQuoteCurrency())
            .build();
        eventProducer.publishTradeExecuted(tradeEvent);
    }

    /**
     * Publishes OrderMatchedEvent for an order.
     */
    private void publishOrderMatchedEvent(Order order, BigDecimal matchedQuantity, BigDecimal matchedPrice) {
        OrderMatchedEvent event = OrderMatchedEvent.builder()
            .eventId(UUID.randomUUID())
            .timestamp(java.time.LocalDateTime.now())
            .orderId(order.getId())
            .matchedQuantity(matchedQuantity)
            .matchedPrice(matchedPrice)
            .fullyFilled(order.isFilled())
            .build();
        eventProducer.publishOrderMatched(event);
    }

    /**
     * Updates balance for a trade execution.
     * 
     * @param userId User ID
     * @param currency Currency to update
     * @param amount Amount to add/subtract
     * @param isCredit true to add (credit), false to subtract (debit)
     */
    private void updateBalanceForTrade(Long userId, Currency currency, BigDecimal amount, boolean isCredit) {
        Wallet wallet = walletRepository.findByUserIdAndCurrency(userId, currency)
            .orElseGet(() -> {
                Wallet newWallet = Wallet.builder()
                    .userId(userId)
                    .currency(currency)
                    .balance(BigDecimal.ZERO)
                    .build();
                return walletRepository.save(newWallet);
            });

        BigDecimal oldBalance = wallet.getBalance();
        
        if (isCredit) {
            wallet.deposit(amount);
        } else {
            // For debits, we're releasing reserved balance, so we don't withdraw again
            // The balance was already withdrawn when order was placed
        }

        wallet = walletRepository.save(wallet);

        // Publish BalanceUpdatedEvent
        BalanceUpdatedEvent balanceEvent = BalanceUpdatedEvent.builder()
            .eventId(UUID.randomUUID())
            .timestamp(java.time.LocalDateTime.now())
            .walletId(wallet.getId())
            .userId(userId)
            .currency(currency)
            .newBalance(wallet.getBalance())
            .changeAmount(isCredit ? amount : amount.negate())
            .reason("TRADE_EXECUTION")
            .build();
        eventProducer.publishBalanceUpdated(balanceEvent);
    }
}
