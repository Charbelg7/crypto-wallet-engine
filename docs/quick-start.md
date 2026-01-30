# Quick Start Guide

Get the Cryptocurrency Wallet & Trading Engine Simulator up and running in minutes.

## Prerequisites Check

```bash
java -version    # Should show Java 21
mvn -version     # Should show Maven 3.9+
docker --version # Should show Docker (optional, for integration tests)
```

## Step 1: Start Infrastructure

```bash
# Start PostgreSQL, Zookeeper, and Kafka
docker-compose up -d postgres zookeeper kafka

# Wait for services to be ready (check logs)
docker-compose logs -f
```

## Step 2: Build and Run

```bash
# Build the project
mvn clean package

# Run the application
mvn spring-boot:run
```

Or run via Docker:
```bash
docker-compose up --build app
```

## Step 3: Verify It's Working

```bash
# Check health
curl http://localhost:8080/actuator/health

# Should return: {"status":"UP"}
```

## Step 4: Test the API

### Create a User
```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"email":"trader@example.com","name":"John Doe"}'
```

### Deposit Funds
```bash
curl -X POST "http://localhost:8080/api/v1/wallets/deposit?userId=1&currency=USDT" \
  -H "Content-Type: application/json" \
  -d '{"amount": 10000}'
```

### Place an Order
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

### Check Order Book
```bash
curl http://localhost:8080/api/v1/market/orderbook/BTC/USDT
```

## Running Tests

### Unit Tests (No Docker Required)
```bash
mvn test
```

**Expected:** All 65 unit tests pass ✅

### Integration Tests (Requires Docker)
```bash
# Ensure Docker is running
docker ps

# Run integration tests
mvn verify
```

## Troubleshooting

### Port Already in Use
Change port in `application.properties`:
```properties
server.port=8081
```

### Docker Not Running
- Unit tests will still pass
- Integration tests will be skipped
- Application can run without Docker (use local PostgreSQL/Kafka)

### Database Connection Failed
Check PostgreSQL is running:
```bash
docker-compose ps postgres
```

### Kafka Connection Failed
Check Kafka is running:
```bash
docker-compose ps kafka
```

## Next Steps

- Read [API Documentation](./api.md) for complete API reference
- Check [Architecture Overview](./architecture.md) to understand the system
- Review [Testing Guide](./testing.md) for testing strategies
