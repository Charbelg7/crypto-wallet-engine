package com.example.cryptoengine.domain.entity;

import com.example.cryptoengine.domain.Currency;
import com.example.cryptoengine.domain.OrderSide;
import com.example.cryptoengine.domain.OrderStatus;
import com.example.cryptoengine.domain.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder()
            .id(1L)
            .userId(1L)
            .type(OrderType.LIMIT)
            .side(OrderSide.BUY)
            .baseCurrency(Currency.BTC)
            .quoteCurrency(Currency.USDT)
            .price(new BigDecimal("50000"))
            .quantity(new BigDecimal("1.0"))
            .filledQuantity(BigDecimal.ZERO)
            .status(OrderStatus.OPEN)
            .build();
    }

    @Test
    void shouldCalculateRemainingQuantity() {
        // Given
        order.setFilledQuantity(new BigDecimal("0.3"));

        // When
        BigDecimal remaining = order.getRemainingQuantity();

        // Then
        assertThat(remaining).isEqualByComparingTo(new BigDecimal("0.7"));
    }

    @Test
    void shouldReturnTrueWhenFilled() {
        // Given
        order.setFilledQuantity(new BigDecimal("1.0"));

        // When
        boolean isFilled = order.isFilled();

        // Then
        assertThat(isFilled).isTrue();
    }

    @Test
    void shouldReturnFalseWhenNotFilled() {
        // Given
        order.setFilledQuantity(new BigDecimal("0.5"));

        // When
        boolean isFilled = order.isFilled();

        // Then
        assertThat(isFilled).isFalse();
    }

    @Test
    void shouldReturnTrueWhenCanCancel() {
        // Given
        order.setStatus(OrderStatus.OPEN);

        // When
        boolean canCancel = order.canCancel();

        // Then
        assertThat(canCancel).isTrue();
    }

    @Test
    void shouldReturnTrueWhenCanCancelPartial() {
        // Given
        order.setStatus(OrderStatus.PARTIAL);

        // When
        boolean canCancel = order.canCancel();

        // Then
        assertThat(canCancel).isTrue();
    }

    @Test
    void shouldReturnFalseWhenCannotCancelFilled() {
        // Given
        order.setStatus(OrderStatus.FILLED);

        // When
        boolean canCancel = order.canCancel();

        // Then
        assertThat(canCancel).isFalse();
    }

    @Test
    void shouldReturnFalseWhenCannotCancelCancelled() {
        // Given
        order.setStatus(OrderStatus.CANCELLED);

        // When
        boolean canCancel = order.canCancel();

        // Then
        assertThat(canCancel).isFalse();
    }

    @Test
    void shouldGetSymbol() {
        // When
        String symbol = order.getSymbol();

        // Then
        assertThat(symbol).isEqualTo("BTC/USDT");
    }

    @Test
    void shouldFillOrderPartially() {
        // When
        order.fill(new BigDecimal("0.3"));

        // Then
        assertThat(order.getFilledQuantity()).isEqualByComparingTo(new BigDecimal("0.3"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIAL);
    }

    @Test
    void shouldFillOrderCompletely() {
        // When
        order.fill(new BigDecimal("1.0"));

        // Then
        assertThat(order.getFilledQuantity()).isEqualByComparingTo(new BigDecimal("1.0"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void shouldThrowExceptionWhenFillExceedsQuantity() {
        // When/Then
        assertThatThrownBy(() -> order.fill(new BigDecimal("1.1")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot fill more than order quantity");
    }

    @Test
    void shouldThrowExceptionWhenFillIsNegative() {
        // When/Then
        assertThatThrownBy(() -> order.fill(new BigDecimal("-0.1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Fill quantity must be positive");
    }

    @Test
    void shouldCancelOrder() {
        // Given
        order.setStatus(OrderStatus.OPEN);

        // When
        order.cancel();

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shouldThrowExceptionWhenCancellingFilledOrder() {
        // Given
        order.setStatus(OrderStatus.FILLED);

        // When/Then
        assertThatThrownBy(() -> order.cancel())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cannot be cancelled");
    }
}
