package com.example.cryptoengine.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO for wallet deposit request.
 */
public record DepositRequest(
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.00000001", message = "Amount must be greater than 0")
    BigDecimal amount,
    
    String idempotencyKey
) {}
