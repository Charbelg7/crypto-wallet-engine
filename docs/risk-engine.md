# Risk Engine Documentation

Comprehensive documentation of the risk management system.

## Overview

The Risk Engine performs pre-trade validation to ensure:
- **Sufficient Balance**: User has enough funds for the order
- **Exposure Limits**: Total position value doesn't exceed limits
- **Order Validity**: Order parameters are valid

## Risk Checks

### 1. Balance Validation

**Purpose:** Ensure user has sufficient balance before placing an order.

**For BUY Orders:**
- Requires quote currency (e.g., USDT)
- Amount = `quantity × price` (for LIMIT orders)
- Amount = `quantity × estimatedPrice × 1.1` (for MARKET orders, 10% buffer)

**For SELL Orders:**
- Requires base currency (e.g., BTC)
- Amount = `quantity`

**Implementation:**
```java
private void validateBalance(Order order) {
    Currency requiredCurrency = order.getSide() == OrderSide.BUY 
        ? order.getQuoteCurrency() 
        : order.getBaseCurrency();
    
    BigDecimal requiredAmount = calculateRequiredAmount(order);
    
    Wallet wallet = walletRepository.findByUserIdAndCurrency(
        order.getUserId(), requiredCurrency);
    
    if (wallet.getBalance().compareTo(requiredAmount) < 0) {
        throw new RiskException("Insufficient balance");
    }
}
```

**Error Response:**
```json
{
  "code": "RISK_VALIDATION_FAILED",
  "message": "Insufficient balance. Required: 5000.00 USDT, Available: 3000.00 USDT"
}
```

---

### 2. Exposure Limit Validation

**Purpose:** Prevent users from exceeding maximum position value.

**Calculation:**
1. Sum all base currency positions (converted to USDT)
2. Add new order's exposure (for BUY orders)
3. Compare against maximum exposure limit

**Exposure Calculation:**
```java
private void validateExposureLimit(Order order) {
    List<Wallet> wallets = walletRepository.findByUserId(order.getUserId());
    
    BigDecimal totalExposure = BigDecimal.ZERO;
    
    // Calculate existing positions
    for (Wallet wallet : wallets) {
        if (wallet.getCurrency() != Currency.USDT && 
            wallet.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal price = priceFeed.getPrice(
                wallet.getCurrency().name() + "/USDT").orElse(BigDecimal.ZERO);
            totalExposure = totalExposure.add(
                wallet.getBalance().multiply(price));
        }
    }
    
    // Add new order exposure (for BUY orders)
    if (order.getSide() == OrderSide.BUY) {
        BigDecimal orderValue = order.getQuantity()
            .multiply(priceFeed.getPrice(order.getSymbol()).orElse(BigDecimal.ZERO));
        totalExposure = totalExposure.add(orderValue);
    }
    
    if (totalExposure.compareTo(maxExposureUsdt) > 0) {
        throw new RiskException("Exposure limit exceeded");
    }
}
```

**Configuration:**
```properties
crypto.risk.max-exposure-usdt=100000
crypto.risk.enabled=true
```

**Error Response:**
```json
{
  "code": "RISK_VALIDATION_FAILED",
  "message": "Exposure limit exceeded. Current: 105000.00 USDT, Limit: 100000.00 USDT"
}
```

---

## Price Feed

### Simulated Price Feed

**Purpose:** Provide current market prices for risk calculations.

**Current Implementation:**
```java
@Component
public class PriceFeed {
    private final Map<String, BigDecimal> prices = new ConcurrentHashMap<>();
    
    public PriceFeed() {
        prices.put("BTC/USDT", new BigDecimal("50000.00"));
        prices.put("ETH/USDT", new BigDecimal("3000.00"));
    }
    
    public Optional<BigDecimal> getPrice(String symbol) {
        return Optional.ofNullable(prices.get(symbol));
    }
    
    public void updatePrice(String symbol, BigDecimal price) {
        prices.put(symbol, price);
    }
}
```

