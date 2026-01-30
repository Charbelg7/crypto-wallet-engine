package com.example.cryptoengine.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Trade entity representing an executed trade between two orders.
 */
@Entity
@Table(name = "trades", indexes = {
    @Index(name = "idx_trade_buy_order", columnList = "orderIdBuy"),
    @Index(name = "idx_trade_sell_order", columnList = "orderIdSell"),
    @Index(name = "idx_trade_symbol_timestamp", columnList = "baseCurrency,quoteCurrency,timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The buy order that participated in this trade.
     */
    @Column(nullable = false)
    private Long orderIdBuy;

    /**
     * The sell order that participated in this trade.
     */
    @Column(nullable = false)
    private Long orderIdSell;

    /**
     * Execution price of the trade.
     */
    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal price;

    /**
     * Quantity traded.
     */
    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;

    /**
     * Base currency (e.g., BTC).
     */
    @Column(nullable = false, length = 10)
    private String baseCurrency;

    /**
     * Quote currency (e.g., USDT).
     */
    @Column(nullable = false, length = 10)
    private String quoteCurrency;

    /**
     * Timestamp when trade was executed.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
