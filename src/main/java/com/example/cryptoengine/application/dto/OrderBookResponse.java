package com.example.cryptoengine.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for order book snapshot response.
 */
public record OrderBookResponse(
    String symbol,
    List<OrderBookLevel> bids,
    List<OrderBookLevel> asks
) {
    public record OrderBookLevel(
        BigDecimal price,
        BigDecimal quantity
    ) {}
}
