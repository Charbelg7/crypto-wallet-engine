package com.example.cryptoengine.matching;

import com.example.cryptoengine.domain.entity.Order;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents an entry in the order book.
 * Immutable value object for thread-safe order book operations.
 */
@Value
public class OrderBookEntry implements Comparable<OrderBookEntry> {
    Long orderId;
    BigDecimal price;
    BigDecimal quantity;
    LocalDateTime timestamp;

    /**
     * Creates an OrderBookEntry from an Order.
     */
    public static OrderBookEntry fromOrder(Order order) {
        return new OrderBookEntry(
            order.getId(),
            order.getPrice(),
            order.getRemainingQuantity(),
            order.getCreatedAt()
        );
    }

    /**
     * For BUY orders: higher price = higher priority (best bid first).
     * For SELL orders: lower price = higher priority (best ask first).
     * 
     * This implementation is for BUY side (bids).
     * For SELL side, we'll use a separate comparator.
     */
    @Override
    public int compareTo(OrderBookEntry other) {
        // First compare by price (descending for bids, ascending for asks)
        int priceCompare = this.price.compareTo(other.price);
        if (priceCompare != 0) {
            return -priceCompare; // Negative for descending order (highest price first)
        }
        // Then by timestamp (earlier orders have priority - FIFO)
        return this.timestamp.compareTo(other.timestamp);
    }

    /**
     * Creates a comparator for SELL orders (asks).
     * Lower price has priority, then FIFO.
     */
    public static int compareAsks(OrderBookEntry a, OrderBookEntry b) {
        int priceCompare = a.price.compareTo(b.price);
        if (priceCompare != 0) {
            return priceCompare; // Ascending order (lowest price first)
        }
        return a.timestamp.compareTo(b.timestamp);
    }
}
