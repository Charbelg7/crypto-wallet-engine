package com.example.cryptoengine.domain;

import java.util.Set;

/**
 * Supported cryptocurrency currencies.
 * In a real system, this would be more comprehensive.
 */
public enum Currency {
    USDT("Tether", 6),  // Stablecoin, 6 decimals
    BTC("Bitcoin", 8),  // Bitcoin, 8 decimals
    ETH("Ethereum", 18); // Ethereum, 18 decimals

    private final String name;
    private final int decimals;

    Currency(String name, int decimals) {
        this.name = name;
        this.decimals = decimals;
    }

    public String getName() {
        return name;
    }

    public int getDecimals() {
        return decimals;
    }

    public static final Set<Currency> SUPPORTED_CURRENCIES = Set.of(USDT, BTC, ETH);
}
