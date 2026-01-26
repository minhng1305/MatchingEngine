# Partition-to-Symbol Mapping and Thread Assignment - Clarification

## Your Questions

1. **Can partition 1 and partition 2 share the same symbol (e.g., AAPL)?**
2. **Do threads processing different partitions share the same OrderBook?**
3. **How are partitions assigned to threads in a server with 4 threads?**

---

## Answer 1: Can Different Partitions Contain the Same Symbol?

### ❌ **NO - Different partitions CANNOT contain the same symbol**

**Kafka's Partitioning Guarantee:**

```java
// In KafkaProducer.java
kafkaTemplate.send(ordersTopic, symbol, orderJson);
//                      ↑         ↑
//                   topic      key (symbol)
```

**Kafka's Formula:**
```
partition = hash(symbol) % number_of_partitions
```

**Key Property:** Same key (symbol) **always** hashes to the same partition.

**Example:**
- `hash("AAPL") % 12 = 9` → **All AAPL orders go to Partition 9**
- `hash("GOOGL") % 12 = 6` → **All GOOGL orders go to Partition 6**
- `hash("MSFT") % 12 = 11` → **All MSFT orders go to Partition 11**

**Result:**
- ✅ **Partition 9** contains **only AAPL orders** (all of them)
- ✅ **Partition 6** contains **only GOOGL orders** (all of them)
- ❌ **Partition 1 and Partition 2 CANNOT both have AAPL** (AAPL always goes to partition 9)

**Important:** This is a **Kafka guarantee** - same key = same partition.

---

## Answer 2: Do Threads Share OrderBooks?

### ✅ **YES - But only if they process the SAME symbol**

**However, since same symbol = same partition, and one partition = one thread:**

**The Reality:**
- **One partition** is consumed by **exactly one thread** (within a consumer group)
- **Same symbol** always goes to **same partition**
- **Therefore:** Only **one thread** processes orders for a given symbol

**Example:**
```
Partition 9 → Contains only AAPL orders
  ↓
Assigned to Server 1, Thread 1
  ↓
Thread 1 processes all AAPL orders
  ↓
Thread 1 uses OrderBook("AAPL")
```

**What About Multiple Threads?**
- Thread 1 processes Partition 0 (AAPL orders) → Uses OrderBook("AAPL")
- Thread 2 processes Partition 1 (GOOGL orders) → Uses OrderBook("GOOGL")
- Thread 3 processes Partition 2 (MSFT orders) → Uses OrderBook("MSFT")
- Thread 4 processes Partition 3 (NVDA orders) → Uses OrderBook("NVDA")

**Result:**
- ✅ **Different threads** → **Different symbols** → **Different OrderBooks** (no sharing)
- ❌ **Threads do NOT share OrderBooks** (because they process different symbols)

**When Would Threads Share OrderBooks?**
- Only if **same symbol** appears in **multiple partitions** → **But this is IMPOSSIBLE** (Kafka guarantee)
- Or if **server dies** and partitions are **reassigned** → Then different thread processes same symbol

---

## Answer 3: How Are Partitions Assigned to Threads?

### Current Setup

**Configuration:**
- **12 partitions** total
- **3 servers** (Server 1, 2, 3)
- **4 threads per server** (concurrency = 4)

**Kafka's Assignment:**

```
Server 1 (4 threads):
  Thread 1 → Partition 0
  Thread 2 → Partition 1
  Thread 3 → Partition 2
  Thread 4 → Partition 3

Server 2 (4 threads):
  Thread 1 → Partition 4
  Thread 2 → Partition 5
  Thread 3 → Partition 6
  Thread 4 → Partition 7

Server 3 (4 threads):
  Thread 1 → Partition 8
  Thread 2 → Partition 9
  Thread 3 → Partition 10
  Thread 4 → Partition 11
```

**How Spring Kafka Does This:**

```java
// KafkaConsumerConfig.java
factory.setConcurrency(4);  // Creates 4 listener threads
```

**Spring Kafka's Behavior:**
- Creates **4 `@KafkaListener` instances** (4 threads)
- Each thread gets assigned **specific partitions** by Kafka's Group Coordinator
- Assignment is **automatic** and **balanced**

**Important:** 
- **One partition** = **One thread** (within a consumer group)
- **One thread** can process **multiple partitions** (if you have more partitions than threads)
- But in your case: **4 threads, 4 partitions per server** → **1:1 mapping**

---

## Detailed Example: Symbol Distribution

### Scenario: 20 Symbols, 12 Partitions

**Symbol → Partition Mapping (hash-based):**

```
AAPL  → hash("AAPL") % 12 = 9  → Partition 9
GOOGL → hash("GOOGL") % 12 = 6  → Partition 6
MSFT  → hash("MSFT") % 12 = 11  → Partition 11
AMZN  → hash("AMZN") % 12 = 3   → Partition 3
TSLA  → hash("TSLA") % 12 = 1   → Partition 1
META  → hash("META") % 12 = 7   → Partition 7
NFLX  → hash("NFLX") % 12 = 4   → Partition 4
NVDA  → hash("NVDA") % 12 = 8   → Partition 8
AMD   → hash("AMD") % 12 = 2    → Partition 2
INTC  → hash("INTC") % 12 = 10  → Partition 10
... (more symbols hash to various partitions)
```

