package com.example.cryptoengine.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for trade response.
 */
public record TradeResponse(
    Long id,
    Long orderIdBuy,
    Long orderIdSell,
    BigDecimal price,
    BigDecimal quantity,
    String baseCurrency,
    String quoteCurrency,
    String symbol,
    LocalDateTime timestamp
) {}
