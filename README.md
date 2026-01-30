# Cryptocurrency Wallet & Trading Engine Simulator

A production-grade cryptocurrency wallet and trading engine simulator built with Spring Boot 3.3+, Java 21, Kafka, and PostgreSQL. This is a **simulation system** (NOT connected to real blockchain or exchanges) designed to demonstrate:

> 📚 **[View Full Documentation](./docs/README.md)** | 📄 **[Professional Profile](./PROFILE.md)**

- **Atomic balance updates** with optimistic locking
- **Event-driven architecture** via Apache Kafka
- **Thread-safe order matching engine** with price-time priority
- **Basic risk management** (balance checks, exposure limits)
- **Dockerized infrastructure** (PostgreSQL + Kafka + App)
- **Load testing** with Gatling

## Tech Stack

- **Java 21** with modern language features (records, pattern matching)
- **Spring Boot 3.3.5** (latest stable)
- **Spring Data JPA** + **PostgreSQL 16**
- **Apache Kafka** (Confluent Platform 7.6.0) for event-driven updates
- **Spring Kafka** for producer/consumer integration
- **Lombok** for reducing boilerplate
- **MapStruct** for DTO mapping
- **JUnit 5** + **Testcontainers** for integration tests
- **Gatling** for load/performance testing
- **Docker Compose** for local development

## Architecture

### Domain Model

- **User**: Traders in the system
- **Wallet**: User balances per currency (USDT, BTC, ETH) with optimistic locking
- **Order**: Trading orders (LIMIT/MARKET, BUY/SELL) with idempotency support
- **Trade**: Executed trades between orders

### Key Components

1. **Matching Engine**: Thread-safe in-memory order book with price-time priority matching
2. **Risk Engine**: Pre-trade validation (balance checks, exposure limits)
3. **Event System**: Kafka-based event publishing (OrderPlaced, OrderMatched, TradeExecuted, BalanceUpdated)
4. **REST APIs**: Versioned endpoints (`/api/v1/...`)

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

### Running Locally

1. **Start infrastructure** (PostgreSQL + Kafka):
   ```bash
   docker-compose up -d postgres zookeeper kafka
   ```

2. **Wait for services to be healthy** (check logs):
   ```bash
   docker-compose logs -f
   ```

3. **Build and run the application**:
   ```bash
   mvn clean package
   mvn spring-boot:run
   ```

   Or run via Docker:
   ```bash
   docker-compose up --build app
   ```

4. **Verify the application**:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

### API Endpoints

#### Users
- `POST /api/v1/users` - Create user
- `GET /api/v1/users/{id}` - Get user

#### Wallets
- `POST /api/v1/wallets/deposit?userId={id}&currency={currency}` - Deposit funds
- `GET /api/v1/wallets/balances?userId={id}` - Get all balances
- `GET /api/v1/wallets/balance?userId={id}&currency={currency}` - Get specific balance

#### Orders
- `POST /api/v1/orders?userId={id}` - Place order
- `POST /api/v1/orders/{orderId}/cancel?userId={id}` - Cancel order
- `GET /api/v1/orders/{orderId}?userId={id}` - Get order
- `GET /api/v1/orders?userId={id}` - Get user's orders

#### Market Data
- `GET /api/v1/market/orderbook/{symbol}` - Get order book (e.g., BTC/USDT)
- `GET /api/v1/market/trades/{symbol}?limit=100` - Get recent trades

### Example Usage

1. **Create a user**:
   ```bash
   curl -X POST http://localhost:8080/api/v1/users \
     -H "Content-Type: application/json" \
     -d '{"email": "trader@example.com", "name": "John Doe"}'
   ```

2. **Deposit USDT**:
   ```bash
   curl -X POST "http://localhost:8080/api/v1/wallets/deposit?userId=1&currency=USDT" \
     -H "Content-Type: application/json" \
     -d '{"amount": 10000}'
   ```

3. **Place a limit BUY order**:
   ```bash
   curl -X POST "http://localhost:8080/api/v1/orders?userId=1" \
     -H "Content-Type: application/json" \
     -d '{
       "type": "LIMIT",
       "side": "BUY",
       "baseCurrency": "BTC",
       "quoteCurrency": "USDT",
       "price": 50000,
       "quantity": 0.1
     }'
   ```

4. **Check order book**:
   ```bash
   curl http://localhost:8080/api/v1/market/orderbook/BTC/USDT
   ```

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests (with Testcontainers)
```bash
# Integration tests require Docker
# They are excluded by default from 'mvn test'
mvn verify  # Runs integration tests in integration-test phase
```

