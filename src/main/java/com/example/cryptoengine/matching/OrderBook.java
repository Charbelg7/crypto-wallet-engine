package com.example.cryptoengine.matching;

import com.example.cryptoengine.domain.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Thread-safe in-memory order book for a trading symbol.
 * Maintains separate bid (BUY) and ask (SELL) sides.
 * 
 * Bids are sorted by price descending (best bid first), then FIFO.
 * Asks are sorted by price ascending (best ask first), then FIFO.
 */
@Slf4j
@Component
public class OrderBook {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * Bids (BUY orders): price -> list of orders at that price level.
     * Sorted by price descending (highest first).
     */
    private final ConcurrentSkipListMap<BigDecimal, List<OrderBookEntry>> bids = 
        new ConcurrentSkipListMap<>(Collections.reverseOrder());
    
    /**
     * Asks (SELL orders): price -> list of orders at that price level.
     * Sorted by price ascending (lowest first).
     */
    private final ConcurrentSkipListMap<BigDecimal, List<OrderBookEntry>> asks = 
        new ConcurrentSkipListMap<>();

    /**
     * Adds an order to the order book.
     * Thread-safe operation.
     */
    public void addOrder(Order order) {
        lock.writeLock().lock();
        try {
            OrderBookEntry entry = OrderBookEntry.fromOrder(order);
            ConcurrentSkipListMap<BigDecimal, List<OrderBookEntry>> side = 
                order.getSide() == com.example.cryptoengine.domain.OrderSide.BUY ? bids : asks;
            
            side.computeIfAbsent(order.getPrice(), k -> new ArrayList<>()).add(entry);
            log.debug("Added order {} to {} side at price {}", order.getId(), order.getSide(), order.getPrice());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes an order from the order book.
     * Thread-safe operation.
     */
    public void removeOrder(Order order) {
        lock.writeLock().lock();
        try {
            ConcurrentSkipListMap<BigDecimal, List<OrderBookEntry>> side = 
                order.getSide() == com.example.cryptoengine.domain.OrderSide.BUY ? bids : asks;
            
            List<OrderBookEntry> priceLevel = side.get(order.getPrice());
            if (priceLevel != null) {
                priceLevel.removeIf(entry -> entry.getOrderId().equals(order.getId()));
                if (priceLevel.isEmpty()) {
                    side.remove(order.getPrice());
                }
            }
            log.debug("Removed order {} from {} side", order.getId(), order.getSide());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Updates an order's quantity in the order book.
     * Used when an order is partially filled.
     */
    public void updateOrderQuantity(Order order) {
        lock.writeLock().lock();
        try {
            removeOrder(order);
            if (order.getStatus() != com.example.cryptoengine.domain.OrderStatus.FILLED && 
                order.getStatus() != com.example.cryptoengine.domain.OrderStatus.CANCELLED) {
                addOrder(order);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets the best bid price (highest buy price).
     */
    public BigDecimal getBestBidPrice() {
        lock.readLock().lock();
        try {
            return bids.isEmpty() ? null : bids.firstKey();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the best ask price (lowest sell price).
     */
    public BigDecimal getBestAskPrice() {
        lock.readLock().lock();
        try {
            return asks.isEmpty() ? null : asks.firstKey();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets all bid orders at or better than the given price.
     * Returns orders sorted by best price first, then FIFO.
     */
    public List<OrderBookEntry> getMatchingBids(BigDecimal maxPrice) {
        lock.readLock().lock();
        try {
            return bids.entrySet().stream()
                .filter(entry -> entry.getKey().compareTo(maxPrice) >= 0)
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets all ask orders at or better than the given price.
     * Returns orders sorted by best price first, then FIFO.
     */
    public List<OrderBookEntry> getMatchingAsks(BigDecimal minPrice) {
        lock.readLock().lock();
        try {
            return asks.entrySet().stream()
                .filter(entry -> entry.getKey().compareTo(minPrice) <= 0)
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets a snapshot of the order book (bids and asks).
     * Returns a map with "bids" and "asks" keys.
     */
    public Map<String, List<OrderBookEntry>> getSnapshot() {
        lock.readLock().lock();
        try {
            Map<String, List<OrderBookEntry>> snapshot = new HashMap<>();
            snapshot.put("bids", bids.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList()));
            snapshot.put("asks", asks.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList()));
            return snapshot;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the depth of the order book (number of price levels).
     */
    public int getDepth() {
        lock.readLock().lock();
        try {
            return bids.size() + asks.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Clears the entire order book (useful for testing).
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            bids.clear();
            asks.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
