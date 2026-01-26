# Complete API Routing Map

This document shows **exactly** which server handles each API call in your system.

---

## Server Architecture

- **Ingress Server**: `http://localhost:8085` (Port 8085)
- **Server 1**: `http://localhost:8080` (Port 8080) - Default server
- **Server 2**: `http://localhost:8081` (Port 8081)
- **Server 3**: `http://localhost:8082` (Port 8082)

---

## 1. Authentication APIs

### Sign Up (Register)

**Frontend Call:**
```typescript
apiService.register({ username, email, password })
```

**Route:**
```
Frontend → http://localhost:8080/api/auth/register
```

**Backend Controller:**
- **File:** `backend/src/main/java/com/project/matchingengine/controllers/authentication/AuthController.java`
- **Method:** `registerHandler()`
- **Endpoint:** `POST /api/auth/register`
- **Server:** **Server 1 (8080)** - Default server

**Flow:**
```
User → Frontend → Server 1 (8080) → Database → Response
```

---

### Login

**Frontend Call:**
```typescript
apiService.login({ username, password })
```

**Route:**
```
Frontend → http://localhost:8080/api/auth/login
```

**Backend Controller:**
- **File:** `backend/src/main/java/com/project/matchingengine/controllers/authentication/AuthController.java`
- **Method:** `loginHandler()`
- **Endpoint:** `POST /api/auth/login`
- **Server:** **Server 1 (8080)** - Default server

**Flow:**
```
User → Frontend → Server 1 (8080) → Database → JWT Token → Response
```

---

## 2. Order Submission

### Submit Order

**Frontend Call:**
```typescript
apiService.submitOrder(order)
```

**Route:**
```
Frontend → http://localhost:8085/api/orders/submit
```

**Backend Controller:**
- **File:** `backend/src/main/java/com/project/matchingengine/controllers/ingress/IngressController.java`
- **Method:** `submitOrder()`
- **Endpoint:** `POST /api/orders/submit`
- **Server:** **Ingress Server (8085)**

**Flow:**
```
User → Frontend → Ingress (8085) → Kafka Topic "orders" → Matching Servers (8080/8081/8082) → Processing
```

**Details:**
1. Frontend sends order to Ingress Server (8085)
2. Ingress validates order and sends to Kafka topic `orders` with key = `symbol`
3. Kafka distributes to matching servers based on partition assignment
4. Matching servers consume from Kafka and process orders

**Note:** Order goes to **Ingress (8085)**, NOT to symbol-specific servers directly.

---

## 3. User Profile & Details

### Get User Info

**Frontend Call:**
```typescript
apiService.getUserInfo()
```

**Route:**
```
Frontend → http://localhost:8080/api/user/info
```

**Backend Controller:**
- **File:** `backend/src/main/java/com/project/matchingengine/controllers/authentication/UserController.java`
- **Method:** `getUserDetails()`
- **Endpoint:** `GET /api/user/info`
- **Server:** **Server 1 (8080)** - Default server

**Returns:**
- userId, username, email
- ledgerBalance, availableBalance
- holdings (stock positions)

---

### Get User Profile (Full)

**Frontend Call:**
```typescript
apiService.getUserProfile()
```

**Route:**
```
Frontend → http://localhost:8080/api/user/profile
```

**Backend Controller:**
- **File:** `backend/src/main/java/com/project/matchingengine/controllers/authentication/UserController.java`
- **Method:** `getUserProfile()`
- **Endpoint:** `GET /api/user/profile`
- **Server:** **Server 1 (8080)** - Default server

**Returns:**
- User info (userId, username, email, balances, holdings)
- Statistics (totalOrders, pendingOrders, filledOrders, totalTrades, totalTradeValue)
- Recent orders (last 10)
- Recent trades (last 10)

---

### Get User Orders

**Frontend Call:**
```typescript
apiService.getUserOrders()
```

**Route:**
```
Frontend → http://localhost:8080/api/user/orders
```

**Backend Controller:**
- **File:** `backend/src/main/java/com/project/matchingengine/controllers/authentication/UserController.java`
- **Method:** `getUserOrders()`
- **Endpoint:** `GET /api/user/orders`
- **Server:** **Server 1 (8080)** - Default server

---

### Get User Trades

**Frontend Call:**
```typescript
apiService.getUserTrades()
```

**Route:**
```
Frontend → http://localhost:8080/api/user/trades
```

