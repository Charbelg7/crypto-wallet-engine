package com.example.cryptoengine.infrastructure.repository;

import com.example.cryptoengine.domain.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
    
    @Query("SELECT t FROM Trade t WHERE t.baseCurrency = :baseCurrency " +
           "AND t.quoteCurrency = :quoteCurrency " +
           "ORDER BY t.timestamp DESC")
    List<Trade> findBySymbol(
        @Param("baseCurrency") String baseCurrency,
        @Param("quoteCurrency") String quoteCurrency
    );
    
    @Query("SELECT t FROM Trade t WHERE t.baseCurrency = :baseCurrency " +
           "AND t.quoteCurrency = :quoteCurrency " +
           "AND t.timestamp >= :since " +
           "ORDER BY t.timestamp DESC")
    List<Trade> findBySymbolSince(
        @Param("baseCurrency") String baseCurrency,
        @Param("quoteCurrency") String quoteCurrency,
        @Param("since") LocalDateTime since
    );
    
    List<Trade> findByOrderIdBuyOrOrderIdSell(Long orderIdBuy, Long orderIdSell);
}