**Partition Contents:**

```
Partition 0: [Symbol1, Symbol2, ...]  (multiple symbols can hash to same partition)
Partition 1: [TSLA, SymbolX, ...]
Partition 2: [AMD, SymbolY, ...]
Partition 3: [AMZN, SymbolZ, ...]
Partition 4: [NFLX, ...]
Partition 5: [...]
Partition 6: [GOOGL, ...]
Partition 7: [META, ...]
Partition 8: [NVDA, ...]
Partition 9: [AAPL, ...]  ← All AAPL orders here
Partition 10: [INTC, ...]
Partition 11: [MSFT, ...]
```

**Key Point:** 
- ✅ **Multiple symbols** can hash to the **same partition**
- ❌ **Same symbol** cannot hash to **different partitions**

---

## Thread Processing Flow

### Server 1 Example (Partitions 0-3)

**Thread Assignment:**
```
Thread 1 → Partition 0
Thread 2 → Partition 1
Thread 3 → Partition 2
Thread 4 → Partition 3
```

**What Each Thread Processes:**

**Thread 1 (Partition 0):**
```
Receives batch: [Order1-SymbolX, Order2-SymbolY, Order3-SymbolX, ...]
  ↓
Groups by symbol: {SymbolX: [Order1, Order3], SymbolY: [Order2]}
  ↓
For SymbolX: Uses OrderBook("SymbolX")
For SymbolY: Uses OrderBook("SymbolY")
```

**Thread 2 (Partition 1):**
```
Receives batch: [Order1-TSLA, Order2-TSLA, Order3-SymbolZ, ...]
  ↓
Groups by symbol: {TSLA: [Order1, Order2], SymbolZ: [Order3]}
  ↓
For TSLA: Uses OrderBook("TSLA")
For SymbolZ: Uses OrderBook("SymbolZ")
```

**Thread 3 (Partition 2):**
```
Receives batch: [Order1-AMD, Order2-AMD, ...]
  ↓
Groups by symbol: {AMD: [Order1, Order2]}
  ↓
For AMD: Uses OrderBook("AMD")
```

**Thread 4 (Partition 3):**
```
Receives batch: [Order1-AMZN, Order2-SymbolW, ...]
  ↓
Groups by symbol: {AMZN: [Order1], SymbolW: [Order2]}
  ↓
For AMZN: Uses OrderBook("AMZN")
For SymbolW: Uses OrderBook("SymbolW")
```

---

## Can Threads Share OrderBooks?

### Answer: **NO (in normal operation)**

**Why:**
1. **Same symbol** → **Same partition** (Kafka guarantee)
2. **Same partition** → **One thread** (Kafka assignment)
3. **Therefore:** **One symbol** → **One thread** → **One OrderBook**

**Example:**
- **AAPL** always goes to **Partition 9**
- **Partition 9** is assigned to **Server 3, Thread 2**
- **Only Thread 2** processes AAPL orders
- **Only Thread 2** uses OrderBook("AAPL")
- **Other threads** never see AAPL orders → **Never use OrderBook("AAPL")**

**When Would Threads Share OrderBooks?**

**Scenario: Server Death and Reassignment**

```
Before:
  Server 1, Thread 1 → Partition 0 (AAPL orders)
  Server 1, Thread 2 → Partition 1 (GOOGL orders)

Server 1 dies:
  ↓
Kafka reassigns:
  Server 2, Thread 1 → Partition 0 (AAPL orders)  ← Now processes AAPL
  Server 2, Thread 2 → Partition 1 (GOOGL orders)
  Server 2, Thread 3 → Partition 2 (MSFT orders)  ← Also gets Server 1's old partitions
  Server 2, Thread 4 → Partition 3 (NVDA orders)
```

**After Reassignment:**
- **Server 2, Thread 1** now processes AAPL → Uses OrderBook("AAPL")
- **Server 2's OrderBook("AAPL")** is **empty** (new server, new OrderBook)
- **Previous AAPL orders** in Server 1's OrderBook → **LOST** ❌

**This is the problem you identified!**

---

## Corrected Understanding

### Your Original Statement (Incorrect)
> "Thread 1 processes: AAPL orders from partition 0  
> Thread 3 processes: AAPL orders from partition 2"

**This is WRONG** because:
- AAPL always goes to the same partition (e.g., partition 9)
- Partition 9 is assigned to one thread only
- Thread 3 cannot process AAPL from partition 2 (AAPL is not in partition 2)

### Correct Understanding

**Reality:**
- **Thread 1** processes **Partition 0** → Contains **SymbolX, SymbolY** (not AAPL)
- **Thread 2** processes **Partition 1** → Contains **TSLA, SymbolZ** (not AAPL)
- **Thread 3** processes **Partition 2** → Contains **AMD, SymbolW** (not AAPL)
- **Thread 4** processes **Partition 3** → Contains **AMZN, SymbolV** (not AAPL)