**Backend Controller:**
- **File:** `backend/src/main/java/com/project/matchingengine/controllers/authentication/UserController.java`
- **Method:** `getUserTrades()`
- **Endpoint:** `GET /api/user/trades`
- **Server:** **Server 1 (8080)** - Default server

---

## 4. Stock Data APIs

### Get All Stocks

**Frontend Call:**
```typescript
apiService.getAllStocks()
```

**Route:**
```
Frontend → ALL Servers in parallel:
  - http://localhost:8080/api/stocks/all
  - http://localhost:8081/api/stocks/all
  - http://localhost:8082/api/stocks/all
```

**Backend Controller:**
- **File:** `backend/src/main/java/com/project/matchingengine/controllers/order/StockController.java`
- **Method:** `getAllStocks()`
- **Endpoint:** `GET /api/stocks/all`
- **Server:** **ALL Servers (8080, 8081, 8082)** - Aggregated

**Flow:**
```
Frontend → Server 1 (8080) → Response
Frontend → Server 2 (8081) → Response
Frontend → Server 3 (8082) → Response
         ↓
    Frontend combines and deduplicates
```

---

### Get Stock Detail (OrderBook)

**Frontend Call:**
```typescript
apiService.getStockDetail(symbol)  // e.g., "AAPL"
```

**Route:**
```
Frontend → Symbol-based routing:
  - If symbol = "AAPL" → http://localhost:8080/api/stocks/AAPL
  - If symbol = "GOOGL" → http://localhost:8080/api/stocks/GOOGL
  - (Routes based on serverRouter.ts configuration)
```

**Backend Controller:**
- **File:** `backend/src/main/java/com/project/matchingengine/controllers/order/StockController.java`
- **Method:** `getStockDetail()`
- **Endpoint:** `GET /api/stocks/{symbol}`
- **Server:** **Symbol-specific server** (determined by `serverRouter.ts`)

**Current Configuration:**
- All symbols currently route to **Server 1 (8080)**
- If you configure multiple servers, symbols will route to their assigned server

---

### Get Stock Trades

**Frontend Call:**
```typescript
apiService.getStockTrades(symbol)  // e.g., "AAPL"
```

**Route:**
```
Frontend → Symbol-based routing:
  - If symbol = "AAPL" → http://localhost:8080/api/stocks/AAPL/trades
  - (Routes based on serverRouter.ts configuration)
```

**Backend Controller:**
- **File:** `backend/src/main/java/com/project/matchingengine/controllers/order/StockController.java`
- **Method:** `getStockTrades()`
- **Endpoint:** `GET /api/stocks/{symbol}/trades`
- **Server:** **Symbol-specific server** (determined by `serverRouter.ts`)

---

## 5. Price APIs

### Get Current Price

**Frontend Call:**
```typescript
apiService.getCurrentPrice(symbol)  // e.g., "AAPL"
```

**Route:**
```
Frontend → Symbol-based routing:
  - If symbol = "AAPL" → http://localhost:8080/api/prices/current/AAPL
  - (Routes based on serverRouter.ts configuration)
```

**Backend Controller:**
- **File:** `backend/src/main/java/com/project/matchingengine/controllers/order/PriceController.java`
- **Method:** `getCurrentPrice()`
- **Endpoint:** `GET /api/prices/current/{symbol}`
- **Server:** **Symbol-specific server** (determined by `serverRouter.ts`)

---

### Get All Prices

**Frontend Call:**
```typescript
apiService.getAllPrices()
```

**Route:**
```
Frontend → http://localhost:8080/api/prices/all
```

**Backend Controller:**
- **File:** `backend/src/main/java/com/project/matchingengine/controllers/order/PriceController.java`
- **Method:** `getAllPrices()`
- **Endpoint:** `GET /api/prices/all`
- **Server:** **Server 1 (8080)** - Default server

---

## 6. Order Management APIs

### Get All Orders

**Frontend Call:**
```typescript
apiService.getAllOrders()
```

**Route:**
```
Frontend → http://localhost:8080/api/orders/all
```

**Backend Controller:**
- **File:** `backend/src/main/java/com/project/matchingengine/controllers/order/OrderController.java`
- **Method:** `getAllOrders()`
- **Endpoint:** `GET /api/orders/all`
- **Server:** **Server 1 (8080)** - Default server

---

### Cancel Order

