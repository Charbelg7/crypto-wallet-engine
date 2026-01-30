package com.example.cryptoengine.domain.entity;

import com.example.cryptoengine.domain.Currency;
import com.example.cryptoengine.domain.OrderSide;
import com.example.cryptoengine.domain.OrderStatus;
import com.example.cryptoengine.domain.OrderType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order entity representing a trading order (buy or sell).
 * Supports both LIMIT and MARKET order types.
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_user", columnList = "userId"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_symbol_status", columnList = "baseCurrency,quoteCurrency,status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Currency baseCurrency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Currency quoteCurrency;

    /**
     * Limit price (null for MARKET orders).
     * For BUY orders: maximum price willing to pay.
     * For SELL orders: minimum price willing to accept.
     */
    @Column(precision = 18, scale = 8)
    private BigDecimal price;

    /**
     * Original order quantity.
     */
    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;

    /**
     * Quantity that has been filled so far.
     */
    @Column(nullable = false, precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal filledQuantity = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.OPEN;

    /**
     * Idempotency key to prevent duplicate order submissions.
     */
    @Column(length = 100)
    private String idempotencyKey;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (filledQuantity == null) {
            filledQuantity = BigDecimal.ZERO;
        }
        if (status == null) {
            status = OrderStatus.OPEN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Returns the remaining quantity to be filled.
     */
    public BigDecimal getRemainingQuantity() {
        return quantity.subtract(filledQuantity);
    }

    /**
     * Returns true if order is fully filled.
     */
    public boolean isFilled() {
        return filledQuantity.compareTo(quantity) >= 0;
    }

    /**
     * Returns true if order can be cancelled.
     */
    public boolean canCancel() {
        return status == OrderStatus.OPEN || status == OrderStatus.PARTIAL;
    }

    /**
     * Returns the trading symbol (e.g., "BTC/USDT").
     */
    public String getSymbol() {
        return baseCurrency.name() + "/" + quoteCurrency.name();
    }

    /**
     * Fills a portion of this order.
     */
    public void fill(BigDecimal fillQuantity) {
        if (fillQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Fill quantity must be positive");
        }
        BigDecimal newFilled = filledQuantity.add(fillQuantity);
        if (newFilled.compareTo(quantity) > 0) {
            throw new IllegalStateException("Cannot fill more than order quantity");
        }
        filledQuantity = newFilled;
        if (isFilled()) {
            status = OrderStatus.FILLED;
        } else {
            status = OrderStatus.PARTIAL;
        }
    }

    /**
     * Cancels the order.
     */
    public void cancel() {
        if (!canCancel()) {
            throw new IllegalStateException("Order cannot be cancelled in current status: " + status);
        }
        status = OrderStatus.CANCELLED;
    }
}
