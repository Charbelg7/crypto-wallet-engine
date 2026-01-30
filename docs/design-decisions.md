# Design Decisions

This document explains key architectural and design decisions made in the Cryptocurrency Wallet & Trading Engine Simulator.

## 1. Optimistic Locking vs Pessimistic Locking

**Decision:** Use optimistic locking for wallet balance updates.

**Rationale:**
- **Better Performance**: No database-level locks, allowing concurrent reads
- **Scalability**: Multiple transactions can proceed simultaneously
- **Deadlock Prevention**: Avoids potential deadlocks from lock contention
- **Trade-off**: Requires retry logic for concurrent modifications

**Implementation:**
```java
@Version
private Long version;  // Auto-incremented on update
```

**Alternative Considered:** Pessimistic locking (`SELECT FOR UPDATE`) was rejected due to potential performance bottlenecks and deadlock risks.

---

## 2. In-Memory Order Book vs Database-Backed

**Decision:** Use in-memory order book with `ConcurrentSkipListMap`.

**Rationale:**
- **Low Latency**: Sub-millisecond order matching
- **High Throughput**: Can handle thousands of orders per second
- **Simple Implementation**: No database round-trips for matching

**Trade-offs:**
- **Single Instance**: Cannot scale horizontally without changes
- **Data Loss Risk**: Order book lost on restart (mitigated by database persistence)

**Future Enhancement:** Distributed order book using Redis or Kafka Streams for horizontal scalability.

---

## 3. Event-Driven Architecture

**Decision:** Use Kafka for domain event publishing.

**Rationale:**
- **Decoupling**: Producers don't need to know about consumers
- **Scalability**: Multiple consumers can process events independently
- **Reliability**: Kafka provides durability and replay capabilities
- **Real-time Updates**: Enables real-time notifications and read model updates

**Event Types:**
- `OrderPlacedEvent` - Order creation
- `OrderMatchedEvent` - Order matching
- `TradeExecutedEvent` - Trade execution
- `BalanceUpdatedEvent` - Balance changes

**Alternative Considered:** Synchronous callbacks were rejected due to tight coupling and scalability concerns.

---

## 4. Price-Time Priority Matching

**Decision:** Implement price-time priority matching algorithm.

**Matching Rules:**
1. **Price Priority**: Better prices match first
   - BUY orders: Higher price = better
   - SELL orders: Lower price = better
2. **Time Priority**: Earlier orders at same price match first (FIFO)

**Rationale:**
- **Fairness**: Ensures fair order execution
- **Industry Standard**: Matches how real exchanges work
- **Predictable**: Clear execution order

**Implementation:**
```java
// Bids sorted by price descending, then timestamp ascending
ConcurrentSkipListMap<BigDecimal, List<OrderBookEntry>> bids
```

---

## 5. Transaction Management Strategy

**Decision:** Use Spring's `@Transactional` with default isolation level.

**Rationale:**
- **ACID Guarantees**: Ensures consistency
- **Automatic Rollback**: Exceptions trigger rollback
- **Simple**: Leverages Spring's transaction management

**Transaction Boundaries:**
- Order placement: Single transaction
- Trade execution: Single transaction (both orders updated atomically)
- Balance updates: Within trade execution transaction

**Consideration:** Could use `@Transactional(isolation = READ_COMMITTED)` for better concurrency, but default is sufficient for this use case.

---

## 6. Idempotency Implementation

**Decision:** Use idempotency keys for orders and deposits.

**Rationale:**
- **Prevents Duplicates**: Network retries won't create duplicate orders
- **Client Control**: Clients can ensure exactly-once semantics
- **Simple**: Database unique constraint on idempotency key

**Implementation:**
```java
@Column(length = 100)
private String idempotencyKey;

// Check before creation
orderRepository.findByIdempotencyKey(key).ifPresent(...)
```

**Alternative Considered:** Distributed locks (Redis) were rejected as overkill for this use case.

---

## 7. DTO Pattern

**Decision:** Use DTOs (Data Transfer Objects) for API boundaries.

**Rationale:**
- **API Stability**: Internal entity changes don't break API contracts
- **Security**: Prevents exposing internal fields
- **Performance**: Can optimize DTOs for serialization
- **Validation**: DTOs can have different validation rules

**Implementation:**
- MapStruct for entity ↔ DTO conversion
- Records for immutable DTOs (Java 21)

**Alternative Considered:** Direct entity serialization was rejected due to tight coupling and security concerns.

