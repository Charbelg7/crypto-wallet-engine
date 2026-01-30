package com.example.cryptoengine.risk;

import com.example.cryptoengine.domain.Currency;
import com.example.cryptoengine.domain.OrderSide;
import com.example.cryptoengine.domain.OrderType;
import com.example.cryptoengine.domain.entity.Order;
import com.example.cryptoengine.domain.entity.Wallet;
import com.example.cryptoengine.infrastructure.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskEngineTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PriceFeed priceFeed;

    @InjectMocks
    private RiskEngine riskEngine;

    private Order buyOrder;
    private Order sellOrder;
    private Wallet usdtWallet;
    private Wallet btcWallet;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(riskEngine, "maxExposureUsdt", new BigDecimal("100000"));
        ReflectionTestUtils.setField(riskEngine, "riskEnabled", true);
        
        buyOrder = Order.builder()
            .id(1L)
            .userId(1L)
            .type(OrderType.LIMIT)
            .side(OrderSide.BUY)
            .baseCurrency(Currency.BTC)
            .quoteCurrency(Currency.USDT)
            .price(new BigDecimal("50000"))
            .quantity(new BigDecimal("0.1"))
            .build();
        
        sellOrder = Order.builder()
            .id(2L)
            .userId(1L)
            .type(OrderType.LIMIT)
            .side(OrderSide.SELL)
            .baseCurrency(Currency.BTC)
            .quoteCurrency(Currency.USDT)
            .price(new BigDecimal("51000"))
            .quantity(new BigDecimal("0.5"))
            .build();
        
        usdtWallet = Wallet.builder()
            .id(1L)
            .userId(1L)
            .currency(Currency.USDT)
            .balance(new BigDecimal("10000"))
            .build();
        
        btcWallet = Wallet.builder()
            .id(2L)
            .userId(1L)
            .currency(Currency.BTC)
            .balance(new BigDecimal("1"))
            .build();
    }

    @Test
    void shouldValidateBuyOrderWithSufficientBalance() {
        // Given
        BigDecimal requiredAmount = new BigDecimal("5000"); // 0.1 * 50000
        when(walletRepository.findByUserIdAndCurrency(1L, Currency.USDT))
            .thenReturn(Optional.of(usdtWallet));
        when(priceFeed.getPrice("BTC/USDT"))
            .thenReturn(Optional.of(new BigDecimal("50000")));
        when(walletRepository.findByUserId(1L))
            .thenReturn(Arrays.asList(usdtWallet, btcWallet));

        // When
        riskEngine.validateOrder(buyOrder);

        // Then
        verify(walletRepository).findByUserIdAndCurrency(1L, Currency.USDT);
        verify(priceFeed, atLeastOnce()).getPrice("BTC/USDT");
    }

    @Test
    void shouldThrowExceptionWhenInsufficientBalanceForBuy() {
        // Given
        Wallet insufficientWallet = Wallet.builder()
            .id(1L)
            .userId(1L)
            .currency(Currency.USDT)
            .balance(new BigDecimal("1000")) // Less than required 5000
            .build();
        
        when(walletRepository.findByUserIdAndCurrency(1L, Currency.USDT))
            .thenReturn(Optional.of(insufficientWallet));

        // When/Then
        assertThatThrownBy(() -> riskEngine.validateOrder(buyOrder))
            .isInstanceOf(RiskException.class)
            .hasMessageContaining("Insufficient balance");
    }

    @Test
    void shouldValidateSellOrderWithSufficientBalance() {
        // Given
        when(walletRepository.findByUserIdAndCurrency(1L, Currency.BTC))
            .thenReturn(Optional.of(btcWallet));

        // When
        riskEngine.validateOrder(sellOrder);

        // Then
        verify(walletRepository).findByUserIdAndCurrency(1L, Currency.BTC);
    }

    @Test
    void shouldThrowExceptionWhenInsufficientBalanceForSell() {
        // Given
        Wallet insufficientWallet = Wallet.builder()
            .id(2L)
            .userId(1L)
            .currency(Currency.BTC)
            .balance(new BigDecimal("0.1")) // Less than required 0.5
            .build();
        
        when(walletRepository.findByUserIdAndCurrency(1L, Currency.BTC))
            .thenReturn(Optional.of(insufficientWallet));

        // When/Then
        assertThatThrownBy(() -> riskEngine.validateOrder(sellOrder))
            .isInstanceOf(RiskException.class)
            .hasMessageContaining("Insufficient balance");
    }

    @Test
    void shouldThrowExceptionWhenWalletNotFound() {
        // Given
        when(walletRepository.findByUserIdAndCurrency(1L, Currency.USDT))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> riskEngine.validateOrder(buyOrder))
            .isInstanceOf(RiskException.class)
            .hasMessageContaining("Wallet not found");
    }

    @Test
    void shouldValidateExposureLimitForLimitOrder() {
        // Given
        when(walletRepository.findByUserIdAndCurrency(1L, Currency.USDT))
            .thenReturn(Optional.of(usdtWallet));
        when(priceFeed.getPrice("BTC/USDT"))
            .thenReturn(Optional.of(new BigDecimal("50000")));
        when(walletRepository.findByUserId(1L))
            .thenReturn(Arrays.asList(usdtWallet, btcWallet));
        when(priceFeed.getPrice("BTC/USDT"))
            .thenReturn(Optional.of(new BigDecimal("50000")));

        // When
        riskEngine.validateOrder(buyOrder);

        // Then
        verify(walletRepository).findByUserId(1L);
    }

    @Test
    void shouldThrowExceptionWhenExposureLimitExceeded() {
        // Given
        ReflectionTestUtils.setField(riskEngine, "maxExposureUsdt", new BigDecimal("1000"));
        
        Wallet largeBtcWallet = Wallet.builder()
            .id(3L)
            .userId(1L)
            .currency(Currency.BTC)
            .balance(new BigDecimal("10")) // Large position
            .build();
        
        when(walletRepository.findByUserIdAndCurrency(1L, Currency.USDT))
            .thenReturn(Optional.of(usdtWallet));
        when(priceFeed.getPrice("BTC/USDT"))
            .thenReturn(Optional.of(new BigDecimal("50000")));
        when(walletRepository.findByUserId(1L))
            .thenReturn(Arrays.asList(usdtWallet, largeBtcWallet));
        when(priceFeed.getPrice("BTC/USDT"))
            .thenReturn(Optional.of(new BigDecimal("50000")));

        // When/Then
        assertThatThrownBy(() -> riskEngine.validateOrder(buyOrder))
            .isInstanceOf(RiskException.class)
            .hasMessageContaining("Exposure limit exceeded");
    }

    @Test
    void shouldSkipValidationWhenRiskEngineDisabled() {
        // Given
        ReflectionTestUtils.setField(riskEngine, "riskEnabled", false);

        // When
        riskEngine.validateOrder(buyOrder);

        // Then
        verify(walletRepository, never()).findByUserIdAndCurrency(any(), any());
    }

    @Test
    void shouldSkipExposureCheckForMarketOrders() {
        // Given
        Order marketOrder = Order.builder()
            .id(3L)
            .userId(1L)
            .type(OrderType.MARKET)
            .side(OrderSide.BUY)
            .baseCurrency(Currency.BTC)
            .quoteCurrency(Currency.USDT)
            .quantity(new BigDecimal("0.1"))
            .build();
        
        when(walletRepository.findByUserIdAndCurrency(1L, Currency.USDT))
            .thenReturn(Optional.of(usdtWallet));
        when(priceFeed.getPrice("BTC/USDT"))
            .thenReturn(Optional.of(new BigDecimal("50000")));

        // When
        riskEngine.validateOrder(marketOrder);

        // Then
        verify(walletRepository, never()).findByUserId(any());
    }
}
