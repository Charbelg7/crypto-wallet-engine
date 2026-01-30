package com.example.cryptoengine.integration;

import com.example.cryptoengine.application.dto.CreateUserRequest;
import com.example.cryptoengine.application.dto.DepositRequest;
import com.example.cryptoengine.application.dto.PlaceOrderRequest;
import com.example.cryptoengine.domain.Currency;
import com.example.cryptoengine.domain.OrderSide;
import com.example.cryptoengine.domain.OrderType;
import com.example.cryptoengine.service.OrderService;
import com.example.cryptoengine.service.UserService;
import com.example.cryptoengine.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for order placement and execution.
 * Uses Testcontainers for PostgreSQL and Kafka.
 * 
 * Note: These tests require Docker to be running.
 * Run with: mvn test -Ddocker.available=true
 * Or skip with: mvn test -DskipITs
 */
@SpringBootTest
@Testcontainers
@EnabledIfSystemProperty(named = "docker.available", matches = "true")
class OrderServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:16-alpine"))
        .withDatabaseName("test_crypto")
        .withUsername("test")
        .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private UserService userService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private OrderService orderService;

    private Long userId;

    @BeforeEach
    void setUp() {
        // Create a test user
        var userRequest = new CreateUserRequest("test@example.com", "Test User");
        userId = userService.createUser(userRequest).id();

        // Deposit USDT for trading
        var depositRequest = new DepositRequest(new BigDecimal("10000"), null);
        walletService.deposit(userId, Currency.USDT, depositRequest);
    }

    @Test
    void testPlaceLimitBuyOrder() {
        // Place a limit BUY order
        var orderRequest = new PlaceOrderRequest(
            OrderType.LIMIT,
            OrderSide.BUY,
            Currency.BTC,
            Currency.USDT,
            new BigDecimal("50000"),
            new BigDecimal("0.1"),
            null
        );

        var order = orderService.placeOrder(userId, orderRequest);

        assertThat(order).isNotNull();
        assertThat(order.id()).isNotNull();
        assertThat(order.type()).isEqualTo(OrderType.LIMIT);
        assertThat(order.side()).isEqualTo(OrderSide.BUY);
        assertThat(order.status()).isEqualTo(com.example.cryptoengine.domain.OrderStatus.OPEN);
    }

    @Test
    void testPlaceLimitSellOrder() {
        // First deposit BTC
        var depositRequest = new DepositRequest(new BigDecimal("1"), null);
        walletService.deposit(userId, Currency.BTC, depositRequest);

        // Place a limit SELL order
        var orderRequest = new PlaceOrderRequest(
            OrderType.LIMIT,
            OrderSide.SELL,
            Currency.BTC,
            Currency.USDT,
            new BigDecimal("51000"),
            new BigDecimal("0.5"),
            null
        );

        var order = orderService.placeOrder(userId, orderRequest);

        assertThat(order).isNotNull();
        assertThat(order.side()).isEqualTo(OrderSide.SELL);
        assertThat(order.status()).isEqualTo(com.example.cryptoengine.domain.OrderStatus.OPEN);
    }
}