**Frontend Call:**
```typescript
apiService.cancelOrder(orderId)
```

**Route:**
```
Frontend → http://localhost:8080/api/orders/{orderId}
```

**Backend Controller:**
- **File:** `backend/src/main/java/com/project/matchingengine/controllers/order/OrderController.java`
- **Method:** `cancelOrder()`
- **Endpoint:** `DELETE /api/orders/{orderId}`
- **Server:** **Server 1 (8080)** - Default server

---

## Summary Table

| API Endpoint | Frontend Method | Route | Server | Notes |
|--------------|----------------|-------|--------|-------|
| **Authentication** |
| `POST /api/auth/register` | `register()` | `http://localhost:8080/api/auth/register` | **Server 1 (8080)** | Default server |
| `POST /api/auth/login` | `login()` | `http://localhost:8080/api/auth/login` | **Server 1 (8080)** | Default server |
| **Order Submission** |
| `POST /api/orders/submit` | `submitOrder()` | `http://localhost:8085/api/orders/submit` | **Ingress (8085)** | → Kafka → Matching Servers |
| **User Profile** |
| `GET /api/user/info` | `getUserInfo()` | `http://localhost:8080/api/user/info` | **Server 1 (8080)** | Default server |
| `GET /api/user/profile` | `getUserProfile()` | `http://localhost:8080/api/user/profile` | **Server 1 (8080)** | Default server |
| `GET /api/user/orders` | `getUserOrders()` | `http://localhost:8080/api/user/orders` | **Server 1 (8080)** | Default server |
| `GET /api/user/trades` | `getUserTrades()` | `http://localhost:8080/api/user/trades` | **Server 1 (8080)** | Default server |
| **Stock Data** |
| `GET /api/stocks/all` | `getAllStocks()` | All servers (8080, 8081, 8082) | **ALL Servers** | Aggregated |
| `GET /api/stocks/{symbol}` | `getStockDetail()` | Symbol-based routing | **Symbol-specific** | Based on serverRouter |
| `GET /api/stocks/{symbol}/trades` | `getStockTrades()` | Symbol-based routing | **Symbol-specific** | Based on serverRouter |
| **Prices** |
| `GET /api/prices/current/{symbol}` | `getCurrentPrice()` | Symbol-based routing | **Symbol-specific** | Based on serverRouter |
| `GET /api/prices/all` | `getAllPrices()` | `http://localhost:8080/api/prices/all` | **Server 1 (8080)** | Default server |
| **Order Management** |
| `GET /api/orders/all` | `getAllOrders()` | `http://localhost:8080/api/orders/all` | **Server 1 (8080)** | Default server |
| `DELETE /api/orders/{orderId}` | `cancelOrder()` | `http://localhost:8080/api/orders/{orderId}` | **Server 1 (8080)** | Default server |

---

## Key Routing Rules

### 1. **Default Server (8080)**
- Authentication (login, register)
- User profile and details
- All prices (aggregated)
- All orders (aggregated)
- Order cancellation

### 2. **Ingress Server (8085)**
- **Order submission ONLY** (all orders go here first, then to Kafka)

### 3. **Symbol-Based Routing**
- Stock details (`/api/stocks/{symbol}`)
- Stock trades (`/api/stocks/{symbol}/trades`)
- Current price (`/api/prices/current/{symbol}`)
- Routes based on `serverRouter.ts` configuration

### 4. **Aggregated (All Servers)**
- All stocks (`/api/stocks/all`)
- Queries all servers in parallel and combines results

---

## Visual Flow Diagrams

### Sign Up Flow
```
User → Frontend → Server 1 (8080) → Database → Response
```

### Order Submission Flow
```
User → Frontend → Ingress (8085) → Kafka Topic "orders" → Matching Servers (8080/8081/8082) → Processing
```

### Get User Profile Flow
```
User → Frontend → Server 1 (8080) → Database → Response
```

### Get Stock Detail Flow
```
User → Frontend → Symbol Router → Server X (8080/8081/8082) → OrderBook → Response
```

---

## Important Notes

1. **All user-related operations** go to **Server 1 (8080)** - the default server
2. **All order submissions** go to **Ingress (8085)** first, then to Kafka
3. **Stock data queries** use symbol-based routing to find the correct server
4. **All stocks** endpoint queries all servers and aggregates results
5. **Current setup:** All symbols route to Server 1 (8080) until you configure multiple servers in `serverRouter.ts`
