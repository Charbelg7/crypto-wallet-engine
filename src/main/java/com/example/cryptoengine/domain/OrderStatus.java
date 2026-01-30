package com.example.cryptoengine.domain;

/**
 * Order status enumeration.
 */
public enum OrderStatus {
    OPEN,        // Order is placed and waiting in order book
    PARTIAL,     // Order is partially filled
    FILLED,      // Order is completely filled
    CANCELLED    // Order is cancelled
}
