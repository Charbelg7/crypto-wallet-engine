package com.example.cryptoengine.domain.event;

import com.example.cryptoengine.domain.Currency;
import com.example.cryptoengine.domain.OrderSide;
import com.example.cryptoengine.domain.OrderType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a new order is placed.
 */
@Value
@Builder
public class OrderPlacedEvent implements DomainEvent {
    UUID eventId;
    LocalDateTime timestamp;
    Long orderId;
    Long userId;
    OrderType type;
    OrderSide side;
    Currency baseCurrency;
    Currency quoteCurrency;
    BigDecimal price; // null for MARKET orders
    BigDecimal quantity;
    String symbol;

    @Override
    public String getEventType() {
        return "OrderPlacedEvent";
    }
}
