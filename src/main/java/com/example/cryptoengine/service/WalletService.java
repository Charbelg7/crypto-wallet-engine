package com.example.cryptoengine.service;

import com.example.cryptoengine.application.dto.DepositRequest;
import com.example.cryptoengine.application.dto.WalletBalanceResponse;
import com.example.cryptoengine.domain.Currency;
import com.example.cryptoengine.domain.entity.Wallet;
import com.example.cryptoengine.domain.event.BalanceUpdatedEvent;
import com.example.cryptoengine.infrastructure.kafka.KafkaEventProducer;
import com.example.cryptoengine.infrastructure.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for wallet operations with atomic balance updates.
 * Uses optimistic locking to prevent concurrent modification issues.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final KafkaEventProducer eventProducer;

    /**
     * Deposits funds into a user's wallet.
     * Atomic operation with optimistic locking.
     * 
     * @param userId User ID
     * @param currency Currency to deposit
     * @param request Deposit request with amount
     * @return Updated wallet balance
     */
    @Transactional
    public WalletBalanceResponse deposit(Long userId, Currency currency, DepositRequest request) {
        Wallet wallet = walletRepository.findByUserIdAndCurrency(userId, currency)
            .orElseGet(() -> {
                Wallet newWallet = Wallet.builder()
                    .userId(userId)
                    .currency(currency)
                    .balance(BigDecimal.ZERO)
                    .build();
                return walletRepository.save(newWallet);
            });

        BigDecimal oldBalance = wallet.getBalance();
        wallet.deposit(request.amount());
        wallet = walletRepository.save(wallet);

        log.info("Deposited {} {} to wallet {} (user {})", 
            request.amount(), currency, wallet.getId(), userId);

        // Publish BalanceUpdatedEvent
        BalanceUpdatedEvent event = BalanceUpdatedEvent.builder()
            .eventId(UUID.randomUUID())
            .timestamp(java.time.LocalDateTime.now())
            .walletId(wallet.getId())
            .userId(userId)
            .currency(currency)
            .newBalance(wallet.getBalance())
            .changeAmount(request.amount())
            .reason("DEPOSIT")
            .build();
        eventProducer.publishBalanceUpdated(event);

        return new WalletBalanceResponse(wallet.getId(), wallet.getCurrency(), wallet.getBalance());
    }

    /**
     * Withdraws funds from a user's wallet.
     * Atomic operation with optimistic locking.
     * 
     * @param userId User ID
     * @param currency Currency to withdraw
     * @param amount Amount to withdraw
     * @return Updated wallet balance
     */
    @Transactional
    public WalletBalanceResponse withdraw(Long userId, Currency currency, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserIdAndCurrency(userId, currency)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Wallet not found for user %d, currency %s", userId, currency)));

        wallet.withdraw(amount);
        wallet = walletRepository.save(wallet);

        log.info("Withdrew {} {} from wallet {} (user {})", 
            amount, currency, wallet.getId(), userId);

        return new WalletBalanceResponse(wallet.getId(), wallet.getCurrency(), wallet.getBalance());
    }

    /**
     * Gets all wallet balances for a user.
     */
    public List<WalletBalanceResponse> getBalances(Long userId) {
        return walletRepository.findByUserId(userId).stream()
            .map(w -> new WalletBalanceResponse(w.getId(), w.getCurrency(), w.getBalance()))
            .collect(Collectors.toList());
    }

    /**
     * Gets a specific wallet balance.
     */
    public WalletBalanceResponse getBalance(Long userId, Currency currency) {
        Wallet wallet = walletRepository.findByUserIdAndCurrency(userId, currency)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Wallet not found for user %d, currency %s", userId, currency)));
        return new WalletBalanceResponse(wallet.getId(), wallet.getCurrency(), wallet.getBalance());
    }
}