**AAPL orders:**
- All go to **Partition 9** (hash-based)
- **Partition 9** assigned to **Server 3, Thread 2**
- **Only Server 3, Thread 2** processes AAPL
- **Only Server 3, Thread 2** uses OrderBook("AAPL")

---

## Server Death: 6 Partitions per Server, 4 Threads — How Does Assignment Work?

### Scenario

**Before Server 1 dies:**
- 12 partitions, 3 servers, 4 threads per server
- Server 1: Partitions 0–3  
- Server 2: Partitions 4–7  
- Server 3: Partitions 8–11  

**After Server 1 dies:**
- 12 partitions, **2 servers** (Server 2 and Server 3)
- **6 partitions per server** (12 ÷ 2 = 6)
- **4 threads per server** (unchanged)

So each remaining server has **6 partitions** but only **4 threads**. The question: **which thread consumes which partition(s)?**

---

### Kafka Rule: More Partitions Than Threads

When **partitions > threads**, some threads are assigned **multiple partitions**:

- **Partitions** = 6  
- **Threads** = 4  
- So **2 threads get 2 partitions each**, **2 threads get 1 partition each** (or similar split).

Kafka’s **partition assignor** (e.g. RangeAssignor, RoundRobinAssignor) decides the exact mapping. Spring Kafka uses the default assignor (often RangeAssignor or similar).

---

### Example Assignment (RangeAssignor-Style)

**Server 2** (6 partitions, 4 threads):

Assume Server 2 is assigned partitions **0, 1, 2, 3, 4, 5** (all of ex–Server 1’s plus its own). RangeAssignor typically gives **consecutive** partitions to each consumer:

```
Thread 1 → Partitions 0, 1
Thread 2 → Partitions 2, 3
Thread 3 → Partition 4
Thread 4 → Partition 5
```

**Server 3** (6 partitions, 4 threads):

Assume Server 3 keeps **6, 7, 8, 9, 10, 11**:

```
Thread 1 → Partitions 6, 7
Thread 2 → Partitions 8, 9
Thread 3 → Partition 10
Thread 4 → Partition 11
```

Exact mapping can vary (e.g. round‑robin), but the **principle** is the same: **each partition is consumed by exactly one thread**; some threads handle more than one partition.

---

### How Each Thread Consumes Multiple Partitions

**Thread 1** with **Partitions 0 and 1**:

1. **Single consumer** subscribes to both partitions.
2. **One poll** can return records from **both** partitions (interleaved).
3. Your `processOrders` receives a **batch** of orders (possibly from both partitions).
4. You group by symbol and route to OrderBooks:
   - Orders from partition 0 (e.g. symbols A, B) → `OrderBook("A")`, `OrderBook("B")`
   - Orders from partition 1 (e.g. symbols C, D) → `OrderBook("C")`, `OrderBook("D")`

So **one thread** can use **multiple OrderBooks** (one per symbol), but **each symbol** still has a **single** OrderBook. No two threads process the **same** partition.

---

### Summary Table (After Server 1 Dies)

| Server | Partitions assigned | Threads | Assignment |
|--------|---------------------|---------|------------|
| Server 2 | 0, 1, 2, 3, 4, 5 | 4 | e.g. T1→0,1; T2→2,3; T3→4; T4→5 |
| Server 3 | 6, 7, 8, 9, 10, 11 | 4 | e.g. T1→6,7; T2→8,9; T3→10; T4→11 |

- **Each partition** → consumed by **exactly one thread** (across the whole group).
- **Each thread** → can have **1 or 2 partitions** (in this 6 partitions / 4 threads case).
- **Same symbol** → still **same partition** → **same thread** → **same OrderBook**. No OrderBook sharing between threads.

---

### How to Verify in Your Setup

Use Kafka’s consumer-group tool:

```bash
./kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group matching-engine-group --describe
```

You’ll see **which consumer (thread)** is assigned **which partition(s)**. The `CONSUMER-ID` and `HOST` columns show the actual mapping.

---

## Summary

| Question | Answer |
|----------|--------|
| **Can partition 1 and 2 share AAPL?** | ❌ **NO** - Same symbol always goes to same partition |
| **Do threads share OrderBooks?** | ❌ **NO** (normally) - Each symbol processed by one thread |
| **When do threads share OrderBooks?** | ✅ **Only after server death** - When partitions reassigned |
| **How are partitions assigned to threads?** | ✅ **1:1 mapping** - 4 threads = 4 partitions per server |
| **After server death (6 partitions, 4 threads)?** | Some threads get **2 partitions**, some **1**; each partition still **one** thread |

---

## Key Insights

1. **Kafka Guarantee:** Same symbol → Same partition (always)
2. **Kafka Assignment:** One partition → One thread (within consumer group)
3. **Result:** One symbol → One thread → One OrderBook (no sharing in normal operation)
4. **Problem:** Server death → Partition reassignment → New server has empty OrderBook → Data loss
5. **6 partitions, 4 threads:** Partitions are spread across threads (e.g. 2+2+1+1); each partition has exactly one consumer.

**Your concern about OrderBook sharing is valid, but it happens AFTER server death (reassignment), not during normal operation.**
