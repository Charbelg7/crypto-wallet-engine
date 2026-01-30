package com.example.cryptoengine.domain.event;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a trade is executed between two orders.
 */
@Value
@Builder
public class TradeExecutedEvent implements DomainEvent {
    UUID eventId;
    LocalDateTime timestamp;
    Long tradeId;
    Long orderIdBuy;
    Long orderIdSell;
    BigDecimal price;
    BigDecimal quantity;
    String baseCurrency;
    String quoteCurrency;
    String symbol;

    @Override
    public String getEventType() {
        return "TradeExecutedEvent";
    }
}
