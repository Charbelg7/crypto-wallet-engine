package com.example.cryptoengine.application.dto;

import com.example.cryptoengine.domain.Currency;
import com.example.cryptoengine.domain.OrderSide;
import com.example.cryptoengine.domain.OrderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO for placing a new order.
 */
public record PlaceOrderRequest(
    @NotNull(message = "Order type is required")
    OrderType type,
    
    @NotNull(message = "Order side is required")
    OrderSide side,
    
    @NotNull(message = "Base currency is required")
    Currency baseCurrency,
    
    @NotNull(message = "Quote currency is required")
    Currency quoteCurrency,
    
    // Price is required for LIMIT orders, optional for MARKET orders
    BigDecimal price,
    
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.00000001", message = "Quantity must be greater than 0")
    BigDecimal quantity,
    
    String idempotencyKey
) {}
