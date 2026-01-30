package com.example.cryptoengine.service;

import com.example.cryptoengine.application.dto.DepositRequest;
import com.example.cryptoengine.application.dto.WalletBalanceResponse;
import com.example.cryptoengine.domain.Currency;
import com.example.cryptoengine.domain.entity.Wallet;
import com.example.cryptoengine.infrastructure.kafka.KafkaEventProducer;
import com.example.cryptoengine.infrastructure.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private KafkaEventProducer eventProducer;

    @InjectMocks
    private WalletService walletService;

    private Long userId;
    private DepositRequest depositRequest;
    private Wallet existingWallet;
    private Wallet newWallet;

    @BeforeEach
    void setUp() {
        userId = 1L;
        depositRequest = new DepositRequest(new BigDecimal("1000"), null);
        
        existingWallet = Wallet.builder()
            .id(1L)
            .userId(userId)
            .currency(Currency.USDT)
            .balance(new BigDecimal("5000"))
            .version(1L)
            .build();
        
        newWallet = Wallet.builder()
            .id(2L)
            .userId(userId)
            .currency(Currency.BTC)
            .balance(BigDecimal.ZERO)
            .version(0L)
            .build();
    }

    @Test
    void shouldDepositToExistingWallet() {
        // Given
        when(walletRepository.findByUserIdAndCurrency(userId, Currency.USDT))
            .thenReturn(Optional.of(existingWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(existingWallet);

        // When
        WalletBalanceResponse result = walletService.deposit(userId, Currency.USDT, depositRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.currency()).isEqualTo(Currency.USDT);
        assertThat(result.balance()).isEqualByComparingTo(new BigDecimal("6000"));
        
        verify(walletRepository).findByUserIdAndCurrency(userId, Currency.USDT);
        verify(walletRepository).save(existingWallet);
        verify(eventProducer).publishBalanceUpdated(any());
    }

    @Test
    void shouldCreateNewWalletOnDeposit() {
        // Given
        when(walletRepository.findByUserIdAndCurrency(userId, Currency.BTC))
            .thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenReturn(newWallet);

        // When
        WalletBalanceResponse result = walletService.deposit(userId, Currency.BTC, depositRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.currency()).isEqualTo(Currency.BTC);
        
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository, atLeastOnce()).save(walletCaptor.capture());
        Wallet savedWallet = walletCaptor.getValue();
        assertThat(savedWallet.getUserId()).isEqualTo(userId);
        assertThat(savedWallet.getCurrency()).isEqualTo(Currency.BTC);
        assertThat(savedWallet.getBalance()).isEqualByComparingTo(new BigDecimal("1000"));
    }

    @Test
    void shouldWithdrawSuccessfully() {
        // Given
        BigDecimal withdrawAmount = new BigDecimal("500");
        when(walletRepository.findByUserIdAndCurrency(userId, Currency.USDT))
            .thenReturn(Optional.of(existingWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(existingWallet);

        // When
        WalletBalanceResponse result = walletService.withdraw(userId, Currency.USDT, withdrawAmount);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.balance()).isEqualByComparingTo(new BigDecimal("4500"));
        
        verify(walletRepository).findByUserIdAndCurrency(userId, Currency.USDT);
        verify(walletRepository).save(existingWallet);
    }

    @Test
    void shouldThrowExceptionWhenWithdrawingFromNonExistentWallet() {
        // Given
        when(walletRepository.findByUserIdAndCurrency(userId, Currency.BTC))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> walletService.withdraw(userId, Currency.BTC, new BigDecimal("100")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");

        verify(walletRepository).findByUserIdAndCurrency(userId, Currency.BTC);
        verify(walletRepository, never()).save(any());
    }

    @Test
    void shouldGetAllBalances() {
        // Given
        Wallet wallet1 = Wallet.builder()
            .id(1L)
            .userId(userId)
            .currency(Currency.USDT)
            .balance(new BigDecimal("1000"))
            .build();
        
        Wallet wallet2 = Wallet.builder()
            .id(2L)
            .userId(userId)
            .currency(Currency.BTC)
            .balance(new BigDecimal("0.5"))
            .build();
        
        when(walletRepository.findByUserId(userId))
            .thenReturn(Arrays.asList(wallet1, wallet2));

        // When
        List<WalletBalanceResponse> result = walletService.getBalances(userId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).currency()).isEqualTo(Currency.USDT);
        assertThat(result.get(1).currency()).isEqualTo(Currency.BTC);
        
        verify(walletRepository).findByUserId(userId);
    }

    @Test
    void shouldGetSpecificBalance() {
        // Given
        when(walletRepository.findByUserIdAndCurrency(userId, Currency.USDT))
            .thenReturn(Optional.of(existingWallet));

        // When
        WalletBalanceResponse result = walletService.getBalance(userId, Currency.USDT);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.currency()).isEqualTo(Currency.USDT);
        assertThat(result.balance()).isEqualByComparingTo(new BigDecimal("5000"));
    }
}
