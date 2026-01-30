# Architecture Overview

## System Architecture

The Cryptocurrency Wallet & Trading Engine Simulator follows a **layered architecture** with **event-driven** components, designed for high performance, reliability, and scalability.

```
┌─────────────────────────────────────────────────────────────┐
│                    REST API Layer                           │
│  (Controllers: User, Wallet, Order, MarketData)             │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│              Application Service Layer                      │
│  (UserService, WalletService, OrderService, etc.)           │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
┌───────▼────────┐      ┌─────────▼──────────┐
│  Domain Layer  │      │  Infrastructure    │
│  (Entities,    │      │ (Repositories,     │
│   Events,      │      │   Kafka, DB)       │
│   Value Obj.)  │      │                    │
└───────┬────────┘      └─────────┬──────────┘
        │                         │
        └────────────┬────────────┘
                     │
        ┌────────────▼────────────┐
        │   Matching Engine       │
        │   (OrderBook,           │
        │    MatchingEngine)      │
        └─────────────────────────┘
```

## Core Components

### 1. Domain Layer

**Entities:**
- `User` - Trader accounts
- `Wallet` - Currency balances with optimistic locking
- `Order` - Trading orders (LIMIT/MARKET, BUY/SELL)
- `Trade` - Executed trades

**Domain Events:**
- `OrderPlacedEvent` - Published when order is created
- `OrderMatchedEvent` - Published when order is matched
- `TradeExecutedEvent` - Published when trade executes
- `BalanceUpdatedEvent` - Published on balance changes

**Value Objects:**
- `Currency` enum (USDT, BTC, ETH)
- `OrderType`, `OrderSide`, `OrderStatus` enums

### 2. Application Layer

**Services:**
- `UserService` - User management
- `WalletService` - Balance operations (deposit/withdraw)
- `OrderService` - Order lifecycle management
- `TradeExecutionService` - Trade execution and balance updates
- `MarketDataService` - Order book and trade queries

**DTOs & Mappers:**
- Request/Response DTOs for API boundaries
- MapStruct mappers for entity ↔ DTO conversion

### 3. Infrastructure Layer

**Repositories:**
- JPA repositories with custom queries
- Optimistic locking support (`@Lock(LockModeType.OPTIMISTIC)`)

**Kafka Integration:**
- `KafkaEventProducer` - Publishes domain events
- Event consumers for read model updates
- Manual acknowledgment mode for reliability

**Database:**
- PostgreSQL 16 with JPA/Hibernate
- Connection pooling (HikariCP)
- Batch operations for performance

### 4. Matching Engine

**OrderBook:**
- In-memory data structure using `ConcurrentSkipListMap`
- Separate bid/ask sides
- Read-write locks for thread safety
- Price-time priority ordering

**MatchingEngine:**
- Price-time priority matching algorithm
- Supports LIMIT and MARKET orders
- Immediate execution for market orders
- Order book placement for limit orders

### 5. Risk Engine

**RiskEngine:**
- Pre-trade balance validation
- Exposure limit checks
- Simulated price feed integration

**PriceFeed:**
- Simple in-memory price store
- Can be extended to connect to real market data

## Design Patterns

### 1. Domain-Driven Design (DDD-lite)

- **Entities**: Rich domain models with business logic
- **Value Objects**: Immutable currency, order types
- **Domain Events**: Decoupled event publishing
- **Repositories**: Data access abstraction

### 2. Event-Driven Architecture

- **Event Sourcing**: Domain events capture state changes
- **CQRS-lite**: Separate read/write models via events
- **Async Processing**: Kafka consumers handle side effects

### 3. Optimistic Locking

- **Version Field**: `@Version` annotation on Wallet entity
- **Concurrency Control**: Prevents lost updates
- **Retry Logic**: Handles `OptimisticLockException`

### 4. Transaction Management

- **ACID Properties**: Database transactions ensure consistency
- **Isolation Levels**: Default Spring transaction isolation
- **Rollback**: Automatic rollback on exceptions

## Data Flow

### Order Placement Flow

```
1. Client → POST /api/v1/orders
2. OrderController → OrderService.placeOrder()
3. RiskEngine.validateOrder() → Balance checks
4. TradeExecutionService.reserveBalance() → Lock funds
5. OrderRepository.save() → Persist order
6. KafkaEventProducer.publishOrderPlaced() → Event
7. MatchingEngine.matchOrder() → Match against order book
8. TradeExecutionService.executeTrade() → Execute trades
9. KafkaEventProducer.publishTradeExecuted() → Event
10. Response → OrderResponse DTO
```

### Trade Execution Flow

```
1. MatchingEngine finds matching orders
2. TradeExecutionService.executeTrade()
   - Update order fills
   - Update buyer balances (receive base, pay quote)
   - Update seller balances (receive quote, pay base)
   - Save orders and trades
3. OrderBook.updateOrderQuantity() → Update order book
4. KafkaEventProducer.publishTradeExecuted() → Event
5. KafkaEventProducer.publishBalanceUpdated() → Events
```

## Thread Safety

### Order Book

- **ConcurrentSkipListMap**: Thread-safe sorted map
- **ReadWriteLock**: Multiple readers, single writer
- **Immutable Entries**: OrderBookEntry is immutable

### Matching Engine

- **Synchronized Access**: Order book locks prevent race conditions
- **Atomic Operations**: Database transactions ensure consistency

### Balance Updates

- **Optimistic Locking**: Version field prevents concurrent modifications
- **Database Transactions**: ACID guarantees

## Scalability Considerations

### Current Design

- **In-Memory Order Book**: Low latency, single-instance only
- **Database**: PostgreSQL for persistence
- **Kafka**: Event streaming for scalability

### Future Enhancements

- **Distributed Order Book**: Redis or Kafka Streams
- **Sharding**: Partition orders by symbol
- **Caching**: Redis for frequently accessed data
- **Read Replicas**: Database read replicas for queries

## Technology Stack Rationale

| Technology | Purpose | Rationale |
|------------|---------|-----------|
| **Java 21** | Language | Modern features, records, pattern matching |
| **Spring Boot 3.3** | Framework | Production-ready, auto-configuration |
| **PostgreSQL** | Database | ACID compliance, JSON support, reliability |
| **Kafka** | Messaging | High throughput, event streaming |
| **Docker** | Containerization | Easy deployment, reproducibility |
| **Testcontainers** | Testing | Real database/Kafka for integration tests |
| **Gatling** | Load Testing | Realistic performance testing |

## Security Considerations

### Current Implementation

- **Input Validation**: Bean validation annotations
- **Exception Handling**: Global exception handler
- **Idempotency**: Prevents duplicate operations

### Production Enhancements

- **JWT Authentication**: Token-based auth
- **Rate Limiting**: Prevent abuse
- **API Keys**: Service-to-service authentication
- **Encryption**: Sensitive data encryption
- **Audit Logging**: Track all operations

## Monitoring & Observability

### Current Logging

- **Structured Logging**: SLF4J + Logback
- **Log Levels**: DEBUG, INFO, WARN, ERROR
- **Event Logging**: Kafka events logged

### Production Recommendations

- **Metrics**: Micrometer + Prometheus
- **Distributed Tracing**: Zipkin/Jaeger
- **Health Checks**: Spring Actuator endpoints
- **Alerting**: PagerDuty/AlertManager
