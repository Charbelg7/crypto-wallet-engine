package com.example.cryptoengine.risk;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple simulated price feed for risk calculations.
 * In a real system, this would connect to market data providers.
 * 
 * For simulation, we use fixed prices that can be updated.
 */
@Component
public class PriceFeed {

    // Simulated prices (in USDT)
    private final Map<String, BigDecimal> prices = new ConcurrentHashMap<>();

    public PriceFeed() {
        // Initialize with some default prices
        prices.put("BTC/USDT", new BigDecimal("50000.00"));
        prices.put("ETH/USDT", new BigDecimal("3000.00"));
        prices.put("BTC/USDT", new BigDecimal("50000.00")); // Duplicate, but safe
    }

    /**
     * Gets the current price for a trading symbol.
     * 
     * @param symbol Trading symbol (e.g., "BTC/USDT")
     * @return Optional price, empty if symbol not found
     */
    public Optional<BigDecimal> getPrice(String symbol) {
        return Optional.ofNullable(prices.get(symbol));
    }

    /**
     * Updates the price for a symbol (useful for testing or simulation).
     */
    public void updatePrice(String symbol, BigDecimal price) {
        prices.put(symbol, price);
    }

    /**
     * Gets all available symbols.
     */
    public java.util.Set<String> getAvailableSymbols() {
        return prices.keySet();
    }
}
