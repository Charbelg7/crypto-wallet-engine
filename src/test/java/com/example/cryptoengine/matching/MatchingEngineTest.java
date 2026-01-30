package com.example.cryptoengine.matching;

import com.example.cryptoengine.domain.Currency;
import com.example.cryptoengine.domain.OrderSide;
import com.example.cryptoengine.domain.OrderStatus;
import com.example.cryptoengine.domain.OrderType;
import com.example.cryptoengine.domain.entity.Order;
import com.example.cryptoengine.domain.entity.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive unit tests for MatchingEngine.
 */
class MatchingEngineTest {

    private OrderBookManager orderBookManager;
    private MatchingEngine matchingEngine;

    @BeforeEach
    void setUp() {
        orderBookManager = new OrderBookManager();
        matchingEngine = new MatchingEngine(orderBookManager);
    }

    @Test
    void shouldMatchLimitBuyOrderWithSellOrder() {
        // Create a SELL order at 50000
        Order sellOrder = Order.builder()
            .id(1L)
            .userId(1L)
            .type(OrderType.LIMIT)
            .side(OrderSide.SELL)
            .baseCurrency(Currency.BTC)
            .quoteCurrency(Currency.USDT)
            .price(new BigDecimal("50000"))
            .quantity(new BigDecimal("1"))
            .filledQuantity(BigDecimal.ZERO)
            .status(OrderStatus.OPEN)
            .build();

        // Add to order book
        orderBookManager.getOrderBook("BTC/USDT").addOrder(sellOrder);

        // Create a BUY order at 51000 (should match at 50000)
        Order buyOrder = Order.builder()
            .id(2L)
            .userId(2L)
            .type(OrderType.LIMIT)
            .side(OrderSide.BUY)
            .baseCurrency(Currency.BTC)
            .quoteCurrency(Currency.USDT)
            .price(new BigDecimal("51000"))
            .quantity(new BigDecimal("0.5"))
            .filledQuantity(BigDecimal.ZERO)
            .status(OrderStatus.OPEN)
            .build();

        // Match
        List<Trade> trades = matchingEngine.matchOrder(buyOrder);

        assertThat(trades).hasSize(1);
        assertThat(trades.get(0).getPrice()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(trades.get(0).getQuantity()).isEqualByComparingTo(new BigDecimal("0.5"));
        assertThat(trades.get(0).getOrderIdBuy()).isEqualTo(2L);
        assertThat(trades.get(0).getOrderIdSell()).isEqualTo(1L);
    }

    @Test
    void shouldMatchLimitSellOrderWithBuyOrder() {
        // Create a BUY order at 51000
        Order buyOrder = Order.builder()
            .id(1L)
            .userId(1L)
            .type(OrderType.LIMIT)
            .side(OrderSide.BUY)
            .baseCurrency(Currency.BTC)
            .quoteCurrency(Currency.USDT)
            .price(new BigDecimal("51000"))
            .quantity(new BigDecimal("1"))
            .filledQuantity(BigDecimal.ZERO)
            .status(OrderStatus.OPEN)
            .build();

        orderBookManager.getOrderBook("BTC/USDT").addOrder(buyOrder);

        // Create a SELL order at 50000 (should match at 51000)
        Order sellOrder = Order.builder()
            .id(2L)
            .userId(2L)
            .type(OrderType.LIMIT)
            .side(OrderSide.SELL)
            .baseCurrency(Currency.BTC)
            .quoteCurrency(Currency.USDT)
            .price(new BigDecimal("50000"))
            .quantity(new BigDecimal("0.5"))
            .filledQuantity(BigDecimal.ZERO)
            .status(OrderStatus.OPEN)
            .build();

        List<Trade> trades = matchingEngine.matchOrder(sellOrder);

        assertThat(trades).hasSize(1);
        assertThat(trades.get(0).getPrice()).isEqualByComparingTo(new BigDecimal("51000"));
        assertThat(trades.get(0).getQuantity()).isEqualByComparingTo(new BigDecimal("0.5"));
    }

    @Test
    void shouldNotMatchWhenPriceNotAcceptable() {
        // Create a SELL order at 50000
        Order sellOrder = Order.builder()
            .id(1L)
            .userId(1L)
            .type(OrderType.LIMIT)
            .side(OrderSide.SELL)
            .baseCurrency(Currency.BTC)
            .quoteCurrency(Currency.USDT)
            .price(new BigDecimal("50000"))
            .quantity(new BigDecimal("1"))
            .filledQuantity(BigDecimal.ZERO)
            .status(OrderStatus.OPEN)
            .build();

        orderBookManager.getOrderBook("BTC/USDT").addOrder(sellOrder);

        // Create a BUY order at 49000 (too low, won't match)
        Order buyOrder = Order.builder()
            .id(2L)
            .userId(2L)
            .type(OrderType.LIMIT)
            .side(OrderSide.BUY)
            .baseCurrency(Currency.BTC)
            .quoteCurrency(Currency.USDT)
            .price(new BigDecimal("49000"))
            .quantity(new BigDecimal("0.5"))
            .filledQuantity(BigDecimal.ZERO)
            .status(OrderStatus.OPEN)
            .build();

        List<Trade> trades = matchingEngine.matchOrder(buyOrder);

        assertThat(trades).isEmpty();
        // Order should be added to order book
        assertThat(orderBookManager.getOrderBook("BTC/USDT").getBestBidPrice())
            .isEqualByComparingTo(new BigDecimal("49000"));
    }

    @Test
    void shouldMatchMarketBuyOrderImmediately() {
        // Create a SELL order at 50000
        Order sellOrder = Order.builder()
            .id(1L)
            .userId(1L)
            .type(OrderType.LIMIT)
            .side(OrderSide.SELL)
            .baseCurrency(Currency.BTC)
            .quoteCurrency(Currency.USDT)
            .price(new BigDecimal("50000"))
            .quantity(new BigDecimal("1"))
            .filledQuantity(BigDecimal.ZERO)
            .status(OrderStatus.OPEN)
            .build();

        orderBookManager.getOrderBook("BTC/USDT").addOrder(sellOrder);

        // Create a MARKET BUY order
        Order marketBuyOrder = Order.builder()
            .id(2L)
            .userId(2L)
            .type(OrderType.MARKET)
            .side(OrderSide.BUY)
            .baseCurrency(Currency.BTC)
            .quoteCurrency(Currency.USDT)
            .price(null)
            .quantity(new BigDecimal("0.5"))
            .filledQuantity(BigDecimal.ZERO)
            .status(OrderStatus.OPEN)
            .build();

        List<Trade> trades = matchingEngine.matchOrder(marketBuyOrder);

        assertThat(trades).hasSize(1);
        assertThat(trades.get(0).getPrice()).isEqualByComparingTo(new BigDecimal("50000"));
    }

    @Test
    void shouldPartiallyFillOrder() {
        // Create a SELL order with quantity 1.0
        Order sellOrder = Order.builder()
            .id(1L)
            .userId(1L)
            .type(OrderType.LIMIT)
            .side(OrderSide.SELL)
            .baseCurrency(Currency.BTC)
            .quoteCurrency(Currency.USDT)
            .price(new BigDecimal("50000"))
            .quantity(new BigDecimal("1.0"))
            .filledQuantity(BigDecimal.ZERO)
            .status(OrderStatus.OPEN)
            .build();

        orderBookManager.getOrderBook("BTC/USDT").addOrder(sellOrder);

        // Create a BUY order with quantity 0.3 (partial fill)
        Order buyOrder = Order.builder()
            .id(2L)
            .userId(2L)
            .type(OrderType.LIMIT)
            .side(OrderSide.BUY)
            .baseCurrency(Currency.BTC)
            .quoteCurrency(Currency.USDT)
            .price(new BigDecimal("51000"))
            .quantity(new BigDecimal("0.3"))
            .filledQuantity(BigDecimal.ZERO)
            .status(OrderStatus.OPEN)
            .build();

        List<Trade> trades = matchingEngine.matchOrder(buyOrder);

        assertThat(trades).hasSize(1);
        assertThat(trades.get(0).getQuantity()).isEqualByComparingTo(new BigDecimal("0.3"));
        // Sell order should still be in order book with remaining quantity
        assertThat(orderBookManager.getOrderBook("BTC/USDT").getBestAskPrice())
            .isEqualByComparingTo(new BigDecimal("50000"));
    }

    @Test
    void shouldMatchMultipleOrders() {
        // Create multiple SELL orders
        Order sellOrder1 = Order.builder()
            .id(1L)
            .userId(1L)
            .type(OrderType.LIMIT)
            .side(OrderSide.SELL)
            .baseCurrency(Currency.BTC)
            .quoteCurrency(Currency.USDT)
            .price(new BigDecimal("50000"))
            .quantity(new BigDecimal("0.2"))
            .filledQuantity(BigDecimal.ZERO)
            .status(OrderStatus.OPEN)
            .build();

        Order sellOrder2 = Order.builder()
            .id(2L)
            .userId(2L)
            .type(OrderType.LIMIT)
            .side(OrderSide.SELL)
            .baseCurrency(Currency.BTC)
            .quoteCurrency(Currency.USDT)
            .price(new BigDecimal("50000"))
            .quantity(new BigDecimal("0.3"))
            .filledQuantity(BigDecimal.ZERO)
            .status(OrderStatus.OPEN)
            .build();

        orderBookManager.getOrderBook("BTC/USDT").addOrder(sellOrder1);
        orderBookManager.getOrderBook("BTC/USDT").addOrder(sellOrder2);

        // Create a BUY order that matches both
        Order buyOrder = Order.builder()
            .id(3L)
            .userId(3L)
            .type(OrderType.LIMIT)
            .side(OrderSide.BUY)
            .baseCurrency(Currency.BTC)
            .quoteCurrency(Currency.USDT)
            .price(new BigDecimal("51000"))
            .quantity(new BigDecimal("0.4"))
            .filledQuantity(BigDecimal.ZERO)
            .status(OrderStatus.OPEN)
            .build();

        List<Trade> trades = matchingEngine.matchOrder(buyOrder);

        assertThat(trades.size()).isGreaterThanOrEqualTo(1);
        // Should match against best price first
        BigDecimal totalMatched = trades.stream()
            .map(Trade::getQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalMatched).isLessThanOrEqualTo(new BigDecimal("0.4"));
    }
}