**Note:** Integration tests are excluded by default. Run `mvn test` to execute unit tests only (no Docker required).

### Load Testing with Gatling

1. **Start the application** (as above)

2. **Run Gatling simulation**:
   ```bash
   mvn gatling:test
   ```

   Or run a specific simulation:
   ```bash
   mvn gatling:test -Dgatling.simulationClass=OrderPlacementSimulation
   ```

3. **View results** in `target/gatling/`

## Project Structure

```
src/
├── main/
│   ├── java/com/example/cryptoengine/
│   │   ├── application/          # Use cases, DTOs, mappers
│   │   ├── controller/            # REST controllers
│   │   ├── domain/                # Domain entities, events, value objects
│   │   ├── infrastructure/        # Repositories, Kafka producers/consumers
│   │   ├── matching/              # Order book & matching engine
│   │   ├── risk/                  # Risk engine & price feed
│   │   └── service/               # Business logic services
│   └── resources/
│       └── application.properties  # Configuration
└── test/
    ├── java/                      # Unit & integration tests
    └── gatling/                   # Gatling load test scenarios
```

## Key Features

### Atomic Balance Updates
- Uses JPA optimistic locking (`@Version`) to prevent concurrent modification
- Database transactions ensure ACID properties
- Prevents double-spend and overdraft scenarios

### Event-Driven Architecture
- Domain events published to Kafka topics:
  - `order-placed-events`
  - `order-matched-events`
  - `trade-executed-events`
  - `balance-updated-events`
- Consumers update read models and send real-time notifications (simulated via logs)

### Thread-Safe Matching Engine
- In-memory order book using `ConcurrentSkipListMap`
- Read-write locks for order book operations
- Price-time priority matching (best price first, then FIFO)

### Risk Management
- Pre-trade balance validation
- Exposure limit checks (configurable max exposure in USDT)
- Simulated price feed for risk calculations

### Idempotency
- Orders support idempotency keys to prevent duplicate submissions
- Idempotency key checked before order creation

## Configuration

Key configuration in `application.properties`:

- `crypto.risk.max-exposure-usdt`: Maximum exposure limit (default: 100,000 USDT)
- `crypto.risk.enabled`: Enable/disable risk checks (default: true)
- `crypto.trading.pairs`: Supported trading pairs (default: BTC/USDT, ETH/USDT)

## Docker Compose Services

- **postgres**: PostgreSQL 16 database
- **zookeeper**: Kafka Zookeeper
- **kafka**: Kafka broker
- **app**: Spring Boot application

## Performance Considerations

- Order book is in-memory for low latency
- Database uses connection pooling (HikariCP)
- Kafka producers use idempotent configuration
- Optimistic locking reduces lock contention vs pessimistic locking

## Limitations (Simulation)

- No real blockchain integration
- Simple price feed (fixed prices, can be updated)
- In-memory order book (not distributed)
- Basic risk checks (not production-grade)
- No WebSocket for real-time updates (events logged only)

## Future Enhancements

- WebSocket support for real-time order book updates
- Distributed order book (Redis/Kafka Streams)
- More sophisticated risk engine
- Order history and analytics
- JWT authentication
- Rate limiting

## 📚 Documentation

Comprehensive documentation is available in the [`docs/`](./docs/) directory:

- **[Architecture Overview](./docs/architecture.md)** - System design and component interactions
- **[API Documentation](./docs/api.md)** - Complete REST API reference with examples
- **[Setup Guide](./docs/setup.md)** - Installation and configuration instructions
- **[Design Decisions](./docs/design-decisions.md)** - Key architectural choices and rationale
- **[Testing Guide](./docs/testing.md)** - Testing strategy and load testing
- **[Performance & Scalability](./docs/performance.md)** - Performance characteristics and optimization
- **[Kafka Events](./docs/kafka-events.md)** - Event-driven architecture documentation
- **[Risk Engine](./docs/risk-engine.md)** - Risk management system documentation

## 👤 Professional Profile

This project demonstrates production-grade backend engineering skills. See **[PROFILE.md](./PROFILE.md)** for a detailed professional profile highlighting:
- Technical achievements and challenges solved
- Architecture and design patterns
- Performance metrics and scalability considerations
- Production-ready features and best practices

## License

This is a demonstration project for portfolio/resume purposes.

## Author

Built to demonstrate production-grade backend engineering skills with Spring Boot, Kafka, and event-driven architecture.
