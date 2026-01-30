package com.example.cryptoengine.domain.event;

import com.example.cryptoengine.domain.Currency;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a wallet balance is updated.
 */
@Value
@Builder
public class BalanceUpdatedEvent implements DomainEvent {
    UUID eventId;
    LocalDateTime timestamp;
    Long walletId;
    Long userId;
    Currency currency;
    BigDecimal newBalance;
    BigDecimal changeAmount; // positive for deposit, negative for withdrawal
    String reason; // e.g., "DEPOSIT", "TRADE_BUY", "TRADE_SELL", "ORDER_CANCELLED"

    @Override
    public String getEventType() {
        return "BalanceUpdatedEvent";
    }
}
