package com.example.cryptoengine.domain.entity;

import com.example.cryptoengine.domain.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletTest {

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = Wallet.builder()
            .id(1L)
            .userId(1L)
            .currency(Currency.USDT)
            .balance(new BigDecimal("1000"))
            .version(1L)
            .build();
    }

    @Test
    void shouldDepositSuccessfully() {
        // Given
        BigDecimal depositAmount = new BigDecimal("500");

        // When
        wallet.deposit(depositAmount);

        // Then
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("1500"));
    }

    @Test
    void shouldThrowExceptionWhenDepositingZero() {
        // When/Then
        assertThatThrownBy(() -> wallet.deposit(BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Deposit amount must be positive");
    }

    @Test
    void shouldThrowExceptionWhenDepositingNegative() {
        // When/Then
        assertThatThrownBy(() -> wallet.deposit(new BigDecimal("-100")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Deposit amount must be positive");
    }

    @Test
    void shouldWithdrawSuccessfully() {
        // Given
        BigDecimal withdrawAmount = new BigDecimal("300");

        // When
        wallet.withdraw(withdrawAmount);

        // Then
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("700"));
    }

    @Test
    void shouldThrowExceptionWhenInsufficientBalance() {
        // Given
        BigDecimal withdrawAmount = new BigDecimal("1500");

        // When/Then
        assertThatThrownBy(() -> wallet.withdraw(withdrawAmount))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Insufficient balance");
    }

    @Test
    void shouldThrowExceptionWhenWithdrawingZero() {
        // When/Then
        assertThatThrownBy(() -> wallet.withdraw(BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Withdrawal amount must be positive");
    }

    @Test
    void shouldThrowExceptionWhenWithdrawingNegative() {
        // When/Then
        assertThatThrownBy(() -> wallet.withdraw(new BigDecimal("-100")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Withdrawal amount must be positive");
    }

    @Test
    void shouldAllowWithdrawingExactBalance() {
        // Given
        BigDecimal withdrawAmount = new BigDecimal("1000");

        // When
        wallet.withdraw(withdrawAmount);

        // Then
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
