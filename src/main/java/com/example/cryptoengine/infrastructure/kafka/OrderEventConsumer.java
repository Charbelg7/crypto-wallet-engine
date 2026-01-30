package com.example.cryptoengine.infrastructure.kafka;

import com.example.cryptoengine.domain.event.OrderMatchedEvent;
import com.example.cryptoengine.domain.event.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for order-related events.
 * Processes OrderPlacedEvent and OrderMatchedEvent.
 * In a production system, this would update read models, send notifications, etc.
 */
@Slf4j
@Component
public class OrderEventConsumer {

    @KafkaListener(topics = "${crypto.kafka.topics.order-placed}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeOrderPlaced(
            @Payload OrderPlacedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        try {
            log.info("Received OrderPlacedEvent: orderId={}, userId={}, symbol={}, side={}, quantity={}, price={}",
                event.getOrderId(), event.getUserId(), event.getSymbol(), 
                event.getSide(), event.getQuantity(), event.getPrice());

            // In a real system, here you would:
            // - Update read models (e.g., user's order history)
            // - Send real-time notifications via WebSocket
            // - Update analytics/cache
            // - Trigger downstream processes

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing OrderPlacedEvent: {}", event.getOrderId(), e);
            // In production, implement retry logic or dead letter queue
        }
    }

    @KafkaListener(topics = "${crypto.kafka.topics.order-matched}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeOrderMatched(
            @Payload OrderMatchedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        try {
            log.info("Received OrderMatchedEvent: orderId={}, matchedQuantity={}, matchedPrice={}, fullyFilled={}",
                event.getOrderId(), event.getMatchedQuantity(), event.getMatchedPrice(), event.isFullyFilled());

            // Update read models, send notifications, etc.

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing OrderMatchedEvent: {}", event.getOrderId(), e);
        }
    }
}
