# Kafka Events Documentation

Complete documentation of the event-driven architecture and Kafka topics.

## Overview

The system uses Apache Kafka for event-driven updates, enabling:
- **Decoupled Components**: Producers don't know about consumers
- **Scalability**: Multiple consumers can process events independently
- **Reliability**: Kafka provides durability and replay capabilities
- **Real-time Updates**: Enables real-time notifications and read model updates

## Event Types

### 1. OrderPlacedEvent

**Topic:** `order-placed-events`

**Published When:** A new order is placed and saved to the database.

**Event Structure:**
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-15T10:30:00",
  "orderId": 123,
  "userId": 1,
  "type": "LIMIT",
  "side": "BUY",
  "baseCurrency": "BTC",
  "quoteCurrency": "USDT",
  "price": 50000.00,
  "quantity": 0.1,
  "symbol": "BTC/USDT"
}
```

**Fields:**
- `eventId`: Unique event identifier (UUID)
- `timestamp`: Event timestamp
- `orderId`: Order ID
- `userId`: User who placed the order
- `type`: Order type (`LIMIT` or `MARKET`)
- `side`: Order side (`BUY` or `SELL`)
- `baseCurrency`: Base currency (e.g., `BTC`)
- `quoteCurrency`: Quote currency (e.g., `USDT`)
- `price`: Limit price (null for MARKET orders)
- `quantity`: Order quantity
- `symbol`: Trading symbol (e.g., `BTC/USDT`)

**Producer:** `KafkaEventProducer.publishOrderPlaced()`

**Consumer:** `OrderEventConsumer.consumeOrderPlaced()`

**Use Cases:**
- Update user's order history cache
- Send real-time notifications
- Update analytics dashboards
- Trigger downstream processes

---

### 2. OrderMatchedEvent

**Topic:** `order-matched-events`

**Published When:** An order is matched (partially or fully filled).

**Event Structure:**
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440001",
  "timestamp": "2024-01-15T10:30:05",
  "orderId": 123,
  "matchedQuantity": 0.1,
  "matchedPrice": 50000.00,
  "fullyFilled": true
}
```

**Fields:**
- `eventId`: Unique event identifier (UUID)
- `timestamp`: Event timestamp
- `orderId`: Order ID that was matched
- `matchedQuantity`: Quantity that was matched
- `matchedPrice`: Price at which order was matched
- `fullyFilled`: Whether order is fully filled

**Producer:** `KafkaEventProducer.publishOrderMatched()`

**Consumer:** `OrderEventConsumer.consumeOrderMatched()`

**Use Cases:**
- Update order status in read models
- Send order fill notifications
- Update user's portfolio
- Trigger settlement processes

---

### 3. TradeExecutedEvent

**Topic:** `trade-executed-events`

**Published When:** A trade is executed between two orders.

**Event Structure:**
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440002",
  "timestamp": "2024-01-15T10:30:05",
  "tradeId": 456,
  "orderIdBuy": 123,
  "orderIdSell": 124,
  "price": 50000.00,
  "quantity": 0.1,
  "baseCurrency": "BTC",
  "quoteCurrency": "USDT",
  "symbol": "BTC/USDT"
}
```

**Fields:**
- `eventId`: Unique event identifier (UUID)
- `timestamp`: Event timestamp
- `tradeId`: Trade ID
- `orderIdBuy`: Buy order ID
- `orderIdSell`: Sell order ID
- `price`: Execution price
- `quantity`: Trade quantity
- `baseCurrency`: Base currency
- `quoteCurrency`: Quote currency
- `symbol`: Trading symbol

**Producer:** `KafkaEventProducer.publishTradeExecuted()`

**Consumer:** `TradeEventConsumer.consumeTradeExecuted()`

**Use Cases:**
- Update trade history cache
- Update market data feeds
- Send real-time trade notifications
- Update analytics/statistics
- Trigger settlement processes

---

### 4. BalanceUpdatedEvent

**Topic:** `balance-updated-events`

**Published When:** A wallet balance is updated (deposit, withdrawal, trade execution).

**Event Structure:**
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440003",
  "timestamp": "2024-01-15T10:30:05",
  "walletId": 789,
  "userId": 1,
  "currency": "USDT",
  "newBalance": 9500.00,
  "changeAmount": -500.00,
  "reason": "TRADE_EXECUTION"
}
```

**Fields:**
- `eventId`: Unique event identifier (UUID)
- `timestamp`: Event timestamp
- `walletId`: Wallet ID
- `userId`: User ID
- `currency`: Currency code
- `newBalance`: New balance after update
- `changeAmount`: Amount changed (positive for deposit, negative for withdrawal)
- `reason`: Reason for update (`DEPOSIT`, `TRADE_EXECUTION`, `ORDER_CANCELLED`)

**Producer:** `KafkaEventProducer.publishBalanceUpdated()`

