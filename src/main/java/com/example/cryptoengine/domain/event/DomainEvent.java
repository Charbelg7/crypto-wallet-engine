package com.example.cryptoengine.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base interface for all domain events.
 * Events are immutable and represent something that happened in the system.
 */
public interface DomainEvent {
    UUID getEventId();
    LocalDateTime getTimestamp();
    String getEventType();
}
