# Redis Caching Strategy & Implementation Tutorial

**Full tutorial for the Matching Engine.**  
**No code changes are made in this document — it is a step-by-step guide only.**

---

## Table of Contents

1. [Current System Overview](#1-current-system-overview)
2. [Recommended Redis Strategy](#2-recommended-redis-strategy)
3. [Redis Key Design](#3-redis-key-design)
4. [Phase 1: User Balance Cache (Redis)](#4-phase-1-user-balance-cache-redis)
5. [Phase 2: OrderBook State in Redis (Optional)](#5-phase-2-orderbook-state-in-redis-optional)
6. [Redis Setup & Configuration](#6-redis-setup--configuration)
7. [Testing & Verification](#7-testing--verification)
8. [Operational Checklist](#8-operational-checklist)

---

## 1. Current System Overview

### 1.1 What You Have Today

| Component | Location | Role |
|-----------|----------|------|
| **UserDetailsCacheService** | `service/authentication/UserDetailsCacheService.java` | In-memory `ConcurrentHashMap` for user balances/holdings; `@Scheduled` sync to DB every 5s |
| **OrderBook** | `models/order/OrderBook.java` | In-memory buy/sell queues + trades; calls `UserDetailsCacheService.applyTrade` on match |
| **OrderBookConfig** | `config/OrderBookConfig.java` | `Map<String, OrderBook>` per JVM (one per symbol from `Stock` enum) |
| **KafkaConsumer** | `service/kafka/KafkaConsumer.java` | Consumes `orders`, groups by symbol, `orderBook.addOrder(order)` |
| **RedisConfig** | `config/RedisConfig.java` | `RedisTemplate<String, Object>` with Jackson serialization |
| **Redis props** | `application-server1/2/3.properties` | `spring.data.redis.host`, `port`, Lettuce pool |

### 1.2 Problems Addressed by Redis

1. **Balance mismatch across servers**  
   Each server has its own `ConcurrentHashMap`. When multiple servers process trades for the same user, they overwrite each other’s DB updates → inconsistent balances.

2. **OrderBook lost on server death**  
   OrderBooks live only in memory. If a server dies and partitions move, the new consumer has empty books → pending/partially filled orders lost.

Redis is used to:
- **Shared user balance cache** → all servers read/write the same balance state.
- **Optional OrderBook persistence** → recover books after failover (Phase 2).

---

## 2. Recommended Redis Strategy

### 2.1 Two-Tier Caching

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        RECOMMENDED REDIS STRATEGY                        │
├─────────────────────────────────────────────────────────────────────────┤
│  Tier 1: User balance & holdings (shared cache)                          │
│    → All servers read/write Redis                                        │
│    → Atomic updates where possible                                       │
│    → Periodic sync to DB for durability                                  │
├─────────────────────────────────────────────────────────────────────────┤
│  Tier 2: OrderBook state (optional, for failover)                        │
│    → Write-through to Redis on addOrder / match                          │
│    → On startup / partition reassignment: load from Redis                │
└─────────────────────────────────────────────────────────────────────────┘
```

**Priority:** Implement **Tier 1 (user balance)** first. It fixes cross-server balance consistency. **Tier 2 (OrderBook)** is for failover resilience.

### 2.2 Design Principles

- **Single source of truth per user:** Redis holds the canonical balance/holdings during active trading; DB is durability backup.
- **Atomic updates:** Prefer Redis atomic ops (or Lua scripts) over read-modify-write to avoid races.
- **Same keys for all servers:** Every matching-engine instance uses the same key scheme and shared Redis.
- **DB sync:** Keep a background job that pushes Redis → DB (similar to current `updateDatabase`), but **driven by Redis state**, not per-server in-memory maps.

---

## 3. Redis Key Design

### 3.1 User Balance & Holdings

Use a **consistent key namespace** and **hash per user** for balance + metadata:

| Key | Type | Description |
|-----|------|-------------|
| `user:{userId}:balance` | Hash | `ledger`, `available`, `updatedAt` |
| `user:{userId}:holding:{symbol}` | String | Holdings quantity (integer) |

**Why hash for balance?**  
You can use `HGETALL` / `HSET` to read/update `ledger` and `available` together. For atomic balance changes, use a **Lua script** (see below).

**Why separate key per holding?**  
`HINCRBY` is atomic. Use `user:{userId}:holding:{symbol}` with `HINCRBY`-style semantics (or a Lua that updates both balance and holding).

**Alternative (simpler):**  
- `user:{userId}:ledger` (String)  
- `user:{userId}:available` (String)  
- `user:{userId}:h:{symbol}` (String) for quantity  

Store numeric values as strings (e.g. `"1000000.00"`). Atomic updates via Lua.

### 3.2 OrderBook State (Phase 2)

| Key | Type | Description |
|-----|------|-------------|
| `ob:{symbol}:buy` | Sorted Set | Score = `-price` (desc), member = order JSON or orderId |
| `ob:{symbol}:sell` | Sorted Set | Score = `price` (asc), member = order JSON or orderId |
| `ob:{symbol}:lastPrice` | String | Last trade price |
| `ob:{symbol}:orders` | Hash | `orderId → order JSON` for fast lookup |

You already have `Order` (and possibly `Trade`) serialization via Jackson. Reuse that for Redis strings.

### 3.3 Key Conventions

- Prefix: `me:` (matching engine) to avoid clashes, e.g. `me:user:{userId}:balance`.
- Use `userId.toString()` and `symbol` as-is (upper-case) in keys.
- Optional: add `me:dirty:users` (Set) to track which users need DB sync, similar to `dirtyUsers`.

---

## 4. Phase 1: User Balance Cache (Redis)

### 4.1 Goal

- Replace **in-memory** `ConcurrentHashMap` + `dirtyUsers` in `UserDetailsCacheService` with **Redis**.
- All servers use the same Redis keys for each user.
- Keep `getBalance`, `applyTrade`, `placeOrder` semantics; change only where data is stored.
- Keep periodic DB sync, but **from Redis**, not from local maps.

### 4.2 Current Method Signatures (Reference)

Your `UserDetailsCacheService` uses:

- `getBalance(UUID userId)` → returns `CachedUserDetails`
- `applyTrade(UUID userId, String symbol, int quantityDelta, double tradePrice, double initialPrice, boolean isBuy)`
- `placeOrder(UUID userId, String symbol, int quantity, double price, boolean isBuy)`
- `updateDatabase()` → `@Scheduled(fixedRate = 5000)`
- `invalidate(UUID userId)`, `getDirtyUsers()`, `evictStaleEntries()`

`OrderBook` calls `applyTrade` for both buyer and seller when a match occurs (`matchBuyOrder` / `matchSellOrder`).

### 4.3 Redis Data Layout (Phase 1)

Use one hash per user for balance and a separate key per holding:

```
me:user:{userId}:balance   → Hash { ledger, available, updatedAt }
me:user:{userId}:h:{symbol} → String (quantity)
```

**Numbers:** Store as strings. Use Lua for atomic balance updates (see below).

### 4.4 Step-by-Step Implementation

#### Step 1: Add a `StringRedisTemplate` (or equivalent)

You have `RedisTemplate<String, Object>`. For simple strings (and Lua returning strings), a `StringRedisTemplate` is convenient.

**Where:** e.g. `RedisConfig` or a new `RedisCacheConfig`.

**What to do:**  
- Define a `StringRedisTemplate` bean (or `RedisTemplate<String, String>`).
- Use it for balance/holding keys and for Lua script execution.

**No code change in this doc** — only the instruction: add this bean if you implement Redis cache.

#### Step 2: Define Redis Key Constants

**Where:** e.g. `config/RedisKeys.java` or inside `UserDetailsCacheService`.

**Example constants:**

```
PREFIX = "me:"
USER_BALANCE = "user:%s:balance"
USER_HOLDING = "user:%s:h:%s"
DIRTY_USERS = "dirty:users"
```

Use `userId` and `symbol` when formatting.

#### Step 3: Implement “load from DB into Redis”

**Logic:**  
- Same as current `loadUserIntoCache`: on cache miss, load `User` + `Portfolio` from DB.
- Instead of putting into `ConcurrentHashMap`, **write to Redis**:
  - `HSET me:user:{userId}:balance ledger available updatedAt`
  - For each portfolio entry: `SET me:user:{userId}:h:{symbol} quantity`
- Optionally `SADD me:dirty:users {userId}` if you use a dirty set.

**When:**  
- On `getBalance(userId)` when Redis doesn’t have `me:user:{userId}:balance` (or you use a dedicated “exists” check).

#### Step 4: Implement `getBalance` via Redis

**Logic:**  
- Check Redis for `me:user:{userId}:balance` (e.g. `HEXISTS` or `HGETALL`).
- **If present:**  
  - `HGETALL me:user:{userId}:balance` → ledger, available.  
  - For holdings: `KEYS me:user:{userId}:h:*` (or maintain a set of symbols) then `MGET` for each symbol, or use `HGETALL` if you store holdings in a hash.  
  - Build `CachedUserDetails` from Redis data and return (no DB call).
- **If absent:**  
  - Call “load from DB into Redis” (Step 3), then read from Redis again and return.

**Note:**  
`CachedUserDetails` can remain as the in-memory DTO. Only the **storage** moves to Redis; callers still get the same structure.

#### Step 5: Atomic `applyTrade` via Lua

`applyTrade` updates:

- Buyer: available balance, ledger balance, holding `{symbol}`.
- Seller: ledger balance, available balance (and optionally holding; in your model, sell reduces holding).

To avoid races between multiple servers, use **one Lua script per trade side** that does all updates atomically.

**Example buyer script (pseudocode):**

```lua
-- KEYS[1]: me:user:{userId}:balance
-- KEYS[2]: me:user:{userId}:h:{symbol}
-- ARGV[1]: quantityDelta, ARGV[2]: tradePrice, ARGV[3]: initialPrice, ARGV[4]: updatedAt (e.g. epoch ms)

local qty = tonumber(ARGV[1])
local tradePrice = tonumber(ARGV[2])
local initialPrice = tonumber(ARGV[3])

local available = tonumber(redis.call('HGET', KEYS[1], 'available') or 0)
local ledger   = tonumber(redis.call('HGET', KEYS[1], 'ledger') or 0)
local hold     = tonumber(redis.call('GET', KEYS[2]) or 0)

local cashDelta = qty * initialPrice - qty * tradePrice
available = available + cashDelta
ledger    = ledger - qty * initialPrice
hold      = hold + qty

redis.call('HSET', KEYS[1], 'available', available, 'ledger', ledger, 'updatedAt', ARGV[4])
redis.call('SET', KEYS[2], hold)
return 'OK'
```

**Seller script:**  
- Increase `available` and `ledger` by `quantityDelta * tradePrice`.  
- Decrease `me:user:{userId}:h:{symbol}` by `quantityDelta`.

**Implementation:**  
- Store scripts as classpath resources or strings.  
- Use `RedisTemplate.execute(RedisScript, keys, args)` (or `StringRedisTemplate`) to run them.  
- Replace the body of `applyTrade` with:  
  - Ensure user exists in Redis (load from DB if not).  
  - Run buyer script for buyer, seller script for seller.  
  - Add `userId` to dirty set if you use it.

#### Step 6: `placeOrder` (Pre-trade Checks)

**Current behaviour:**  
- `getBalance` → check funds (buy) or holdings (sell) → deduct in memory → mark dirty.

**With Redis:**  
- `getBalance` → from Redis (or load-from-DB-then-Redis).  
- Check `available >= price * quantity` (buy) or `holding(symbol) >= quantity` (sell).  
- **Deduct in Redis**, not in memory:
  - Buy: decrease `available` (e.g. via Lua or `HINCRBY`-like logic).
  - Sell: decrease `user:{userId}:h:{symbol}`.

Use a Lua script if you want “check and deduct” to be atomic. Otherwise, you risk over-spend if two orders are validated concurrently.

#### Step 7: `updateDatabase` from Redis

**Current behaviour:**  
- `@Scheduled(fixedRate = 5000)` iterates `dirtyUsers`, reads from in-memory cache, writes to `User` and `Portfolio`.

**With Redis:**  
- **Option A (dirty set):** Iterate `SMEMBERS me:dirty:users`. For each `userId`, read balance and holdings from Redis, write to DB, then `SREM me:dirty:users {userId}`.
- **Option B (scan):** Periodically scan `me:user:*:balance` and sync all to DB (simpler but less targeted).

**Logic per user:**  
- `HGETALL me:user:{userId}:balance` → set `User.ledgerBalance`, `User.availableBalance`.  
- For holdings: get all `me:user:{userId}:h:*`, then update `Portfolio` rows (insert/update/delete) to match Redis.  
- Use a **single DB transaction** per user (or per batch) to keep consistency.

**Critical:**  
- Only **one** process should run this sync job (e.g. one designated instance, or a distributed lock via Redis).  
- If multiple servers run it, you can end up with duplicate or conflicting writes. Use a Redis lock (e.g. `SET me:lock:db-sync NX EX 10`) around the sync.

#### Step 8: Eviction and Invalidation

**evictStaleEntries:**  
- Currently evicts from `ConcurrentHashMap` by `lastAccessTime`.  
- With Redis, you can:
  - **Option 1:** Don’t evict; Redis has its own memory limits. Use `MAXMEMORY` and eviction policy.  
  - **Option 2:** Store `updatedAt` in `me:user:{userId}:balance` and periodically `SCAN` + delete old keys (e.g. no access for 30 minutes).

**invalidate(userId):**  
- `DEL me:user:{userId}:balance` and `DEL` each `me:user:{userId}:h:{symbol}` (or use a pattern delete).  
- `SREM me:dirty:users {userId}` if applicable.

### 4.5 Optional: Store Balances in Cents

To use `INCRBY` / `DECRBY`, store balances in **smallest unit** (e.g. cents):

- `available` = 100000000 → $1,000,000.00  
- Use `HINCRBY` for balance deltas.

You’d need to:
- Convert `double` ↔ `long` in your app.  
- Change Redis key design to numeric values.

This is a **variant** of the same strategy; the Lua approach above works with decimal strings too.

---

## 5. Phase 2: OrderBook State in Redis (Optional)

### 5.1 Goal

- Persist OrderBook state (buy/sell queues, last price) so that when a server dies and partitions are reassigned, the new consumer can **reload** OrderBooks from Redis instead of starting empty.

### 5.2 Write-Through on `addOrder`

**Where:** `OrderBook.addOrder` (and match logic).

**Flow:**  
1. Update in-memory buy/sell queues and run matching as today.  
2. After each `addOrder` (and after any match):
   - **Persist to Redis:**
     - `ob:{symbol}:buy`: sorted set of active buy orders (score = `-price` or similar).  
     - `ob:{symbol}:sell`: sorted set of sell orders (score = `price`).  
     - `ob:{symbol}:orders`: hash of `orderId → order JSON`.  
     - `ob:{symbol}:lastPrice`: last trade price.

**Serialization:**  
- Reuse Jackson to serialize `Order` to JSON. Store as string in Redis.

**Complexity:**  
- OrderBook uses `PriorityQueue` and in-place updates (e.g. `setCurrentQuantity`).  
- You must **reflect** those updates in Redis: when an order is matched or removed, update/remove it from the sets and hash.  
- This implies touching `matchBuyOrder` / `matchSellOrder` and any logic that mutates orders.

### 5.3 Loading OrderBook from Redis on Startup

**When:**  
- On matching-engine startup, **or** when a partition is assigned that includes symbols this instance did not previously serve.

**Logic:**  
1. For each symbol you care about (e.g. from `Stock` enum, or from partition-to-symbol mapping):
   - `ZRANGE ob:{symbol}:buy 0 -1 WITHSCORES` and `ZRANGE ob:{symbol}:sell 0 -1 WITHSCORES`.
   - `HGETALL ob:{symbol}:orders` → order JSONs.
2. Rebuild `PriorityQueue<Order>` (buy and sell) from Redis data, then create `OrderBook(symbol, ...)` and put it in `OrderBookConfig`’s map.

**Order of processing:**  
- When consuming from Kafka, you must apply orders **in the same order** as the log. Redis holds a snapshot; Kafka holds the sequence. So: **Kafka remains the source of order sequence**; Redis is a **recovery store** for book state.

### 5.4 Scope of Changes

- `OrderBook`: add hooks to write through to Redis (or delegate to a small `OrderBookRedisRepository`).  
- `OrderBookConfig` / startup: optionally load existing books from Redis before starting Kafka consumer.  
- Ensure only one writer per symbol (same as today: one partition per symbol, one consumer per partition).

Phase 2 is **optional** and more invasive. Phase 1 alone already fixes balance consistency.

---

## 6. Redis Setup & Configuration

### 6.1 Run Redis Locally

```bash
# macOS (Homebrew)
brew install redis
brew services start redis

# Or run in foreground
redis-server
```

Default: `localhost:6379`.

### 6.2 Application Properties

You already have:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=2000ms
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0
spring.data.redis.lettuce.pool.max-wait=-1ms
```

Use the same Redis for **all** matching-engine instances (server1, server2, server3). Ingress does not use the balance cache.

### 6.3 RedisConfig

Existing `RedisConfig` creates `RedisTemplate<String, Object>`. For Phase 1:

- Add a `StringRedisTemplate` (or `RedisTemplate<String, String>`) for balance/holding keys and Lua.  
- Keep the existing template if you still store objects (e.g. OrderBook Phase 2).

### 6.4 Connection Testing

- Use `RedisConnectionFactory` or `RedisTemplate.getConnectionFactory().getConnection().ping()`.  
- Add a simple health check or startup check that pings Redis; fail fast if Redis is down.

---

## 7. Testing & Verification

### 7.1 Unit Tests

- **UserDetailsCacheService:**  
  - Use `@EmbeddedRedis` or Testcontainers Redis.  
  - Test `getBalance` (cache miss → load from DB → Redis populated).  
  - Test `applyTrade` (buy/sell) and then `getBalance` → balances and holdings match expected.  
  - Test two “servers” (two service instances) sharing same Redis: both `applyTrade` for same user → single consistent balance.

### 7.2 Integration Tests

- Start Redis + Kafka + 2+ matching engines + ingress.  
- Submit orders for the **same user** on different symbols (so different servers process them).  
- Query user balance (from any server or API) and compare to DB after sync.  
- Assert no “last write wins” overwrites and no negative balances.

### 7.3 Redis CLI Checks

```bash
redis-cli

# After applyTrade for user U and symbol S
HGETALL me:user:<U>:balance
GET me:user:<U>:h:S

# Optional dirty set
SMEMBERS me:dirty:users
```

---

## 8. Operational Checklist

- [ ] Redis running and reachable from all matching-engine hosts.  
- [ ] Same `spring.data.redis.*` config for server1/2/3.  
- [ ] All balance/holding keys use shared prefix and schema (e.g. `me:user:*`).  
- [ ] `applyTrade` (and optionally `placeOrder`) use atomic Lua scripts.  
- [ ] DB sync job runs only once (single instance or Redis lock).  
- [ ] Eviction/invalidation strategy defined (Redis or application-level).  
- [ ] Health check includes Redis ping.  
- [ ] Monitoring for Redis memory, latency, and key count.

---

## Implementation Quick Reference

| Current operation | Redis equivalent |
|-------------------|------------------|
| `cache.get(userId)` | `HGETALL me:user:{userId}:balance` + `GET me:user:{userId}:h:{symbol}` for each holding |
| `loadUserIntoCache` | `HSET` balance hash, `SET` each holding key; optionally `SADD me:dirty:users {userId}` |
| `applyTrade` (buy/sell) | Run Lua script: update balance hash + holding key; `SADD me:dirty:users {userId}` |
| `placeOrder` check + deduct | Lua: check `available` / holding, then deduct; `SADD me:dirty:users {userId}` |
| `updateDatabase` loop over `dirtyUsers` | `SMEMBERS me:dirty:users` → for each user, `HGETALL` + holdings → write DB → `SREM` |
| `invalidate(userId)` | `DEL me:user:{userId}:balance`, `DEL` each `me:user:{userId}:h:*`, `SREM me:dirty:users {userId}` |
| `evictStaleEntries` | `SCAN` keys, check `updatedAt` in hash; `DEL` if stale; or rely on Redis eviction |

---

## Summary

| Phase | What | Purpose |
|-------|------|---------|
| **1** | Redis user balance cache | Single shared view of balances/holdings; fix cross-server mismatch and DB overwrites. |
| **2** | Redis OrderBook state | Survive server failures and partition reassignment without losing book state. |

**Best Redis strategy for your system:**  
- **Tier 1:** Redis as shared user balance cache, atomic updates via Lua, periodic DB sync from Redis.  
- **Tier 2 (optional):** Write-through of OrderBook to Redis and reload on startup/reassignment.

**Implementation order:**  
1. Redis key design and constants.  
2. Load-from-DB into Redis + `getBalance` from Redis.  
3. Lua scripts for `applyTrade` (and `placeOrder` if needed).  
4. `updateDatabase` from Redis with single-writer sync.  
5. Eviction/invalidation.  
6. Tests and rollout.  
7. Optionally Phase 2 OrderBook persistence.

This tutorial does not modify your codebase; it only describes the strategy and implementation steps you can follow when you are ready to add Redis caching.
