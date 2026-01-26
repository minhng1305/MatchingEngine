# Industry Standard Routing Implementation Plan

## Table of Contents
1. [Target System Design](#target-system-design)
2. [Architecture Overview](#architecture-overview)
3. [Step-by-Step Implementation Guide](#step-by-step-implementation-guide)
4. [Code Changes Required](#code-changes-required)
5. [Configuration Changes](#configuration-changes)
6. [Testing Strategy](#testing-strategy)

---

## Target System Design

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Load Balancer                            │
└────────────────────┬────────────────────────────────────────┘
                     │
         ┌───────────┼───────────┐
         │           │           │
    ┌────▼────┐ ┌────▼────┐ ┌────▼────┐
    │Ingress1 │ │Ingress2 │ │Ingress3 │  (Stateless, Port 8085)
    │:8085    │ │:8085    │ │:8085    │
    └────┬────┘ └────┬────┘ └────┬────┘
         │           │           │
         └───────────┼───────────┘
                     │
              ┌──────▼──────┐
              │   Kafka     │
              │  Topic:     │
              │  "orders"   │
              │             │
              │ Partitions: │
              │  12-24      │
              │  (keyed by  │
              │   symbol)   │
              └──────┬──────┘
                     │
         ┌───────────┼───────────┐
         │           │           │
    ┌────▼────┐ ┌────▼────┐ ┌────▼────┐
    │Server1  │ │Server2  │ │Server3  │
    │:8080    │ │:8081    │ │:8082    │
    │         │ │         │ │         │
    │Consumer │ │Consumer │ │Consumer │
    │Group:   │ │Group:   │ │Group:   │
    │matching │ │matching │ │matching │
    │-engine  │ │-engine  │ │-engine  │
    │-group   │ │-group   │ │-group   │
    └─────────┘ └─────────┘ └─────────┘
         │           │           │
         └───────────┼───────────┘
                     │
              ┌──────▼──────┐
              │  PostgreSQL │
              │  Database   │
              └─────────────┘
```

### Key Design Principles

1. **Single Ingress Layer**
   - Stateless ingress servers (can run multiple instances)
   - Handles authentication, risk checks, formatting
   - Produces to Kafka only

2. **Single Kafka Topic**
   - Topic name: `orders`
   - Keyed by `symbol` (ensures all orders for a symbol go to same partition)
   - Multiple partitions (12-24 recommended) for parallelism

3. **Consumer Group Pattern**
   - All 3 matching servers in same consumer group: `matching-engine-group`
   - Kafka automatically distributes partitions across instances
   - One active consumer thread per partition

4. **Thread-Safe OrderBooks**
   - Each symbol has its own OrderBook
   - Protected by locks for concurrent access
   - Per-partition ordering + symbol scoping = correctness

5. **Idempotent Producer & DLQ**
   - Producer: `enable.idempotence=true`, `acks=all`
   - Consumer: Dead Letter Queue for failed messages

---

## Architecture Overview

### Current System (Before Changes)

```
Client → OrderController → OrderService → KafkaProducer
                                    ↓
                          Topic: order-{symbol}
                          (One topic per symbol)
                                    ↓
                          KafkaConsumer (per server)
                          (Subscribes to assigned symbols)
```

**Issues:**
- One topic per symbol (many topics to manage)
- Static symbol assignment per server
- No load balancing across servers
- Hard to scale

### New System (After Changes)

```
Client → Load Balancer → Ingress Server → KafkaProducer
                                              ↓
                                    Topic: "orders"
                                    Key: symbol
                                    (Single topic, multiple partitions)
                                              ↓
                                    KafkaConsumer (all servers)
                                    (Same consumer group)
                                    (Partitions distributed automatically)
```

**Benefits:**
- Single topic (easier management)
- Automatic load balancing (Kafka distributes partitions)
- Easy scaling (add more consumer instances)
- Industry-standard pattern

---

## Step-by-Step Implementation Guide

### Phase 1: Kafka Topic Setup

#### Step 1.1: Create Single Kafka Topic

**Action:** Create the `orders` topic with appropriate partitions

```bash
# Navigate to Kafka bin directory
cd backend/kafka_2.13-3.9.1/bin

# Create topic with 12 partitions (adjust based on your needs)
./kafka-topics.sh --create \
  --topic orders \
  --partitions 12 \
  --replication-factor 1 \
  --bootstrap-server localhost:9092

# Verify topic creation
./kafka-topics.sh --describe \
  --topic orders \
  --bootstrap-server localhost:9092
```

**Partition Count Calculation:**
- Recommended: 12-24 partitions
- Formula: `partitions = number_of_servers × threads_per_server`
- Example: 3 servers × 4 threads = 12 partitions minimum
- Add extra partitions for hot symbols (dedicated partitions)

**Note:** You can increase partitions later, but decreasing requires recreating the topic.

#### Step 1.2: (Optional) Create Dead Letter Queue Topic

```bash
./kafka-topics.sh --create \
  --topic orders-dlq \
  --partitions 3 \
  --replication-factor 1 \
  --bootstrap-server localhost:9092
```

---

### Phase 2: Ingress Server Setup

#### Step 2.1: Create Ingress Server Configuration

**New File:** `backend/src/main/resources/application-ingress.properties`

```properties
spring.application.name=matchingengine-ingress

# Server Port
server.port=8085

# Kafka Producer Configuration
spring.kafka.producer.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

# Kafka Topic Configuration
app.kafka.topic.orders=orders

# No database needed (stateless)
# No consumer needed (only produces)

# CORS Configuration
spring.web.cors.allowed-origins=http://localhost:3000
```

#### Step 2.2: Create Ingress Controller

**New File:** `backend/src/main/java/com/project/matchingengine/controllers/ingress/IngressController.java`

**Purpose:** Receives all orders and produces to Kafka

**Key Changes from Current OrderController:**
- No database operations
- No OrderBook access
- Only produces to Kafka
- Stateless (can run multiple instances)

#### Step 2.3: Update Ingress KafkaProducer

**Modify:** `KafkaProducer.java` (or create IngressKafkaProducer)

**Changes:**
- Use single topic: `orders`
- Key = `order.getSymbol()` (not orderId)
- This ensures symbol-based partitioning

---

### Phase 3: Update Matching Servers

#### Step 3.1: Update TopicListProvider

**File:** `backend/src/main/java/com/project/matchingengine/service/kafka/TopicListProvider.java`

**Current Behavior:**
- Reads `app.kafka.topics.assigned-symbols`
- Creates topic list: `["order-aapl", "order-msft", ...]`

**New Behavior:**
- Returns single topic: `["orders"]`
- All servers subscribe to same topic

**Change:**
```java
public String[] getTopics() {
    // Return single topic instead of per-symbol topics
    return new String[]{"orders"};
}
```

#### Step 3.2: Update KafkaConsumer

**File:** `backend/src/main/java/com/project/matchingengine/service/kafka/KafkaConsumer.java`

**Current Behavior:**
- Consumes from multiple topics (per symbol)
- Groups orders by symbol within batch

**New Behavior:**
- Consumes from single topic `orders`
- Still groups by symbol (for efficiency)
- Kafka guarantees same symbol = same partition = ordered

**No major changes needed** - current grouping logic is already correct!

#### Step 3.3: Update Consumer Configuration

**File:** `backend/src/main/java/com/project/matchingengine/config/KafkaConsumerConfig.java`

**Current:**
- `concurrency=3` (3 threads per partition)
- Batch listener enabled

**Update:**
- Set `concurrency` based on partition count
- Formula: `concurrency = total_partitions / number_of_servers`
- Example: 12 partitions / 3 servers = 4 threads per server

**Important:** Total consumer threads across all servers should not exceed partition count.

---

### Phase 4: Make OrderBook Thread-Safe

#### Step 4.1: Add Locks to OrderBook

**File:** `backend/src/main/java/com/project/matchingengine/models/order/OrderBook.java`

**Current Issue:**
- `PriorityQueue` is not thread-safe
- `ArrayList` is not thread-safe
- Multiple consumer threads can access same OrderBook concurrently

**Solution:**
- Add `ReentrantReadWriteLock`
- Write lock for: `addOrder()`, `matchBuyOrder()`, `matchSellOrder()`
- Read lock for: `getOrderBookSummary()`, `getAllOrdersToUpdate()`

**Why This Works:**
- Each symbol has its own OrderBook
- Kafka ensures same symbol = same partition = same consumer thread
- But multiple threads can process different symbols concurrently
- Locks protect against concurrent access to same OrderBook

---

### Phase 5: Update Producer Configuration

#### Step 5.1: Ensure Idempotent Producer

**File:** `backend/src/main/java/com/project/matchingengine/config/KafkaProducerConfig.java`

**Current:**
- Already has `ENABLE_IDEMPOTENCE_CONFIG = true` ✅

**Add:**
- `acks=all` (ensure all replicas acknowledge)
- `retries=3` (retry on transient failures)

---

### Phase 6: Add Error Handling (DLQ)

#### Step 6.1: Configure Dead Letter Queue

**File:** `backend/src/main/java/com/project/matchingengine/config/KafkaConsumerConfig.java`

**Add:**
- `DefaultErrorHandler` with `DeadLetterPublishingRecoverer`
- Retry logic (3 attempts with backoff)
- Route failed messages to `orders-dlq` topic

---

### Phase 7: Update Application Properties

#### Step 7.1: Update All Server Configurations

**Files to Update:**
- `application-server1.properties`
- `application-server2.properties`
- `application-server3.properties`

**Changes:**
1. Remove `app.kafka.topics.assigned-symbols` (no longer needed)
2. Ensure all use same `spring.kafka.consumer.group-id=matching-engine-group`
3. Add `app.kafka.topic.orders=orders`

---

## Code Changes Required

### 1. KafkaProducer.java

**Current Code:**
```java
public void sendOrder(Order order) {
    String topicName = generateTopicName(order.getSymbol()); // order-aapl
    kafkaTemplate.send(topicName, order.getOrderId().toString(), orderJson);
}
```

**Required Changes:**
```java
@Value("${app.kafka.topic.orders:orders}")
private String ordersTopic;

public void sendOrder(Order order) {
    String orderJson = objectMapper.writeValueAsString(order);
    // Use single topic
    // Key = symbol (for partitioning)
    kafkaTemplate.send(ordersTopic, order.getSymbol(), orderJson);
}
```

**Key Changes:**
- Topic: `orders` (constant, not per-symbol)
- Key: `order.getSymbol()` (not `orderId`)
- This ensures symbol-based partitioning

---

### 2. TopicListProvider.java

**Current Code:**
```java
public String[] getTopics() {
    List<String> topics = Arrays.stream(assignedSymbols.split(","))
        .map(symbol -> orderTopicPrefix + symbol.toLowerCase())
        .collect(Collectors.toList());
    return topics.toArray(new String[0]);
}
```

**Required Changes:**
```java
@Value("${app.kafka.topic.orders:orders}")
private String ordersTopic;

public String[] getTopics() {
    // All servers subscribe to same topic
    return new String[]{ordersTopic};
}
```

**Key Changes:**
- Remove dependency on `assigned-symbols`
- Return single topic: `["orders"]`
- All servers get same topic list

---

### 3. KafkaConsumer.java

**Current Code:**
```java
@KafkaListener(
    topics = "#{@topicListProvider.getTopics()}",
    groupId = "${spring.kafka.consumer.group-id}",
    containerFactory = "kafkaListenerContainerFactory"
)
public void processOrders(List<String> orderJsonBatch) {
    // Groups by symbol - this is already correct!
    Map<String, List<Order>> ordersBySymbol = orders.stream()
        .collect(Collectors.groupingBy(Order::getSymbol));
    // ...
}
```

**Required Changes:**
- **No changes needed!** ✅
- Current grouping logic is perfect
- Kafka ensures same symbol = same partition = ordered processing

**Optional Optimization:**
- Add logging to show which partition is being processed
- Add metrics for processing time per symbol

---

### 4. KafkaConsumerConfig.java

**Current Code:**
```java
factory.setConcurrency(3); // 3 threads per partition
```

**Required Changes:**
```java
@Value("${app.kafka.consumer.concurrency:4}")
private int concurrency;

factory.setConcurrency(concurrency); // Adjust based on partitions
```

**Calculation:**
- 12 partitions / 3 servers = 4 threads per server
- Set in properties: `app.kafka.consumer.concurrency=4`

**Add Error Handler:**
```java
@Bean
public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> kafkaTemplate) {
    DeadLetterPublishingRecoverer recoverer = 
        new DeadLetterPublishingRecoverer(kafkaTemplate);
    FixedBackOff backOff = new FixedBackOff(1000L, 3L); // 3 retries, 1s delay
    return new DefaultErrorHandler(recoverer, backOff);
}

// In kafkaListenerContainerFactory:
factory.setCommonErrorHandler(errorHandler);
```

---

### 5. OrderBook.java

**Current Code:**
```java
public class OrderBook {
    private final PriorityQueue<Order> buyOrdersList;
    private final PriorityQueue<Order> sellOrdersList;
    private final ArrayList<Trade> trades;
    
    public void addOrder(Order order) {
        // Not thread-safe!
        if (order.getSide() == OrderSide.BUY) {
            matchBuyOrder(order);
            buyOrdersList.add(order);
        }
    }
}
```

**Required Changes:**
```java
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class OrderBook {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReadLock readLock = lock.readLock();
    private final WriteLock writeLock = lock.writeLock();
    
    public void addOrder(Order order) {
        writeLock.lock();
        try {
            if (order.getSide() == OrderSide.BUY) {
                matchBuyOrder(order);
                if (order.getCurrentQuantity() > 0) {
                    buyOrdersList.add(order);
                }
            } else {
                matchSellOrder(order);
                if (order.getCurrentQuantity() > 0) {
                    sellOrdersList.add(order);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }
    
    public OrderBookSummary getOrderBookSummary() {
        readLock.lock();
        try {
            return this.orderBookSummary;
        } finally {
            readLock.unlock();
        }
    }
    
    // Apply locks to all public methods that access shared state
}
```

**Key Changes:**
- Add `ReentrantReadWriteLock`
- Write lock: `addOrder()`, `matchBuyOrder()`, `matchSellOrder()`
- Read lock: `getOrderBookSummary()`, `getAllOrdersToUpdate()`, `getCurrentPrice()`

---

### 6. KafkaProducerConfig.java

**Current Code:**
```java
configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
```

**Required Changes:**
```java
configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // Ensure all replicas acknowledge
configProps.put(ProducerConfig.RETRIES_CONFIG, 3); // Retry on failures
configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5); // For idempotence
```

---

### 7. Create Ingress Controller (New)

**New File:** `backend/src/main/java/com/project/matchingengine/controllers/ingress/IngressController.java`

```java
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:3000")
public class IngressController {
    
    @Autowired
    private KafkaProducer kafkaProducer;
    
    @PostMapping("/submit")
    public ResponseEntity<?> submitOrder(@RequestBody Map<String, Object> orderData) {
        try {
            // Validate and create Order object (same as current OrderController)
            Order order = createOrderFromRequest(orderData);
            
            // Only produce to Kafka (no database, no OrderBook access)
            kafkaProducer.sendOrder(order);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "orderId", order.getOrderId().toString(),
                "message", "Order submitted successfully"
            ));
        } catch (Exception e) {
            logger.error("Error submitting order: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}
```

**Key Points:**
- Stateless (no database, no OrderBook)
- Only produces to Kafka
- Can run multiple instances behind load balancer

---

## Configuration Changes

### application-ingress.properties (New)

```properties
spring.application.name=matchingengine-ingress
server.port=8085

# Kafka Producer
spring.kafka.producer.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

# Topic Configuration
app.kafka.topic.orders=orders

# No consumer needed
# No database needed
```

### application-server1.properties (Update)

```properties
# Remove this line:
# app.kafka.topics.assigned-symbols=AAPL,GOOGL,MSFT,AMZN,TSLA,META,NFLX

# Add this:
app.kafka.topic.orders=orders
app.kafka.consumer.concurrency=4

# Ensure same consumer group:
spring.kafka.consumer.group-id=matching-engine-group
```

### application-server2.properties (Update)

```properties
# Remove:
# app.kafka.topics.assigned-symbols=NVDA,AMD,INTC,IBM,ORCL,CSCO,SAP

# Add:
app.kafka.topic.orders=orders
app.kafka.consumer.concurrency=4

# Same consumer group:
spring.kafka.consumer.group-id=matching-engine-group
```

### application-server3.properties (Update)

```properties
# Remove:
# app.kafka.topics.assigned-symbols=ADOBE,CRM,TWTR,SNAP,BABA,TCEHY

# Add:
app.kafka.topic.orders=orders
app.kafka.consumer.concurrency=4

# Same consumer group:
spring.kafka.consumer.group-id=matching-engine-group
```

---

## Testing Strategy

### Test 1: Topic Creation

```bash
# Verify topic exists
kafka-topics.sh --describe --topic orders --bootstrap-server localhost:9092

# Should show 12 partitions
```

### Test 2: Symbol-Based Partitioning

```bash
# Send test orders with different symbols
# Verify they go to different partitions

# Monitor partition assignment:
kafka-console-consumer.sh \
  --topic orders \
  --from-beginning \
  --bootstrap-server localhost:9092 \
  --property print.partition=true \
  --property print.key=true
```

### Test 3: Consumer Group Distribution

**Start all 3 servers:**
```bash
# Terminal 1
mvn spring-boot:run -Dspring-boot.run.profiles=server1

# Terminal 2
mvn spring-boot:run -Dspring-boot.run.profiles=server2

# Terminal 3
mvn spring-boot:run -Dspring-boot.run.profiles=server3
```

**Verify partition assignment:**
- Check logs: Each server should show which partitions it's consuming
- Kafka should distribute 12 partitions across 3 servers (4 partitions each)

### Test 4: Order Processing

**Send orders via Ingress:**
```bash
curl -X POST http://localhost:8085/api/orders/submit \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "AAPL",
    "side": "BUY",
    "quantity": 100,
    "price": 150.0,
    "userId": "test-user-id"
  }'
```

**Verify:**
- Order appears in Kafka topic
- One of the matching servers processes it
- OrderBook is updated
- WebSocket broadcast sent

### Test 5: Concurrent Processing

**Send multiple orders for different symbols:**
```bash
# Send 100 orders for AAPL, GOOGL, MSFT simultaneously
# Verify all are processed correctly
# Verify no data corruption in OrderBooks
```

### Test 6: Failover

**Kill one matching server:**
- Kafka should redistribute partitions to remaining servers
- Orders should continue processing
- No data loss

---

## Deployment Checklist

### Pre-Deployment

- [ ] Create Kafka topic `orders` with appropriate partitions
- [ ] Create DLQ topic `orders-dlq`
- [ ] Update all configuration files
- [ ] Make OrderBook thread-safe
- [ ] Update KafkaProducer to use symbol as key
- [ ] Update TopicListProvider to return single topic
- [ ] Test locally with all 3 servers

### Deployment Steps

1. **Deploy Ingress Server**
   - Start ingress server on port 8085
   - Verify it can produce to Kafka
   - (Optional) Deploy multiple instances behind load balancer

2. **Deploy Matching Servers**
   - Start Server1, Server2, Server3
   - Verify all join same consumer group
   - Verify partitions are distributed evenly

3. **Update Frontend**
   - Change API endpoint from `http://localhost:8080` to `http://localhost:8085`
   - Or use load balancer URL

4. **Monitor**
   - Check Kafka consumer lag
   - Monitor partition distribution
   - Watch for errors in DLQ

---

## Performance Considerations

### Partition Count

**Formula:**
```
partitions = (number_of_servers × threads_per_server) + buffer
```

**Example:**
- 3 servers × 4 threads = 12 partitions minimum
- Add 4-8 extra for hot symbols = 16-20 partitions total

### Hot Symbols

**If one symbol dominates traffic:**
- Option 1: Give it dedicated partition(s)
- Option 2: Create separate topic for hot symbols
- Option 3: Increase partitions for that symbol's hash range

### Consumer Threads

**Rule:**
- Total threads across all servers ≤ Total partitions
- Example: 12 partitions, 3 servers → 4 threads per server max

**Too many threads:**
- Some threads will be idle
- Wasted resources

**Too few threads:**
- Underutilized partitions
- Lower throughput

---

## Troubleshooting

### Issue: Orders Not Processing

**Check:**
1. Kafka topic exists: `kafka-topics.sh --list`
2. Consumers are subscribed: Check server logs
3. Consumer group is active: `kafka-consumer-groups.sh --describe --group matching-engine-group`
4. Partitions are assigned: Check consumer logs

### Issue: Orders Going to Wrong Partition

**Check:**
1. Producer key = symbol (not orderId)
2. Topic has enough partitions
3. Hash function is working correctly

### Issue: Consumer Lag Growing

**Solutions:**
1. Increase consumer threads (up to partition count)
2. Increase batch size
3. Add more consumer instances
4. Optimize OrderBook processing

### Issue: Data Corruption

**Check:**
1. OrderBook locks are properly implemented
2. No concurrent access to same OrderBook
3. Kafka guarantees are working (same symbol = same partition)

---

## Summary

### What Changes

1. **Kafka Topic:** One topic `orders` instead of per-symbol topics
2. **Producer Key:** Symbol instead of orderId
3. **Consumer Subscription:** All servers subscribe to same topic
4. **Consumer Group:** All servers in same group (automatic load balancing)
5. **OrderBook:** Thread-safe with locks
6. **Ingress Server:** New stateless server for order submission

### What Stays the Same

1. **OrderBook Logic:** Matching algorithm unchanged
2. **Database:** Same persistence strategy
3. **WebSocket:** Same broadcasting
4. **User Cache:** Same caching strategy

### Benefits

- ✅ Industry-standard architecture
- ✅ Automatic load balancing
- ✅ Easy scaling (add more consumers)
- ✅ Better performance (parallel processing)
- ✅ Simplified management (one topic)

---

## Next Steps

1. Review this plan
2. Create Kafka topics
3. Implement code changes (one file at a time)
4. Test incrementally
5. Deploy to staging
6. Monitor and optimize

**Ready to implement?** Start with Phase 1 (Kafka topic setup) and work through each phase systematically.