---

## 8. Risk Engine Design

**Decision:** Pre-trade risk checks before order placement.

**Checks Performed:**
1. **Balance Validation**: Sufficient funds for order
2. **Exposure Limits**: Total position value < max exposure

**Rationale:**
- **Prevention**: Better to reject invalid orders than handle failures later
- **User Experience**: Clear error messages before order placement
- **Compliance**: Risk checks are required in financial systems

**Implementation:**
```java
@Transactional(readOnly = true)
public void validateOrder(Order order) {
    validateBalance(order);
    validateExposureLimit(order);
}
```

**Future Enhancement:** Real-time risk monitoring, position limits per currency, margin requirements.

---

## 9. Market Order Handling

**Decision:** Market orders execute immediately at best available price.

**Rationale:**
- **User Expectation**: Market orders should fill immediately
- **Simplicity**: No need to manage market orders in order book
- **Risk**: Uses conservative price estimates for balance checks

**Implementation:**
- Market BUY: Match against asks (sell orders)
- Market SELL: Match against bids (buy orders)
- Uses best available price from order book

**Consideration:** In production, would need slippage protection and better price estimation.

---

## 10. Thread Safety Strategy

**Decision:** Use `ConcurrentSkipListMap` and `ReadWriteLock` for order book.

**Rationale:**
- **Concurrent Reads**: Multiple threads can read simultaneously
- **Safe Writes**: Write lock ensures exclusive access
- **Performance**: Better than synchronized blocks

**Implementation:**
```java
private final ReadWriteLock lock = new ReentrantReadWriteLock();
private final ConcurrentSkipListMap<BigDecimal, List<OrderBookEntry>> bids;
```

**Alternative Considered:** `synchronized` blocks were rejected due to potential contention.

---

## 11. Database Choice: PostgreSQL

**Decision:** Use PostgreSQL as the primary database.

**Rationale:**
- **ACID Compliance**: Ensures data consistency
- **Mature**: Battle-tested in production
- **Features**: JSON support, full-text search, extensions
- **Open Source**: No licensing costs

**Alternative Considered:** MySQL was considered but PostgreSQL's JSON support and better concurrency control made it preferable.

---

## 12. Kafka Consumer Acknowledgment Mode

**Decision:** Use manual acknowledgment mode.

**Rationale:**
- **Reliability**: Only acknowledge after successful processing
- **Error Handling**: Can retry failed messages
- **Control**: Explicit control over message processing

**Implementation:**
```java
@KafkaListener(...)
public void consume(..., Acknowledgment acknowledgment) {
    try {
        // Process event
        acknowledgment.acknowledge();
    } catch (Exception e) {
        // Log error, don't acknowledge
    }
}
```

**Future Enhancement:** Dead letter queue for failed messages.

---

## 13. Testing Strategy

**Decision:** Use Testcontainers for integration tests.

**Rationale:**
- **Real Environment**: Tests against real PostgreSQL and Kafka
- **Isolation**: Each test gets fresh containers
- **Reliability**: Tests match production environment

**Implementation:**
```java
@Container
static PostgreSQLContainer<?> postgres = ...;

@Container
static KafkaContainer kafka = ...;
```

**Alternative Considered:** Embedded databases were rejected as they don't match production behavior.

---

## 14. Error Handling Strategy

**Decision:** Use `@ControllerAdvice` for global exception handling.

**Rationale:**
- **Consistency**: Uniform error responses across all endpoints
- **Separation**: Error handling logic separated from business logic
- **Maintainability**: Single place to update error handling

**Implementation:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handle(...) { ... }
}
```

---

## 15. Currency Support

**Decision:** Support only USDT, BTC, ETH initially.

**Rationale:**
- **Simplicity**: Focus on core functionality first
- **Common Pairs**: These are the most common trading pairs
- **Extensibility**: Easy to add more currencies later

**Implementation:**
```java
public enum Currency {
    USDT, BTC, ETH
}
```

**Future Enhancement:** Dynamic currency support via database configuration.

---

## Summary

These design decisions prioritize:
1. **Performance**: In-memory order book, optimistic locking
2. **Reliability**: Transactions, event-driven architecture
3. **Scalability**: Kafka for event streaming
4. **Maintainability**: Clean architecture, DTOs, separation of concerns
5. **Testability**: Testcontainers, unit tests

Each decision balances trade-offs between performance, complexity, and maintainability, suitable for a production-grade demonstration project.
