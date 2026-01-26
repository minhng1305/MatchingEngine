# How Kafka Distributes Partitions Across Servers

## Quick Answer

**Kafka automatically distributes partitions across servers in the same consumer group.** You don't need to manually assign partitions - Kafka's consumer group coordinator handles this automatically.

---

## How It Works

### Consumer Group Coordination

When multiple servers join the **same consumer group** (`matching-engine-group`), Kafka's **Group Coordinator** automatically:

1. **Detects all consumers** in the group
2. **Assigns partitions** to each consumer
3. **Rebalances** when consumers join/leave
4. **Ensures** each partition is consumed by exactly one consumer

### Your Current Setup

```
Topic: "orders" (12 partitions)
Consumer Group: "matching-engine-group"

Server1 (4 consumer threads) → Gets assigned partitions: 0, 1, 2, 3
Server2 (4 consumer threads) → Gets assigned partitions: 4, 5, 6, 7
Server3 (4 consumer threads) → Gets assigned partitions: 8, 9, 10, 11
```

**Key Points:**
- Each partition is consumed by **exactly one thread** (across all servers)
- Kafka **automatically** distributes partitions evenly
- If Server1 dies, its partitions are **automatically reassigned** to Server2/Server3

---

## How Messages Are Routed to Partitions

### Producer Side (Ingress Server)

```java
// In KafkaProducer.java
kafkaTemplate.send(ordersTopic, symbol, orderJson);
//                      ↑         ↑
//                   topic      key
```

**Kafka's Partitioning Formula:**
```
partition = hash(key) % number_of_partitions
```

**Example:**
- Symbol "AAPL" → hash("AAPL") = 12345 → 12345 % 12 = 9 → **Partition 9**
- Symbol "GOOGL" → hash("GOOGL") = 67890 → 67890 % 12 = 6 → **Partition 6**
- Symbol "MSFT" → hash("MSFT") = 11111 → 11111 % 12 = 11 → **Partition 11**

**Important:** Same symbol always hashes to same partition (guaranteed by Kafka)

### Consumer Side (Matching Servers)

**Kafka automatically assigns partitions to consumers:**

1. **Server1 starts** → Joins consumer group → Gets assigned partitions (e.g., 0-3)
2. **Server2 starts** → Joins consumer group → Gets assigned partitions (e.g., 4-7)
3. **Server3 starts** → Joins consumer group → Gets assigned partitions (e.g., 8-11)

**Each server's consumer threads process only their assigned partitions.**

---

## How to Verify Partition Assignment

### Method 1: Check Consumer Group Status (Recommended)

```bash
# Navigate to Kafka bin directory
cd backend/kafka_2.13-3.9.1/bin

# Describe consumer group to see partition assignments
./kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group matching-engine-group \
  --describe
```

**Expected Output:**
```
GROUP           TOPIC   PARTITION  CURRENT-OFFSET  LAG  CONSUMER-ID                    HOST        CLIENT-ID
matching-engine orders  0          100             0    consumer-1-abc123              /192.168.1.1 consumer-1
matching-engine orders  1          150             0    consumer-1-abc123              /192.168.1.1 consumer-1
matching-engine orders  2          200             0    consumer-1-abc123              /192.168.1.1 consumer-1
matching-engine orders  3          75              0    consumer-1-abc123              /192.168.1.1 consumer-1
matching-engine orders  4          300             0    consumer-2-def456              /192.168.1.2 consumer-2
matching-engine orders  5          250             0    consumer-2-def456              /192.168.1.2 consumer-2
...
```

**What This Shows:**
- **PARTITION**: Which partition
- **CONSUMER-ID**: Which consumer thread is processing it
- **HOST**: Which server (IP address)
- **CURRENT-OFFSET**: How many messages processed
- **LAG**: Messages waiting to be processed (0 = caught up)

### Method 2: Add Logging to Your Consumer

Add partition information to your consumer logs:

```java
@KafkaListener(
    topics = "${app.kafka.topic.orders:orders}",
    groupId = "${spring.kafka.consumer.group-id}",
    containerFactory = "kafkaListenerContainerFactory"
)
public void processOrders(
    List<String> orderJsonBatch,
    @Header(KafkaHeaders.RECEIVED_PARTITION_ID) List<Integer> partitions,
    @Header(KafkaHeaders.RECEIVED_TOPIC) List<String> topics
) {
    logger.info("Processing batch: {} orders from partitions: {}", 
        orderJsonBatch.size(), partitions);
    // ... rest of processing
}
```

### Method 3: Check Server Logs

When servers start, Spring Kafka logs partition assignments:

```
[main] o.a.k.c.c.internals.ConsumerCoordinator : [Consumer clientId=consumer-matching-engine-group-1, groupId=matching-engine-group] 
  Assigned partitions: [orders-0, orders-1, orders-2, orders-3]
```

---

## Example: Complete Flow

### Scenario: Order for "AAPL" is submitted

**Step 1: Ingress Server (Port 8085)**
```
Client → POST /api/orders/submit {symbol: "AAPL", ...}
  ↓
IngressController.submitOrder()
  ↓
KafkaProducer.sendOrder(order)
  ↓
kafkaTemplate.send("orders", "AAPL", orderJson)
  ↓
Kafka calculates: hash("AAPL") % 12 = partition 9
  ↓
Message sent to: Topic "orders", Partition 9
```

**Step 2: Kafka**
```
Topic: orders
Partition 9: [Order1-AAPL, Order2-AAPL, Order3-AAPL, ...]
```

