package com.example.cryptoengine.matching;

import com.example.cryptoengine.domain.Currency;
import com.example.cryptoengine.domain.OrderSide;
import com.example.cryptoengine.domain.OrderStatus;
import com.example.cryptoengine.domain.OrderType;
import com.example.cryptoengine.domain.entity.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OrderBookTest {

    private OrderBook orderBook;

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook();
    }

    @Test
    void shouldAddOrderToOrderBook() {
        // Given
        Order buyOrder = createOrder(1L, OrderSide.BUY, new BigDecimal("50000"), new BigDecimal("0.1"));

        // When
        orderBook.addOrder(buyOrder);

        // Then
        BigDecimal bestBid = orderBook.getBestBidPrice();
        assertThat(bestBid).isEqualByComparingTo(new BigDecimal("50000"));
    }

    @Test
    void shouldGetBestBidPrice() {
        // Given
        orderBook.addOrder(createOrder(1L, OrderSide.BUY, new BigDecimal("50000"), new BigDecimal("0.1")));
        orderBook.addOrder(createOrder(2L, OrderSide.BUY, new BigDecimal("51000"), new BigDecimal("0.2")));

        // When
        BigDecimal bestBid = orderBook.getBestBidPrice();

        // Then
        assertThat(bestBid).isEqualByComparingTo(new BigDecimal("51000")); // Higher price is better for bids
    }

    @Test
    void shouldGetBestAskPrice() {
        // Given
        orderBook.addOrder(createOrder(1L, OrderSide.SELL, new BigDecimal("50000"), new BigDecimal("0.1")));
        orderBook.addOrder(createOrder(2L, OrderSide.SELL, new BigDecimal("49000"), new BigDecimal("0.2")));

        // When
        BigDecimal bestAsk = orderBook.getBestAskPrice();

        // Then
        assertThat(bestAsk).isEqualByComparingTo(new BigDecimal("49000")); // Lower price is better for asks
    }

    @Test
    void shouldRemoveOrderFromOrderBook() {
        // Given
        Order order = createOrder(1L, OrderSide.BUY, new BigDecimal("50000"), new BigDecimal("0.1"));
        orderBook.addOrder(order);

        // When
        orderBook.removeOrder(order);

        // Then
        assertThat(orderBook.getBestBidPrice()).isNull();
    }

    @Test
    void shouldUpdateOrderQuantity() {
        // Given
        Order order = createOrder(1L, OrderSide.BUY, new BigDecimal("50000"), new BigDecimal("0.1"));
        orderBook.addOrder(order);
        
        // Partially fill order
        order.fill(new BigDecimal("0.05"));

        // When
        orderBook.updateOrderQuantity(order);

        // Then
        Map<String, List<OrderBookEntry>> snapshot = orderBook.getSnapshot();
        List<OrderBookEntry> bids = snapshot.get("bids");
        assertThat(bids).hasSize(1);
        assertThat(bids.get(0).getQuantity()).isEqualByComparingTo(new BigDecimal("0.05"));
    }

    @Test
    void shouldGetMatchingBids() {
        // Given
        orderBook.addOrder(createOrder(1L, OrderSide.BUY, new BigDecimal("50000"), new BigDecimal("0.1")));
        orderBook.addOrder(createOrder(2L, OrderSide.BUY, new BigDecimal("51000"), new BigDecimal("0.2")));
        orderBook.addOrder(createOrder(3L, OrderSide.BUY, new BigDecimal("49000"), new BigDecimal("0.3")));

        // When
        List<OrderBookEntry> matchingBids = orderBook.getMatchingBids(new BigDecimal("50000"));

        // Then
        assertThat(matchingBids).hasSize(2); // Only bids >= 50000
        assertThat(matchingBids.get(0).getPrice()).isEqualByComparingTo(new BigDecimal("51000"));
    }

    @Test
    void shouldGetMatchingAsks() {
        // Given
        orderBook.addOrder(createOrder(1L, OrderSide.SELL, new BigDecimal("50000"), new BigDecimal("0.1")));
        orderBook.addOrder(createOrder(2L, OrderSide.SELL, new BigDecimal("49000"), new BigDecimal("0.2")));
        orderBook.addOrder(createOrder(3L, OrderSide.SELL, new BigDecimal("51000"), new BigDecimal("0.3")));

        // When
        List<OrderBookEntry> matchingAsks = orderBook.getMatchingAsks(new BigDecimal("50000"));

        // Then
        assertThat(matchingAsks).hasSize(2); // Only asks <= 50000
        assertThat(matchingAsks.get(0).getPrice()).isEqualByComparingTo(new BigDecimal("49000"));
    }

    @Test
    void shouldGetSnapshot() {
        // Given
        orderBook.addOrder(createOrder(1L, OrderSide.BUY, new BigDecimal("50000"), new BigDecimal("0.1")));
        orderBook.addOrder(createOrder(2L, OrderSide.SELL, new BigDecimal("51000"), new BigDecimal("0.2")));

        // When
        Map<String, List<OrderBookEntry>> snapshot = orderBook.getSnapshot();

        // Then
        assertThat(snapshot).containsKeys("bids", "asks");
        assertThat(snapshot.get("bids")).hasSize(1);
        assertThat(snapshot.get("asks")).hasSize(1);
    }

    @Test
    void shouldClearOrderBook() {
        // Given
        orderBook.addOrder(createOrder(1L, OrderSide.BUY, new BigDecimal("50000"), new BigDecimal("0.1")));
        orderBook.addOrder(createOrder(2L, OrderSide.SELL, new BigDecimal("51000"), new BigDecimal("0.2")));

        // When
        orderBook.clear();

        // Then
        assertThat(orderBook.getBestBidPrice()).isNull();
        assertThat(orderBook.getBestAskPrice()).isNull();
        assertThat(orderBook.getDepth()).isEqualTo(0);
    }

    @Test
    void shouldReturnNullWhenNoBids() {
        // When
        BigDecimal bestBid = orderBook.getBestBidPrice();

        // Then
        assertThat(bestBid).isNull();
    }

    @Test
    void shouldReturnNullWhenNoAsks() {
        // When
        BigDecimal bestAsk = orderBook.getBestAskPrice();

        // Then
        assertThat(bestAsk).isNull();
    }

    private Order createOrder(Long id, OrderSide side, BigDecimal price, BigDecimal quantity) {
        return Order.builder()
            .id(id)
            .userId(1L)
            .type(OrderType.LIMIT)
            .side(side)
            .baseCurrency(Currency.BTC)
            .quoteCurrency(Currency.USDT)
            .price(price)
            .quantity(quantity)
            .filledQuantity(BigDecimal.ZERO)
            .status(OrderStatus.OPEN)
            .build();
    }
}
