package com.example.cryptoengine.domain;

/**
 * Order type enumeration.
 */
public enum OrderType {
    LIMIT,  // Limit order: executes at specified price or better
    MARKET  // Market order: executes immediately at best available price
}
