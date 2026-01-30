package com.example.cryptoengine.infrastructure.repository;

import com.example.cryptoengine.domain.Currency;
import com.example.cryptoengine.domain.OrderStatus;
import com.example.cryptoengine.domain.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByUserId(Long userId);
    
    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);
    
    Optional<Order> findByIdAndUserId(Long id, Long userId);
    
    @Query("SELECT o FROM Order o WHERE o.baseCurrency = :baseCurrency " +
           "AND o.quoteCurrency = :quoteCurrency " +
           "AND o.status IN :statuses " +
           "ORDER BY o.createdAt ASC")
    List<Order> findBySymbolAndStatuses(
        @Param("baseCurrency") Currency baseCurrency,
        @Param("quoteCurrency") Currency quoteCurrency,
        @Param("statuses") List<OrderStatus> statuses
    );
    
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
}
