package com.example.cryptoengine.infrastructure.kafka;

import com.example.cryptoengine.domain.event.TradeExecutedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for trade execution events.
 * Processes TradeExecutedEvent.
 */
@Slf4j
@Component
public class TradeEventConsumer {

    @KafkaListener(topics = "${crypto.kafka.topics.trade-executed}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTradeExecuted(
            @Payload TradeExecutedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        try {
            log.info("Received TradeExecutedEvent: tradeId={}, symbol={}, price={}, quantity={}, buyOrder={}, sellOrder={}",
                event.getTradeId(), event.getSymbol(), event.getPrice(), 
                event.getQuantity(), event.getOrderIdBuy(), event.getOrderIdSell());

            // In a real system:
            // - Update trade history cache
            // - Update market data feeds
            // - Send real-time trade notifications
            // - Update analytics/statistics
            // - Trigger settlement processes

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing TradeExecutedEvent: {}", event.getTradeId(), e);
        }
    }
}
