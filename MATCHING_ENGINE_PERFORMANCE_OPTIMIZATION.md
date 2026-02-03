# Matching Engine Performance Optimization Guide

This document identifies **performance optimization opportunities** in the current matching engine codebase, aligned with **industry best practices** for low-latency order matching, Kafka-based ingestion, and Redis-backed user state.

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Ingestion Path: Ingress → Kafka](#2-ingestion-path-ingress--kafka)
3. [Consumption Path: Kafka → OrderBook](#3-consumption-path-kafka--orderbook)
4. [OrderBook & Matching Core](#4-orderbook--matching-core)
5. [Redis & UserDetailsCacheService](#5-redis--userdetailscacheservice)
6. [Persistence & Scheduled Jobs](#6-persistence--scheduled-jobs)
7. [Read APIs & Caching](#7-read-apis--caching)
8. [WebSocket & Broadcast](#8-websocket--broadcast)
9. [Summary: Priority Matrix](#9-summary-priority-matrix)

---

## 1. Executive Summary

| Area | Main issue | Industry-standard approach | Impact |
|------|------------|----------------------------|--------|
| **Kafka producer** | No batching/compression; sync send | Batch + linger + compression; fire-and-forget with callback | High (ingestion latency & throughput) |
| **Redis** | `KEYS` pattern scan; N round-trips per user | `SCAN` + pipeline / `HGETALL` + MGET where possible | High (cache latency under load) |
| **OrderBook** | Full queue copy for top-5; Redis call per trade | Iterator for top-N; batch Redis updates per order | Medium (matching hot path) |
| **DB / scheduled** | All order books processed every 5s; no dirty tracking | Dirty symbols only; batch size + connection pool | Medium (CPU & DB load) |
| **Read APIs** | Trades by symbol from DB | Serve recent trades from OrderBook or short TTL cache | Medium (API latency) |
| **WebSocket** | Broadcast object; possible serialization per subscriber | Serialize once; consider topic-per-symbol backpressure | Low–Medium |

The rest of the document explains **what** in the current code causes each issue and **how** to optimize it.

---

## 2. Ingestion Path: Ingress → Kafka

### Current Behavior

- **IngressController** builds an `Order`, calls **OrderService.submitOrder(order)** → **KafkaProducer.sendOrder(order)**.
- **KafkaProducer.sendOrder**: `objectMapper.writeValueAsString(order)` then `kafkaTemplate.send(topic, symbol, orderJson)`.
- **KafkaProducerConfig**: No `linger.ms`, no `batch.size`, no compression. Each `send()` typically results in one request to the broker.

### Why It Matters

- **Latency**: Synchronous-looking send (unless you use callbacks and don’t block) and no batching mean more round-trips to Kafka.
- **Throughput**: Small batches or single-message sends underutilize the network and increase broker load.
- **Industry standard**: Producers use batching + compression + async send with callbacks; ingestion API returns quickly after handing off to Kafka.

### Optimizations

| Optimization | What to do | Where |
|--------------|------------|--------|
| **Producer batching** | Set `linger.ms` (e.g. 5–20 ms) and `batch.size` (e.g. 16–32 KB) so multiple orders are sent in one batch. | `KafkaProducerConfig`: add `ProducerConfig.LINGER_MS_CONFIG`, `ProducerConfig.BATCH_SIZE_CONFIG`. |
| **Compression** | Use `compression.type=lz4` (or `zstd`) to reduce payload size and network I/O. | Same config. |
| **Fire-and-forget with callback** | Don’t block the request thread on `send()`. Use `send().whenComplete()` or callback; return 202 Accepted or 200 after enqueue. | `KafkaProducer.sendOrder`: use `kafkaTemplate.send(...).addCallback(...)`; Ingress/OrderService returns without waiting for broker ack if acceptable. |
| **Pre-serialization pool** | Reuse buffers or a small pool for JSON serialization to reduce allocations on hot path. | Optional: reuse `ObjectMapper` (already shared); for very high throughput, consider a pool of serialization buffers. |

**Code-level note**: Today `send()` is fire-and-forget from a blocking perspective, but adding an explicit callback and tuning batching/compression will improve throughput and latency under load.

---

## 3. Consumption Path: Kafka → OrderBook

### Current Behavior

- **KafkaConsumerConfig**: `MAX_POLL_RECORDS_CONFIG = 500`, `FETCH_MIN_BYTES = 1MB`, `FETCH_MAX_WAIT_MS = 500`, batch listener enabled. Good base.
- **KafkaConsumer.processOrders**: Deserializes each JSON string in a loop; runs **orderRepo.saveAll(orders)** asynchronously via `CompletableFuture.runAsync(..., dbExecutor)`; groups orders by symbol; for each symbol calls **orderBook.addOrder(order)** in a loop, then **broadcastOrderBookUpdate** once per symbol.

### Why It Matters

- Deserialization in a loop is fine but can be a cost at very high message rates.
- Matching is done **one order at a time** per symbol; Redis (applyTrade) is called **per trade**, not batched.

### Optimizations

| Optimization | What to do | Where |
|--------------|------------|--------|
| **Batch matching (same symbol)** | For a given symbol, you already have a list of orders. Optionally pass the batch into OrderBook and run matching in a single lock hold, or at least avoid repeated lock acquire/release per order if you later refactor. | `KafkaConsumer`: already groups by symbol; `OrderBook` could add `addOrders(List<Order>)` that holds writeLock once and processes all orders, then one summary update. |
| **Reduce Redis round-trips per batch** | Today each match calls `userDetailsCacheService.applyTrade` twice (buyer + seller). For multiple matches in one order, that’s 2N Redis Lua calls. Use a **pipeline** or a **single Lua script** that applies multiple trade updates (e.g. for one order’s matches). | `OrderBook`: collect (userId, symbol, qty, price, side) per match; call a new `UserDetailsCacheService.applyTradesBatch(updates)` that uses Redis pipeline or one Lua script. |
| **Avoid blocking consumer thread on DB** | You already offload `orderRepo.saveAll` to `dbExecutor`. Ensure other heavy work (e.g. broadcast) doesn’t block the consumer thread unnecessarily. | Keep WebSocket broadcast quick (serialize once, send); if it grows, consider delegating to another thread. |

---

## 4. OrderBook & Matching Core

### Current Behavior

- **OrderBook.addOrder**: Takes writeLock; matches one order (matchBuyOrder / matchSellOrder); each match does 2× `userDetailsCacheService.applyTrade` and mutates queues; updates `currentPrice` from `trades` at the end.
- **OrderBook.updateOrderBookSummary**: Takes readLock; builds top-5 buys/sells by **copying the entire PriorityQueue** twice: `new PriorityQueue<>(buyOrdersList)` and `new PriorityQueue<>(sellOrdersList)`, then polls 5 from each. Also calls `getMostRecent10Trades()` (another readLock + copy).

### Why It Matters

- **PriorityQueue copy**: O(n) in queue size. For deep books, this is expensive and runs on every summary update (scheduled job + after each batch in consumer). Industry practice: use an **iterator** or **peek + limited iteration** to get top 5 without copying the whole queue.
- **Lock hold time**: Long matching loops with Redis calls inside writeLock increase contention with readers (HTTP, scheduled job). Batching Redis updates (see above) shortens lock hold time.
- **currentPrice**: Recomputing from `trades.get(trades.size()-1)` at the end of addOrder is fine; avoid doing it again unnecessarily in read paths.

### Optimizations

| Optimization | What to do | Where |
|--------------|------------|--------|
| **Top-N without full copy** | Don’t copy entire buy/sell queues. Use an iterator over the queue, or a temporary list filled by polling up to 5 (and re-inserting if you must preserve the queue). Better: many implementations keep a separate “depth view” or iterate the heap structure up to N elements. | `OrderBook.updateOrderBookSummary`: replace `new PriorityQueue<>(buyOrdersList)` with a loop that peeks/polls up to 5 and stores in a list (and re-add to a temp queue then drain back if you need to preserve order—or use a data structure that supports “first N” without full copy). |
| **Batch Redis updates** | See [Consumption path](#3-consumption-path-kafka--orderbook): collect all balance/holding updates for the current order’s matches and apply in one pipeline or batched Lua call. | `OrderBook` match methods + `UserDetailsCacheService`. |
| **Reduce getMostRecent10Trades copies** | You already return a copy for safety. If updateOrderBookSummary is the main caller, ensure you’re not creating unnecessary intermediate lists. Minor. | `OrderBook`: keep single subList copy. |

---

## 5. Redis & UserDetailsCacheService

### Current Behavior

- **getBalance**: `opsForHash().entries(balanceKey)`; on cache miss, **loadUserIntoRedis** (DB read + multiple Redis writes).
- **loadHoldingsFromRedis**: `stringRedisTemplate.keys("me:user:" + userId + ":h:*")` then one `get(key)` per holding key.
- **getAllCachedBalances** / **evictStaleEntries** / **getDirtyUsers** / **getCacheSize**: Use `stringRedisTemplate.keys("me:user:*:balance")` or similar.
- **applyTrade** / **placeOrder**: Use Lua scripts (good—atomic). No pipelining across multiple users/trades.

### Why It Matters

- **KEYS pattern**: In Redis, `KEYS` is O(N) over the keyspace and blocks the event loop. In production, **SCAN** is the standard (cursor-based, non-blocking).
- **N+1 in loadHoldingsFromRedis**: One `KEYS` + one `GET` per holding. For users with many symbols, use **MGET** or **pipeline** for the GETs; use **SCAN** instead of KEYS.
- **Repeated getBalance in updateDatabase**: For each dirty user you call `getBalance(userId)` (Redis read + possibly loadHoldingsFromRedis). Pipeline multiple users’ balance + holdings reads where possible.

### Optimizations

| Optimization | What to do | Where |
|--------------|------------|--------|
| **Replace KEYS with SCAN** | Use `stringRedisTemplate.scan(ScanOptions.scanOptions().match(pattern).count(100).build())` (or equivalent) to iterate balance keys or holding keys. | `UserDetailsCacheService`: getAllCachedBalances, evictStaleEntries, getCacheSize, loadHoldingsFromRedis, invalidate. |
| **Batch holdings read** | For a known set of holding keys (e.g. from Stock enum or from a per-user key list), use **MGET** or **pipeline** instead of one GET per key. | loadHoldingsFromRedis: after SCAN, collect keys and use MGET or pipeline. |
| **Pipeline in updateDatabase** | When reading many dirty users’ balances/holdings, use Redis pipeline to send multiple HGETALL/MGET in one round-trip. | updateDatabase: build list of keys for dirty users, execute pipeline, then map results to CachedUserDetails. |
| **Optional: connection pooling** | Ensure Redis connection pool size is tuned (e.g. Lettuce pool) for concurrent threads. | application.properties / Redis connection factory. |

---

## 6. Persistence & Scheduled Jobs

### Current Behavior

- **OrderBookService.updateTradeTable** (every 5s): Iterates **all** OrderBooks; for each: getTrades snapshot → tradeRepo.saveAll; getAllOrdersToUpdate → collect; updateOrderBookSummary; clearTradeRecords. Then dedupe orders by orderId and orderRepo.saveAll.
- **UserDetailsCacheService.updateDatabase** (every 5s): Takes distributed lock; getDirtyUsers; for each dirty user getBalance (Redis); updates User and Portfolio in DB; clearDirtyUsers.

### Why It Matters

- **All symbols every 5s**: Even inactive symbols get trades/orders snapshotted and summaries updated. Industry approach: only process **dirty** or **recently active** symbols (e.g. set a “dirty” flag or last-modified timestamp per symbol when addOrder is called).
- **Large batches**: saveAll of many orders/trades in one go can cause long transactions and memory spikes. Consider batching (e.g. 500 per batch) and/or chunked saves.
- **DB connection pool**: Ensure pool size and timeouts match the number of threads and batch size to avoid waits.

### Optimizations

| Optimization | What to do | Where |
|--------------|------------|--------|
| **Dirty-symbol tracking** | When OrderBook receives at least one order, mark symbol as dirty (e.g. Set<String> or bit set in OrderBookConfig). In updateTradeTable, only iterate dirty symbols; clear dirty after process. | OrderBookConfig + OrderBook (or KafkaConsumer) + OrderBookService.updateTradeTable. |
| **Chunked DB writes** | Split large lists into chunks (e.g. 200–500) and call saveAll per chunk to avoid huge transactions. | OrderBookService.updateTradeTable: chunk allOrdersToUpdate and tradesSnapshot; UserDetailsCacheService.updateDatabase: chunk usersToUpdate and portfoliosToUpdate. |
| **Tune scheduler** | If 5s is too aggressive, consider 10s or make it configurable; balance freshness vs DB/CPU load. | @Scheduled fixedRate; make configurable via properties. |

---

## 7. Read APIs & Caching

### Current Behavior

- **StockController.getAllStocks**: For each stock, gets OrderBook and getCurrentPrice() (readLock + last trade). No DB.
- **StockController.getStockDetail** / **getOrderBookSummary**: OrderBook from config, getOrderBookSummary(). In-memory. Good.
- **StockController.getStockTrades** (`GET /{symbol}/trades`): **TradeService.getTradesBySymbol(symbol)** → **tradeRepo.findTradesBySymbol(symbol)** → DB.

### Why It Matters

- **Trades by symbol**: Hitting the DB every time is slower and adds load. Recent trades already exist in OrderBook (getMostRecent10Trades or the trades list before clearTradeRecords). For “last N trades” API, serving from memory (or a short TTL cache) is standard.

### Optimizations

| Optimization | What to do | Where |
|--------------|------------|--------|
| **Serve recent trades from OrderBook** | For “recent trades” or “last 10” semantics, return OrderBook.getMostRecent10Trades() (or a dedicated method) so the API doesn’t hit DB. Optionally keep a “trades history” cache (e.g. Caffeine) keyed by symbol with TTL. | StockController.getStockTrades: get OrderBook for symbol, call getMostRecent10Trades() (or similar), return; or add TradeService.getRecentTradesFromOrderBook(symbol) that uses OrderBookConfig. |
| **Limit DB fallback** | If you need longer history, use DB only for “older” or paginated requests; keep “latest” from memory. | TradeService / controller: branch on query params (e.g. limit=10 → memory; else DB). |

---

## 8. WebSocket & Broadcast

### Current Behavior

- **WebSocketNotificationService.broadcastOrderBookUpdate**: Receives OrderBookSummary; `messagingTemplate.convertAndSend(destination, summary)`. STOMP will serialize the object (likely once per destination).

### Why It Matters

- Serializing a large summary for many subscribers can add up. Serialize once and send the same payload if the broker supports it.
- Per-symbol destination is good; avoid broadcasting full book to “all symbols” if only one symbol changed.

### Optimizations

| Optimization | What to do | Where |
|--------------|------------|--------|
| **Serialize once** | If convertAndSend serializes per subscriber, pre-serialize to String/byte[] and send that (if your broker API allows). Otherwise ensure ObjectMapper is reused (already is). | WebSocketNotificationService. |
| **Backpressure / throttling** | If a symbol updates very frequently, consider throttling broadcasts (e.g. max once per 50–100 ms per symbol) to avoid overwhelming clients. | Optional: throttle in KafkaConsumer before calling broadcastOrderBookUpdate. |

---

## 9. Summary: Priority Matrix

| Priority | Area | Optimization | Effort | Impact |
|----------|------|--------------|--------|--------|
| **High** | Kafka producer | Batching + compression + async callback | Low | Throughput & latency |
| **High** | Redis | Replace KEYS with SCAN; pipeline/MGET for holdings | Medium | Cache latency & Redis load |
| **Medium** | OrderBook | Top-5 without full queue copy; batch Redis applyTrade | Medium | Matching & lock contention |
| **Medium** | Scheduled jobs | Dirty-symbol only; chunked DB writes | Medium | CPU & DB load |
| **Medium** | Read API | Recent trades from OrderBook (or cache) | Low | API latency |
| **Low** | Consumer | Optional addOrders batch + single lock | Low–Medium | Lock granularity |
| **Low** | WebSocket | Serialize once; optional throttle | Low | Broadcast cost |

Implementing **Kafka producer batching/compression** and **Redis KEYS → SCAN + pipeline/MGET** will give the largest gain with moderate effort. Then **OrderBook top-N and dirty-symbol persistence** will further reduce CPU and DB load. The rest can be phased in as load grows.

---

## References (Industry Practice)

- **Kafka**: Batching, compression, idempotent producer (you have this), async send with callbacks.
- **Redis**: Avoid KEYS; use SCAN; pipeline for multiple commands; Lua for atomic read-modify-write (you have this).
- **Matching engines**: Minimize lock hold time; batch external calls (DB/Redis); dirty-set or event-driven persistence.
- **APIs**: Serve hot data from memory/cache; use DB for history or pagination.
