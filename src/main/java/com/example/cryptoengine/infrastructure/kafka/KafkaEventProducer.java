package com.example.cryptoengine.infrastructure.kafka;

import com.example.cryptoengine.domain.event.BalanceUpdatedEvent;
import com.example.cryptoengine.domain.event.OrderMatchedEvent;
import com.example.cryptoengine.domain.event.OrderPlacedEvent;
import com.example.cryptoengine.domain.event.TradeExecutedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for publishing domain events.
 * Handles all event types: OrderPlaced, OrderMatched, TradeExecuted, BalanceUpdated.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${crypto.kafka.topics.order-placed}")
    private String orderPlacedTopic;

    @Value("${crypto.kafka.topics.order-matched}")
    private String orderMatchedTopic;

    @Value("${crypto.kafka.topics.trade-executed}")
    private String tradeExecutedTopic;

    @Value("${crypto.kafka.topics.balance-updated}")
    private String balanceUpdatedTopic;

    /**
     * Publishes OrderPlacedEvent.
     */
    public void publishOrderPlaced(OrderPlacedEvent event) {
        final OrderPlacedEvent finalEvent = (event.getEventId() == null) 
            ? OrderPlacedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(java.time.LocalDateTime.now())
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .type(event.getType())
                .side(event.getSide())
                .baseCurrency(event.getBaseCurrency())
                .quoteCurrency(event.getQuoteCurrency())
                .price(event.getPrice())
                .quantity(event.getQuantity())
                .symbol(event.getSymbol())
                .build()
            : event;

        String key = String.valueOf(finalEvent.getOrderId());
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(orderPlacedTopic, key, finalEvent);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Published OrderPlacedEvent for order {} to topic {}", finalEvent.getOrderId(), orderPlacedTopic);
            } else {
                log.error("Failed to publish OrderPlacedEvent for order {}", finalEvent.getOrderId(), ex);
            }
        });
    }

    /**
     * Publishes OrderMatchedEvent.
     */
    public void publishOrderMatched(OrderMatchedEvent event) {
        final OrderMatchedEvent finalEvent = (event.getEventId() == null)
            ? OrderMatchedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(java.time.LocalDateTime.now())
                .orderId(event.getOrderId())
                .matchedQuantity(event.getMatchedQuantity())
                .matchedPrice(event.getMatchedPrice())
                .fullyFilled(event.isFullyFilled())
                .build()
            : event;

        String key = String.valueOf(finalEvent.getOrderId());
        kafkaTemplate.send(orderMatchedTopic, key, finalEvent)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Published OrderMatchedEvent for order {} to topic {}", finalEvent.getOrderId(), orderMatchedTopic);
                } else {
                    log.error("Failed to publish OrderMatchedEvent for order {}", finalEvent.getOrderId(), ex);
                }
            });
    }

    /**
     * Publishes TradeExecutedEvent.
     */
    public void publishTradeExecuted(TradeExecutedEvent event) {
        final TradeExecutedEvent finalEvent = (event.getEventId() == null)
            ? TradeExecutedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(java.time.LocalDateTime.now())
                .tradeId(event.getTradeId())
                .orderIdBuy(event.getOrderIdBuy())
                .orderIdSell(event.getOrderIdSell())
                .price(event.getPrice())
                .quantity(event.getQuantity())
                .baseCurrency(event.getBaseCurrency())
                .quoteCurrency(event.getQuoteCurrency())
                .symbol(event.getSymbol())
                .build()
            : event;

        String key = String.valueOf(finalEvent.getTradeId());
        kafkaTemplate.send(tradeExecutedTopic, key, finalEvent)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Published TradeExecutedEvent for trade {} to topic {}", finalEvent.getTradeId(), tradeExecutedTopic);
                } else {
                    log.error("Failed to publish TradeExecutedEvent for trade {}", finalEvent.getTradeId(), ex);
                }
            });
    }

    /**
     * Publishes BalanceUpdatedEvent.
     */
    public void publishBalanceUpdated(BalanceUpdatedEvent event) {
        final BalanceUpdatedEvent finalEvent = (event.getEventId() == null)
            ? BalanceUpdatedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(java.time.LocalDateTime.now())
                .walletId(event.getWalletId())
                .userId(event.getUserId())
                .currency(event.getCurrency())
                .newBalance(event.getNewBalance())
                .changeAmount(event.getChangeAmount())
                .reason(event.getReason())
                .build()
            : event;

        String key = finalEvent.getUserId() + ":" + finalEvent.getCurrency().name();
        kafkaTemplate.send(balanceUpdatedTopic, key, finalEvent)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Published BalanceUpdatedEvent for wallet {} to topic {}", finalEvent.getWalletId(), balanceUpdatedTopic);
                } else {
                    log.error("Failed to publish BalanceUpdatedEvent for wallet {}", finalEvent.getWalletId(), ex);
                }
            });
    }
}
