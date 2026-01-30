# Testing Guide

Comprehensive testing strategy for the Cryptocurrency Wallet & Trading Engine Simulator.

## Testing Philosophy

- **Unit Tests**: Fast, isolated tests for individual components
- **Integration Tests**: Test against real database and Kafka using Testcontainers
- **Load Tests**: Performance testing with Gatling
- **Test Coverage**: Aim for >80% code coverage

## Unit Tests

### Matching Engine Tests

**Location:** `src/test/java/com/example/cryptoengine/matching/MatchingEngineTest.java`

**Purpose:** Test order matching logic in isolation.

**Example:**
```java
@Test
void testLimitOrderMatching() {
    // Create sell order
    Order sellOrder = Order.builder()...
    
    // Add to order book
    orderBookManager.getOrderBook("BTC/USDT").addOrder(sellOrder);
    
    // Create buy order
    Order buyOrder = Order.builder()...
    
    // Match
    List<Trade> trades = matchingEngine.matchOrder(buyOrder);
    
    assertThat(trades).hasSize(1);
    assertThat(trades.get(0).getPrice()).isEqualByComparingTo(...);
}
```

**Run:**
```bash
mvn test -Dtest=MatchingEngineTest
```

### Service Layer Tests

**Purpose:** Test business logic without database/Kafka.

**Strategy:**
- Mock repositories
- Mock Kafka producers
- Test business rules and validations

**Example Structure:**
```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private KafkaEventProducer eventProducer;
    
    @InjectMocks
    private OrderService orderService;
    
    @Test
    void testPlaceOrder() {
        // Given
        when(orderRepository.findByIdempotencyKey(...)).thenReturn(Optional.empty());
        
        // When
        OrderResponse response = orderService.placeOrder(...);
        
        // Then
        assertThat(response).isNotNull();
        verify(eventProducer).publishOrderPlaced(...);
    }
}
```

## Integration Tests

### Testcontainers Setup

**Location:** `src/test/java/com/example/cryptoengine/integration/OrderServiceIntegrationTest.java`

**Purpose:** Test against real PostgreSQL and Kafka.

**Setup:**
```java
@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:16-alpine"))
        .withDatabaseName("test_crypto");
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
```

**Run:**
```bash
mvn verify  # Runs integration tests
```

**Requirements:**
- Docker must be running
- Sufficient resources (2GB+ RAM recommended)

### Integration Test Scenarios

#### 1. Order Placement Flow
```java
@Test
void testPlaceLimitBuyOrder() {
    // Create user
    UserResponse user = userService.createUser(...);
    
    // Deposit funds
    walletService.deposit(user.id(), Currency.USDT, ...);
    
    // Place order
    OrderResponse order = orderService.placeOrder(user.id(), ...);
    
    // Verify
    assertThat(order.status()).isEqualTo(OrderStatus.OPEN);
    assertThat(order.quantity()).isEqualByComparingTo(...);
}
```

#### 2. Trade Execution Flow
```java
@Test
void testOrderMatchingAndExecution() {
    // Create two users
    // Place buy order
    // Place sell order at matching price
    // Verify trade executed
    // Verify balances updated
}
```

#### 3. Balance Update Atomicity
```java
@Test
void testConcurrentBalanceUpdates() {
    // Create user with balance
    // Run concurrent deposit/withdraw operations
    // Verify final balance is correct
    // Verify no lost updates (optimistic locking)
}
```

## Load Testing with Gatling

### Gatling Simulation

**Location:** `src/test/gatling/user-files/simulations/OrderPlacementSimulation.scala`

**Purpose:** Test system under load.

### Running Gatling Tests

1. **Start the application:**
   ```bash
   mvn spring-boot:run
   ```

2. **Run Gatling simulation:**
   ```bash
   mvn gatling:test
   ```

3. **View results:**
   Open `target/gatling/index.html` in browser

### Gatling Scenarios

#### Scenario 1: Order Placement Load
```scala
val placeLimitBuyOrder = scenario("Place Limit Buy Order")
  .exec(createUser)
  .exec(deposit)
  .repeat(10) {
    exec(http("place_limit_buy_order")
      .post("/api/v1/orders?userId=${userId}")
      .body(StringBody(...))
      .check(status.in(201, 200)))
  }
```

**Load Profile:**
- Ramp up: 50 users over 30 seconds
- Sustained: 10 users/second for 2 minutes

