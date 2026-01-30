package com.example.cryptoengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Main application class for Cryptocurrency Wallet & Trading Engine Simulator.
 * 
 * This is a simulation system (NOT connected to real blockchain/exchange) that demonstrates:
 * - Atomic balance updates with optimistic locking
 * - Event-driven architecture via Kafka
 * - Thread-safe order matching engine
 * - Basic risk management
 * - Production-grade patterns and practices
 */
@SpringBootApplication
@EnableKafka
public class CryptoWalletEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(CryptoWalletEngineApplication.class, args);
    }
}