**Consumer:** `BalanceEventConsumer.consumeBalanceUpdated()`

**Use Cases:**
- Update balance cache
- Send real-time balance updates via WebSocket
- Update account statements
- Trigger notifications for large changes
- Update portfolio value

---

## Kafka Configuration

### Producer Configuration

```properties
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.producer.acks=all
spring.kafka.producer.retries=3
spring.kafka.producer.properties.enable.idempotence=true
```

**Key Settings:**
- **Acks=all**: Wait for all replicas to acknowledge (highest durability)
- **Idempotence=true**: Prevent duplicate messages
- **Retries=3**: Retry failed sends

### Consumer Configuration

```properties
spring.kafka.consumer.group-id=crypto-wallet-engine-group
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.properties.spring.json.trusted.packages=*
spring.kafka.listener.ack-mode=manual
```

**Key Settings:**
- **Group ID**: Consumer group for load balancing
- **Auto Offset Reset**: Start from earliest if no offset exists
- **Ack Mode**: Manual acknowledgment for reliability

### Topic Configuration

Topics are auto-created with default settings. For production, create topics manually:

```bash
# Create order-placed-events topic
kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --topic order-placed-events \
  --partitions 3 \
  --replication-factor 1 \
  --config retention.ms=604800000

# Create order-matched-events topic
kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --topic order-matched-events \
  --partitions 3 \
  --replication-factor 1

# Create trade-executed-events topic
kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --topic trade-executed-events \
  --partitions 3 \
  --replication-factor 1

# Create balance-updated-events topic
kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --topic balance-updated-events \
  --partitions 3 \
  --replication-factor 1
```

**Partitioning Strategy:**
- **Order Events**: Partition by `orderId` (key)
- **Trade Events**: Partition by `tradeId` (key)
- **Balance Events**: Partition by `userId:currency` (key)

## Event Flow

### Order Placement Flow

```
1. OrderService.placeOrder()
   ↓
2. OrderRepository.save(order)
   ↓
3. KafkaEventProducer.publishOrderPlaced(event)
   ↓
4. Kafka Topic: order-placed-events
   ↓
5. OrderEventConsumer.consumeOrderPlaced()
   ↓
6. Update read models / Send notifications
```

### Trade Execution Flow

```
1. MatchingEngine.matchOrder()
   ↓
2. TradeExecutionService.executeTrade()
   ↓
3. KafkaEventProducer.publishTradeExecuted(event)
   ↓
4. KafkaEventProducer.publishOrderMatched(event) [x2]
   ↓
5. KafkaEventProducer.publishBalanceUpdated(event) [x4]
   ↓
6. Kafka Topics
   ↓
7. Consumers process events
```

## Consumer Implementation

### Order Event Consumer

```java
@KafkaListener(topics = "${crypto.kafka.topics.order-placed}")
public void consumeOrderPlaced(
        @Payload OrderPlacedEvent event,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        Acknowledgment acknowledgment) {
    try {
        // Process event
        log.info("Received OrderPlacedEvent: orderId={}", event.getOrderId());
        
        // Update read models, send notifications, etc.
        
        acknowledgment.acknowledge();
    } catch (Exception e) {
        log.error("Error processing OrderPlacedEvent", e);
        // Don't acknowledge - will be retried
    }
}
```

### Error Handling

**Current Implementation:**
- Log errors
- Don't acknowledge on failure (allows retry)

**Production Enhancements:**
- Dead letter queue for failed messages
- Retry with exponential backoff
- Alerting on repeated failures
- Circuit breaker pattern

## Monitoring

### Key Metrics

- **Producer Metrics:**
  - Messages sent per second
  - Send errors
  - Latency (p50, p95, p99)

- **Consumer Metrics:**
  - Messages consumed per second
  - Consumer lag
  - Processing errors

- **Topic Metrics:**
  - Message rate
  - Partition sizes
  - Retention

### Kafka Tools

```bash
# List topics
kafka-topics.sh --list --bootstrap-server localhost:9092

# Describe topic
kafka-topics.sh --describe --topic order-placed-events --bootstrap-server localhost:9092

# Consumer group status
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group crypto-wallet-engine-group --describe

# Consume messages (for debugging)
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic order-placed-events --from-beginning
```

## Best Practices

1. **Idempotent Consumers**: Handle duplicate events gracefully
2. **Event Versioning**: Include event version for schema evolution
3. **Dead Letter Queue**: Route failed messages to DLQ
4. **Monitoring**: Monitor consumer lag and error rates
5. **Partitioning**: Choose partition keys for even distribution

## Future Enhancements

- **Schema Registry**: Use Confluent Schema Registry for event schemas
- **Event Sourcing**: Store all events for audit trail
- **CQRS**: Separate read/write models via events
- **Saga Pattern**: Distributed transactions via events
- **Event Replay**: Replay events for recovery/testing
