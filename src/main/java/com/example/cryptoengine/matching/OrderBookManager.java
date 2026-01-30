package com.example.cryptoengine.matching;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages multiple order books, one per trading symbol.
 * Thread-safe singleton that provides access to symbol-specific order books.
 */
@Slf4j
@Component
public class OrderBookManager {

    private final ConcurrentMap<String, OrderBook> orderBooks = new ConcurrentHashMap<>();

    /**
     * Gets or creates an order book for the given symbol.
     */
    public OrderBook getOrderBook(String symbol) {
        return orderBooks.computeIfAbsent(symbol, s -> {
            log.info("Created new order book for symbol: {}", s);
            return new OrderBook();
        });
    }

    /**
     * Removes an order book (useful for cleanup or testing).
     */
    public void removeOrderBook(String symbol) {
        orderBooks.remove(symbol);
        log.info("Removed order book for symbol: {}", symbol);
    }

    /**
     * Gets all active symbols.
     */
    public java.util.Set<String> getActiveSymbols() {
        return orderBooks.keySet();
    }
}
