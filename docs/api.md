# API Documentation

Complete REST API reference for the Cryptocurrency Wallet & Trading Engine Simulator.

## Base URL

```
http://localhost:8080/api/v1
```

## Authentication

Currently, the API uses query parameters for user identification. In production, this would be replaced with JWT tokens or API keys.

## Common Response Formats

### Success Response
```json
{
  "id": 1,
  "email": "user@example.com",
  ...
}
```

### Error Response
```json
{
  "code": "BAD_REQUEST",
  "message": "Validation failed: {field: error}",
  "timestamp": "2024-01-15T10:30:00",
  "details": {
    "field": "error message"
  }
}
```

## Endpoints

### Users

#### Create User
```http
POST /api/v1/users
Content-Type: application/json

{
  "email": "trader@example.com",
  "name": "John Doe"
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "email": "trader@example.com",
  "name": "John Doe",
  "createdAt": "2024-01-15T10:30:00"
}
```

**Validation:**
- `email`: Required, valid email format
- `name`: Required, 1-100 characters

---

#### Get User
```http
GET /api/v1/users/{id}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "email": "trader@example.com",
  "name": "John Doe",
  "createdAt": "2024-01-15T10:30:00"
}
```

---

### Wallets

#### Deposit Funds
```http
POST /api/v1/wallets/deposit?userId={userId}&currency={currency}
Content-Type: application/json

{
  "amount": 10000.00,
  "idempotencyKey": "optional-key-123"
}
```

**Query Parameters:**
- `userId` (required): User ID
- `currency` (required): Currency code (USDT, BTC, ETH)

**Request Body:**
- `amount` (required): Deposit amount (min: 0.00000001)
- `idempotencyKey` (optional): Idempotency key

**Response:** `200 OK`
```json
{
  "walletId": 1,
  "currency": "USDT",
  "balance": 10000.00
}
```

**Example:**
```bash
curl -X POST "http://localhost:8080/api/v1/wallets/deposit?userId=1&currency=USDT" \
  -H "Content-Type: application/json" \
  -d '{"amount": 10000}'
```

---

#### Get All Balances
```http
GET /api/v1/wallets/balances?userId={userId}
```

**Response:** `200 OK`
```json
[
  {
    "walletId": 1,
    "currency": "USDT",
    "balance": 10000.00
  },
  {
    "walletId": 2,
    "currency": "BTC",
    "balance": 0.5
  }
]
```

---

#### Get Specific Balance
```http
GET /api/v1/wallets/balance?userId={userId}&currency={currency}
```

**Response:** `200 OK`
```json
{
  "walletId": 1,
  "currency": "USDT",
  "balance": 10000.00
}
```

---

### Orders

#### Place Order
```http
POST /api/v1/orders?userId={userId}
Content-Type: application/json

{
  "type": "LIMIT",
  "side": "BUY",
  "baseCurrency": "BTC",
  "quoteCurrency": "USDT",
  "price": 50000.00,
  "quantity": 0.1,
  "idempotencyKey": "optional-key-123"
}
```

**Query Parameters:**
- `userId` (required): User ID

**Request Body:**
- `type` (required): `LIMIT` or `MARKET`
- `side` (required): `BUY` or `SELL`
- `baseCurrency` (required): Base currency (BTC, ETH)
- `quoteCurrency` (required): Quote currency (USDT)
- `price` (required for LIMIT): Limit price
- `quantity` (required): Order quantity (min: 0.00000001)
- `idempotencyKey` (optional): Idempotency key

**Response:** `201 Created`
```json
{
  "id": 1,
  "userId": 1,
  "type": "LIMIT",
  "side": "BUY",
  "baseCurrency": "BTC",
  "quoteCurrency": "USDT",
  "symbol": "BTC/USDT",
  "price": 50000.00,
  "quantity": 0.1,
  "filledQuantity": 0.0,
  "remainingQuantity": 0.1,
  "status": "OPEN",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

**Example - Limit Buy Order:**
```bash
curl -X POST "http://localhost:8080/api/v1/orders?userId=1" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "LIMIT",
    "side": "BUY",
    "baseCurrency": "BTC",
    "quoteCurrency": "USDT",
    "price": 50000,
    "quantity": 0.1
  }'
```

**Example - Market Sell Order:**
```bash
curl -X POST "http://localhost:8080/api/v1/orders?userId=1" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "MARKET",
    "side": "SELL",
    "baseCurrency": "BTC",
    "quoteCurrency": "USDT",
    "quantity": 0.5
  }'