**Step 3: Consumer Assignment**
```
Consumer Group: matching-engine-group

Server1 (threads 0-3): Assigned partitions [0, 1, 2, 3]
Server2 (threads 4-7): Assigned partitions [4, 5, 6, 7]
Server3 (threads 8-11): Assigned partitions [8, 9, 10, 11]
                                      ↑
                              Partition 9 assigned to Server3
```

**Step 4: Server3 Processes Order**
```
Server3's Consumer Thread (assigned to partition 9)
  ↓
Receives batch: [Order1-AAPL, Order2-AAPL, ...]
  ↓
KafkaConsumer.processOrders()
  ↓
Groups by symbol: {AAPL: [Order1, Order2, ...]}
  ↓
OrderBook("AAPL").addOrder(Order1)
OrderBook("AAPL").addOrder(Order2)
  ↓
Matching happens
  ↓
WebSocket broadcast
```

---

## Key Concepts

### 1. Partition Assignment is Automatic

**You don't configure which server gets which partition.** Kafka's Group Coordinator does this automatically using:

- **Range Assignor** (default): Assigns consecutive partitions
- **Round Robin Assignor**: Distributes evenly
- **Sticky Assignor**: Minimizes rebalancing

### 2. Same Symbol = Same Partition

**Guaranteed by Kafka:**
- Symbol "AAPL" always → Partition 9 (hash("AAPL") % 12)
- All orders for "AAPL" go to partition 9
- Only one consumer thread processes partition 9
- **Result:** Orders for "AAPL" are processed in order

### 3. Rebalancing

**When a server joins/leaves:**
```
Before: Server1 [0-3], Server2 [4-7], Server3 [8-11]

Server1 crashes:
  ↓
Kafka detects: Server1 left group
  ↓
Rebalancing: Reassign partitions [0-3]
  ↓
After: Server2 [0-3, 4-7], Server3 [8-11]
```

**Rebalancing is automatic** - no manual intervention needed.

---

## How to Monitor Partition Assignment

### Real-Time Monitoring Script

Create a script to continuously monitor partition assignments:

```bash
#!/bin/bash
# monitor-partitions.sh

while true; do
  clear
  echo "=== Kafka Partition Assignment ==="
  echo "Time: $(date)"
  echo ""
  
  ./kafka-consumer-groups.sh \
    --bootstrap-server localhost:9092 \
    --group matching-engine-group \
    --describe \
    | grep "orders"
  
  echo ""
  echo "Press Ctrl+C to stop"
  sleep 5
done
```

### Check Which Partition a Symbol Maps To

```bash
# Calculate partition for a symbol (for debugging)
# This is what Kafka does internally

# You can't directly query this, but you can:
# 1. Send a test message with that symbol
# 2. Check which partition it appears in
# 3. Or add logging to your producer
```

---

## Adding Partition Logging to Your Code

### Option 1: Log in Producer (See which partition messages go to)

**Update `KafkaProducer.java`:**

```java
public void sendOrder(Order order) {
    try {
        String orderJson = objectMapper.writeValueAsString(order);
        String symbol = order.getSymbol();
        
        // Send and get result to see partition
        ListenableFuture<SendResult<String, String>> future = 
            kafkaTemplate.send(ordersTopic, symbol, orderJson);
        
        future.addCallback(
            result -> {
                int partition = result.getRecordMetadata().partition();
                logger.info("Order {} for symbol {} sent to partition {}", 
                    order.getOrderId(), symbol, partition);
            },
            failure -> logger.error("Failed to send order: {}", failure.getMessage())
        );
    } catch (Exception e) {
        throw new RuntimeException("Failed to serialize order to JSON", e);
    }
}
```

### Option 2: Log in Consumer (See which partition is being processed)

**Update `KafkaConsumer.java`:**

```java
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;

@KafkaListener(
    topics = "${app.kafka.topic.orders:orders}",
    groupId = "${spring.kafka.consumer.group-id}",
    containerFactory = "kafkaListenerContainerFactory"
)
public void processOrders(
    List<String> orderJsonBatch,
    @Header(KafkaHeaders.RECEIVED_PARTITION_ID) List<Integer> partitions
) {
    // Log which partitions this batch came from
    Set<Integer> uniquePartitions = new HashSet<>(partitions);
    logger.info("Processing {} orders from partitions: {}", 
        orderJsonBatch.size(), uniquePartitions);
    
    // ... rest of processing
}
```

---

## Summary

### How Partition Assignment Works

1. **Automatic**: Kafka's Group Coordinator assigns partitions
2. **Dynamic**: Rebalances when servers join/leave
3. **Fair**: Distributes partitions evenly across consumers
4. **Guaranteed**: Each partition consumed by exactly one thread

### How Symbol → Partition Mapping Works

1. **Producer**: Uses `hash(symbol) % partitions` to determine partition
2. **Guaranteed**: Same symbol always → same partition
3. **Result**: Orders for same symbol processed in order

### How to Verify

1. **Check consumer group**: `kafka-consumer-groups.sh --describe`
2. **Add logging**: Log partition info in producer/consumer
3. **Monitor logs**: Check server startup logs for assignments

### Your Current Setup

- **12 partitions** → Distributed across 3 servers
- **4 threads per server** → Each thread gets ~1 partition
- **Automatic assignment** → Kafka handles everything
- **Symbol-based routing** → Same symbol = same partition = ordered processing

**You don't need to manually assign partitions - Kafka does it automatically!**
