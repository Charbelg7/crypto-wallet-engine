package com.example.cryptoengine.risk;

/**
 * Exception thrown when risk validation fails.
 */
public class RiskException extends RuntimeException {
    
    public RiskException(String message) {
        super(message);
    }
    
    public RiskException(String message, Throwable cause) {
        super(message, cause);
    }
}
