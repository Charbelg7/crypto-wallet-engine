package com.example.cryptoengine.application.dto;

import com.example.cryptoengine.domain.Currency;
import java.math.BigDecimal;

/**
 * DTO for wallet balance response.
 */
public record WalletBalanceResponse(
    Long walletId,
    Currency currency,
    BigDecimal balance
) {}
