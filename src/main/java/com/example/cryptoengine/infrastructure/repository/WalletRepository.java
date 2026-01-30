package com.example.cryptoengine.infrastructure.repository;

import com.example.cryptoengine.domain.Currency;
import com.example.cryptoengine.domain.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    
    /**
     * Finds wallet by user ID and currency with optimistic lock.
     * This ensures atomic updates and prevents concurrent modification issues.
     */
    @Lock(LockModeType.OPTIMISTIC)
    Optional<Wallet> findByUserIdAndCurrency(Long userId, Currency currency);
    
    List<Wallet> findByUserId(Long userId);
    
    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId")
    List<Wallet> findAllByUserId(@Param("userId") Long userId);
}