#### Scenario 2: Mixed Workload
```scala
val mixedWorkload = scenario("Mixed Workload")
  .exec(createUser)
  .exec(deposit)
  .exec(getBalances)
  .repeat(5) {
    exec(placeBuyOrder)
    .exec(placeSellOrder)
    .exec(getOrderBook)
  }
```

**Load Profile:**
- Ramp up: 20 users over 30 seconds
- Sustained: 5 users/second for 2 minutes

### Performance Assertions

```scala
setUp(...)
  .assertions(
    global.responseTime.max.lt(2000),  // Max response time < 2s
    global.successfulRequests.percent.gt(95)  // Success rate > 95%
  )
```

### Interpreting Results

**Key Metrics:**
- **Response Time**: P50, P95, P99 percentiles
- **Throughput**: Requests per second
- **Error Rate**: Percentage of failed requests
- **Active Users**: Concurrent users

**Example Results:**
```
================================================================================
---- Global Information --------------------------------------------------------
> request count                                   5000 (OK=4750  KO=250)
> min response time                                 10 (OK=10     KO=500)
> max response time                               1500 (OK=1500   KO=2000)
> mean response time                               250 (OK=250    KO=1000)
> std deviation                                     150 (OK=150    KO=200)
> response time 95th percentile                     800 (OK=800    KO=1800)
> response time 99th percentile                    1200 (OK=1200   KO=1900)
> mean requests/sec                               100.0 (OK=95.0   KO=5.0)
```

## Test Data Management

### Test Fixtures

Create reusable test data:

```java
public class TestFixtures {
    public static CreateUserRequest createUserRequest() {
        return new CreateUserRequest(
            "test@example.com",
            "Test User"
        );
    }
    
    public static PlaceOrderRequest createLimitBuyOrder() {
        return new PlaceOrderRequest(
            OrderType.LIMIT,
            OrderSide.BUY,
            Currency.BTC,
            Currency.USDT,
            new BigDecimal("50000"),
            new BigDecimal("0.1"),
            null
        );
    }
}
```

### Database Cleanup

**Option 1: Transaction Rollback**
```java
@Transactional
@Rollback
@Test
void testSomething() {
    // Test code - automatically rolled back
}
```

**Option 2: Manual Cleanup**
```java
@BeforeEach
void setUp() {
    // Clean database
    orderRepository.deleteAll();
    walletRepository.deleteAll();
}
```

## Test Coverage

### Generate Coverage Report

```bash
mvn clean test jacoco:report
```

View report: `target/site/jacoco/index.html`

### Coverage Goals

- **Services**: >90%
- **Controllers**: >80%
- **Repositories**: >70%
- **Domain**: >85%

## Continuous Integration

### GitHub Actions Example

```yaml
name: CI

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
      - uses: actions/setup-maven@v3
      
      - name: Run tests
        run: mvn clean verify
```

## Best Practices

### 1. Test Isolation
- Each test should be independent
- Use `@BeforeEach` for setup
- Clean up test data

### 2. Test Naming
```java
@Test
void shouldPlaceOrderWhenUserHasSufficientBalance() { ... }

@Test
void shouldRejectOrderWhenBalanceInsufficient() { ... }
```

### 3. Arrange-Act-Assert Pattern
```java
@Test
void testSomething() {
    // Arrange
    User user = createUser();
    depositFunds(user, 10000);
    
    // Act
    OrderResponse order = orderService.placeOrder(...);
    
    // Assert
    assertThat(order.status()).isEqualTo(OrderStatus.OPEN);
}
```

### 4. Test Edge Cases
- Zero amounts
- Negative amounts (should fail)
- Very large amounts
- Concurrent operations
- Network failures (for integration tests)

### 5. Mock External Dependencies
- Mock Kafka producers in unit tests
- Use Testcontainers for integration tests
- Don't mock domain logic

## Troubleshooting Tests

### Testcontainers Issues

**Problem:** Tests fail with "Could not find Docker"

**Solution:**
```bash
# Ensure Docker is running
docker ps

# Check Docker socket permissions (Linux)
sudo chmod 666 /var/run/docker.sock
```

### Flaky Tests

**Problem:** Tests pass/fail intermittently

**Solutions:**
- Add proper synchronization
- Use `awaitility` for async operations
- Increase timeouts
- Check for race conditions

### Slow Tests

**Problem:** Integration tests take too long

**Solutions:**
- Use `@Sql` for database setup
- Reuse containers across tests
- Parallel test execution
- Mock external services where possible

## Next Steps

- Add more integration test scenarios
- Increase test coverage
- Add performance benchmarks
- Set up CI/CD pipeline