**Usage:**
- Risk calculations
- Market order price estimation
- Exposure limit calculations

**Future Enhancement:**
- Connect to real market data API
- WebSocket for real-time price updates
- Price history for backtesting

---

## Risk Engine Flow

### Order Placement Flow

```
1. OrderService.placeOrder()
   ↓
2. RiskEngine.validateOrder(order)
   ↓
3. validateBalance(order)
   ├─ Check wallet balance
   ├─ Calculate required amount
   └─ Throw RiskException if insufficient
   ↓
4. validateExposureLimit(order) [if LIMIT order]
   ├─ Calculate current exposure
   ├─ Add new order exposure
   └─ Throw RiskException if exceeds limit
   ↓
5. If validation passes → Continue order placement
```

### Risk Check Timing

**Pre-Trade Validation:**
- Performed **before** order is saved
- Prevents invalid orders from entering system
- Clear error messages for users

**Post-Trade Monitoring:**
- Not currently implemented
- Future: Real-time position monitoring
- Future: Margin calls, forced liquidation

---

## Configuration

### Application Properties

```properties
# Risk Engine Configuration
crypto.risk.max-exposure-usdt=100000
crypto.risk.enabled=true
```

### Environment Variables

```bash
CRYPTO_RISK_MAX_EXPOSURE_USDT=100000
CRYPTO_RISK_ENABLED=true
```

---

## Error Handling

### RiskException

**Custom Exception:**
```java
public class RiskException extends RuntimeException {
    public RiskException(String message) {
        super(message);
    }
}
```

**Global Handler:**
```java
@ExceptionHandler(RiskException.class)
public ResponseEntity<ErrorResponse> handleRiskException(RiskException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ErrorResponse(
            "RISK_VALIDATION_FAILED",
            ex.getMessage(),
            LocalDateTime.now()
        ));
}
```

---

## Testing

### Unit Tests

```java
@Test
void testInsufficientBalance() {
    // Given
    Order order = createOrderWithInsufficientBalance();
    
    // When/Then
    assertThatThrownBy(() -> riskEngine.validateOrder(order))
        .isInstanceOf(RiskException.class)
        .hasMessageContaining("Insufficient balance");
}
```

### Integration Tests

```java
@Test
void testExposureLimit() {
    // Create user with positions
    // Place order that exceeds limit
    // Verify RiskException thrown
}
```

---

## Future Enhancements

### 1. Advanced Risk Checks

- **Position Limits**: Per-currency position limits
- **Margin Requirements**: Margin-based trading
- **Leverage Limits**: Maximum leverage per user
- **Velocity Limits**: Maximum orders per time period

### 2. Real-Time Monitoring

- **Position Tracking**: Real-time position updates
- **P&L Calculation**: Profit/loss tracking
- **Risk Alerts**: Notifications for high risk
- **Auto-Liquidation**: Automatic position closure

### 3. Market Data Integration

- **Real-Time Prices**: WebSocket price feeds
- **Order Book Depth**: Real order book data
- **Historical Prices**: Price history for backtesting
- **Volatility Calculations**: Risk-adjusted limits

### 4. Regulatory Compliance

- **KYC Checks**: Know Your Customer validation
- **AML Screening**: Anti-Money Laundering checks
- **Transaction Limits**: Regulatory limits
- **Reporting**: Compliance reporting

---

## Best Practices

1. **Fail Fast**: Validate before order creation
2. **Clear Errors**: Provide actionable error messages
3. **Configurable**: Make limits configurable
4. **Auditable**: Log all risk checks
5. **Testable**: Unit test all risk logic

---

## Summary

The Risk Engine provides:
- ✅ Pre-trade balance validation
- ✅ Exposure limit checks
- ✅ Configurable risk parameters
- ✅ Clear error messages
- ✅ Extensible design for future enhancements

**Current State:** Basic risk checks implemented  
**Future State:** Advanced risk management with real-time monitoring
