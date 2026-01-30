package com.example.cryptoengine.infrastructure.kafka;

import com.example.cryptoengine.domain.event.BalanceUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for balance update events.
 * Processes BalanceUpdatedEvent.
 */
@Slf4j
@Component
public class BalanceEventConsumer {

    @KafkaListener(topics = "${crypto.kafka.topics.balance-updated}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeBalanceUpdated(
            @Payload BalanceUpdatedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        try {
            log.info("Received BalanceUpdatedEvent: walletId={}, userId={}, currency={}, newBalance={}, change={}, reason={}",
                event.getWalletId(), event.getUserId(), event.getCurrency(), 
                event.getNewBalance(), event.getChangeAmount(), event.getReason());

            // In a real system:
            // - Update balance cache
            // - Send real-time balance updates via WebSocket
            // - Update account statements
            // - Trigger notifications for large changes

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing BalanceUpdatedEvent: {}", event.getWalletId(), e);
        }
    }
}
