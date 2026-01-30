package com.example.cryptoengine.application.dto;

import com.example.cryptoengine.domain.Currency;
import com.example.cryptoengine.domain.OrderSide;
import com.example.cryptoengine.domain.OrderStatus;
import com.example.cryptoengine.domain.OrderType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for order response.
 */
public record OrderResponse(
    Long id,
    Long userId,
    OrderType type,
    OrderSide side,
    Currency baseCurrency,
    Currency quoteCurrency,
    String symbol,
    BigDecimal price,
    BigDecimal quantity,
    BigDecimal filledQuantity,
    BigDecimal remainingQuantity,
    OrderStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
