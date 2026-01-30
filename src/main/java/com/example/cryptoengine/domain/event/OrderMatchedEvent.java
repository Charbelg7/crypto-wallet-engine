package com.example.cryptoengine.domain.event;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when an order is matched (partially or fully).
 */
@Value
@Builder
public class OrderMatchedEvent implements DomainEvent {
    UUID eventId;
    LocalDateTime timestamp;
    Long orderId;
    BigDecimal matchedQuantity;
    BigDecimal matchedPrice;
    boolean fullyFilled;

    @Override
    public String getEventType() {
        return "OrderMatchedEvent";
    }
}
