package com.example.cryptoengine.risk;

import com.example.cryptoengine.domain.Currency;
import com.example.cryptoengine.domain.OrderSide;
import com.example.cryptoengine.domain.OrderType;
import com.example.cryptoengine.domain.entity.Order;
import com.example.cryptoengine.domain.entity.Wallet;
import com.example.cryptoengine.infrastructure.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Risk engine that performs pre-trade checks:
 * - Sufficient balance validation
 * - Exposure limit checks (total position value)
 * 
 * Uses a simple price feed simulation for exposure calculations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskEngine {

    private final WalletRepository walletRepository;
    private final PriceFeed priceFeed;

    @Value("${crypto.risk.max-exposure-usdt:100000}")
    private BigDecimal maxExposureUsdt;

    @Value("${crypto.risk.enabled:true}")
    private boolean riskEnabled;

    /**
     * Validates if an order can be placed based on risk rules.
     * 
     * @param order The order to validate
     * @throws RiskException if order violates risk rules
     */
    @Transactional(readOnly = true)
    public void validateOrder(Order order) {
        if (!riskEnabled) {
            log.debug("Risk engine is disabled, skipping validation");
            return;
        }

        // Check sufficient balance
        validateBalance(order);

        // Check exposure limits (for LIMIT orders that will rest in order book)
        if (order.getType() == OrderType.LIMIT) {
            validateExposureLimit(order);
        }
    }

    /**
     * Validates that user has sufficient balance for the order.
     */
    private void validateBalance(Order order) {
        Currency requiredCurrency;
        BigDecimal requiredAmount;

        if (order.getSide() == OrderSide.BUY) {
            // BUY order: need quote currency (e.g., USDT)
            requiredCurrency = order.getQuoteCurrency();
            if (order.getType() == OrderType.MARKET) {
                // For market orders, we need to estimate worst-case cost
                // Use a conservative estimate: best ask price * quantity * 1.1 (10% buffer)
                BigDecimal estimatedPrice = priceFeed.getPrice(order.getSymbol())
                    .orElseThrow(() -> new RiskException("Cannot determine price for market order"));
                requiredAmount = order.getQuantity()
                    .multiply(estimatedPrice)
                    .multiply(new BigDecimal("1.1")); // 10% buffer
            } else {
                // LIMIT order: exact cost
                requiredAmount = order.getQuantity().multiply(order.getPrice());
            }
        } else {
            // SELL order: need base currency (e.g., BTC)
            requiredCurrency = order.getBaseCurrency();
            requiredAmount = order.getQuantity();
        }

        Wallet wallet = walletRepository.findByUserIdAndCurrency(order.getUserId(), requiredCurrency)
            .orElseThrow(() -> new RiskException(
                String.format("Wallet not found for currency: %s", requiredCurrency)));

        if (wallet.getBalance().compareTo(requiredAmount) < 0) {
            throw new RiskException(
                String.format("Insufficient balance. Required: %s %s, Available: %s %s",
                    requiredAmount, requiredCurrency,
                    wallet.getBalance(), requiredCurrency));
        }

        log.debug("Balance check passed for order {}: {} {} available", 
            order.getId(), wallet.getBalance(), requiredCurrency);
    }

    /**
     * Validates that user's total exposure doesn't exceed limits.
     * Exposure = sum of (position value in USDT) for all open positions.
     */
    private void validateExposureLimit(Order order) {
        List<Wallet> wallets = walletRepository.findByUserId(order.getUserId());
        Map<Currency, Wallet> walletMap = wallets.stream()
            .collect(Collectors.toMap(Wallet::getCurrency, Function.identity()));

        BigDecimal totalExposure = BigDecimal.ZERO;

        // Calculate exposure for base currency positions
        if (order.getBaseCurrency() != Currency.USDT) {
            Wallet baseWallet = walletMap.get(order.getBaseCurrency());
            if (baseWallet != null && baseWallet.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal basePrice = priceFeed.getPrice(order.getBaseCurrency().name() + "/USDT")
                    .orElse(BigDecimal.ZERO);
                totalExposure = totalExposure.add(baseWallet.getBalance().multiply(basePrice));
            }
        }

        // Add the new order's exposure
        if (order.getSide() == OrderSide.BUY) {
            // BUY order increases exposure (acquiring base currency)
            BigDecimal orderValue = order.getQuantity()
                .multiply(priceFeed.getPrice(order.getSymbol()).orElse(BigDecimal.ZERO));
            totalExposure = totalExposure.add(orderValue);
        }
        // SELL orders reduce exposure, so we don't add them

        if (totalExposure.compareTo(maxExposureUsdt) > 0) {
            throw new RiskException(
                String.format("Exposure limit exceeded. Current: %s USDT, Limit: %s USDT",
                    totalExposure.setScale(2, RoundingMode.HALF_UP), maxExposureUsdt));
        }

        log.debug("Exposure check passed for order {}: {} USDT exposure", 
            order.getId(), totalExposure.setScale(2, RoundingMode.HALF_UP));
    }
}