```

**Validation Rules:**
- MARKET orders cannot have `price`
- LIMIT orders must have `price`
- Sufficient balance required (quote currency for BUY, base currency for SELL)
- Risk checks (exposure limits)

---

#### Cancel Order
```http
POST /api/v1/orders/{orderId}/cancel?userId={userId}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "status": "CANCELLED",
  ...
}
```

**Errors:**
- `400 Bad Request`: Order not found
- `409 Conflict`: Order cannot be cancelled (already filled)

---

#### Get Order
```http
GET /api/v1/orders/{orderId}?userId={userId}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "userId": 1,
  "type": "LIMIT",
  "side": "BUY",
  "status": "OPEN",
  ...
}
```

---

#### Get User Orders
```http
GET /api/v1/orders?userId={userId}
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "status": "OPEN",
    ...
  },
  {
    "id": 2,
    "status": "FILLED",
    ...
  }
]
```

---

### Market Data

#### Get Order Book
```http
GET /api/v1/market/orderbook/{symbol}
```

**Path Parameters:**
- `symbol`: Trading pair (e.g., `BTC/USDT`, `ETH/USDT`)

**Response:** `200 OK`
```json
{
  "symbol": "BTC/USDT",
  "bids": [
    {
      "price": 50000.00,
      "quantity": 0.5
    },
    {
      "price": 49999.00,
      "quantity": 1.0
    }
  ],
  "asks": [
    {
      "price": 50001.00,
      "quantity": 0.3
    },
    {
      "price": 50002.00,
      "quantity": 0.7
    }
  ]
}
```

**Example:**
```bash
curl http://localhost:8080/api/v1/market/orderbook/BTC/USDT
```

---

#### Get Recent Trades
```http
GET /api/v1/market/trades/{symbol}?limit=100
```

**Path Parameters:**
- `symbol`: Trading pair

**Query Parameters:**
- `limit` (optional): Number of trades to return (default: 100)

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "orderIdBuy": 10,
    "orderIdSell": 11,
    "price": 50000.00,
    "quantity": 0.1,
    "baseCurrency": "BTC",
    "quoteCurrency": "USDT",
    "symbol": "BTC/USDT",
    "timestamp": "2024-01-15T10:30:00"
  }
]
```

**Example:**
```bash
curl "http://localhost:8080/api/v1/market/trades/BTC/USDT?limit=50"
```

---

## Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `BAD_REQUEST` | 400 | Invalid request parameters |
| `VALIDATION_ERROR` | 400 | Request validation failed |
| `CONFLICT` | 409 | Resource conflict (e.g., order already filled) |
| `RISK_VALIDATION_FAILED` | 403 | Risk check failed (insufficient balance, exposure limit) |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

## Examples

### Complete Trading Flow

```bash
# 1. Create user
USER_ID=$(curl -s -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"email":"trader@example.com","name":"John Doe"}' \
  | jq -r '.id')

# 2. Deposit USDT
curl -X POST "http://localhost:8080/api/v1/wallets/deposit?userId=$USER_ID&currency=USDT" \
  -H "Content-Type: application/json" \
  -d '{"amount": 10000}'

# 3. Place limit buy order
ORDER_ID=$(curl -s -X POST "http://localhost:8080/api/v1/orders?userId=$USER_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "LIMIT",
    "side": "BUY",
    "baseCurrency": "BTC",
    "quoteCurrency": "USDT",
    "price": 50000,
    "quantity": 0.1
  }' | jq -r '.id')

# 4. Check order book
curl http://localhost:8080/api/v1/market/orderbook/BTC/USDT

# 5. Check balances
curl "http://localhost:8080/api/v1/wallets/balances?userId=$USER_ID"

# 6. Cancel order (if needed)
curl -X POST "http://localhost:8080/api/v1/orders/$ORDER_ID/cancel?userId=$USER_ID"
```

## Rate Limiting

Currently not implemented. In production, consider:
- Per-user rate limits
- Per-IP rate limits
- Token bucket algorithm

## Idempotency

Orders and deposits support idempotency keys:

```json
{
  "amount": 10000,
  "idempotencyKey": "deposit-2024-01-15-001"
}
```

If the same idempotency key is used twice, the operation returns the original result without creating a duplicate.
