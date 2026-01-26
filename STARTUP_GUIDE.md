# Startup Guide - Multi-Server Matching Engine

## Prerequisites

1. **Build the application first:**
   ```bash
   cd backend
   ./gradlew clean build -x test
   ```

2. **Ensure PostgreSQL and Redis are running:**
   ```bash
   # PostgreSQL
   brew services start postgresql
   
   # Redis
   brew services start redis
   ```

3. **Create Kafka topics (if not already created):**
   ```bash
   cd backend
   ./scripts/create-kafka-topics.sh
   ```

---

## Startup Sequence

**Your understanding is correct!** Start services in this order:

1. **Kafka Zookeeper** (Terminal 1)
2. **Kafka Server** (Terminal 2)
3. **Ingress Server** (Terminal 3)
4. **Server 1** (Terminal 4)
5. **Server 2** (Terminal 5)
6. **Server 3** (Terminal 6)

---

## Commands

### Terminal 1: Kafka Zookeeper
```bash
cd backend/kafka_2.13-3.9.1
bin/zookeeper-server-start.sh config/zookeeper.properties
```

**Wait for:** `[ZooKeeperMain] INFO binding to port 0.0.0.0/0.0.0.0:2181`

---

### Terminal 2: Kafka Server
```bash
cd backend/kafka_2.13-3.9.1
bin/kafka-server-start.sh config/server.properties
```

**Wait for:** `[KafkaServer] INFO started (kafka.server.KafkaServer)`

---

### Terminal 3: Ingress Server (Port 8085)
```bash
cd backend
java -jar build/libs/matchingengine-0.0.1-SNAPSHOT.jar --spring.profiles.active=ingress
```

**Or using Gradle:**
```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=ingress'
```

**Wait for:** `Started Application in X.XXX seconds`

---

### Terminal 4: Server 1 (Port 8080)
```bash
cd backend
java -jar build/libs/matchingengine-0.0.1-SNAPSHOT.jar --spring.profiles.active=server1
```

**Or using Gradle:**
```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=server1'
```

---

### Terminal 5: Server 2 (Port 8081)
```bash
cd backend
java -jar build/libs/matchingengine-0.0.1-SNAPSHOT.jar --spring.profiles.active=server2
```

**Or using Gradle:**
```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=server2'
```

---

### Terminal 6: Server 3 (Port 8082)
```bash
cd backend
java -jar build/libs/matchingengine-0.0.1-SNAPSHOT.jar --spring.profiles.active=server3
```

**Or using Gradle:**
```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=server3'
```

---

## Verification

### Check All Services Are Running

**Check ports:**
```bash
netstat -an | grep LISTEN | grep -E "(2181|9092|8080|8081|8082|8085)"
```

**Expected output:**
- `2181` - Zookeeper
- `9092` - Kafka
- `8080` - Server 1
- `8081` - Server 2
- `8082` - Server 3
- `8085` - Ingress Server

### Check Kafka Topics
```bash
cd backend/kafka_2.13-3.9.1/bin
./kafka-topics.sh --bootstrap-server localhost:9092 --list
```

**Expected:**
- `orders` (12 partitions)
- `orders-dlq` (3 partitions)

### Check Partition Assignment
```bash
cd backend
./scripts/check-partition-assignment.sh
```

This shows which server is processing which partition.

---

## Quick Start Script

You can also create a script to start everything. Here's a template:

```bash
#!/bin/bash
# start-all.sh

# Terminal 1: Zookeeper
osascript -e 'tell app "Terminal" to do script "cd '$PWD'/backend/kafka_2.13-3.9.1 && bin/zookeeper-server-start.sh config/zookeeper.properties"'

sleep 3

# Terminal 2: Kafka
osascript -e 'tell app "Terminal" to do script "cd '$PWD'/backend/kafka_2.13-3.9.1 && bin/kafka-server-start.sh config/server.properties"'

sleep 5

# Terminal 3: Ingress
osascript -e 'tell app "Terminal" to do script "cd '$PWD'/backend && java -jar build/libs/matchingengine-0.0.1-SNAPSHOT.jar --spring.profiles.active=ingress"'

sleep 3

# Terminal 4: Server 1
osascript -e 'tell app "Terminal" to do script "cd '$PWD'/backend && java -jar build/libs/matchingengine-0.0.1-SNAPSHOT.jar --spring.profiles.active=server1"'

sleep 2

# Terminal 5: Server 2
osascript -e 'tell app "Terminal" to do script "cd '$PWD'/backend && java -jar build/libs/matchingengine-0.0.1-SNAPSHOT.jar --spring.profiles.active=server2"'

sleep 2

# Terminal 6: Server 3
osascript -e 'tell app "Terminal" to do script "cd '$PWD'/backend && java -jar build/libs/matchingengine-0.0.1-SNAPSHOT.jar --spring.profiles.active=server3"'

echo "All services starting in separate terminals..."
```

---

## Troubleshooting

### "Port already in use"
- Stop the service using that port
- Or change the port in the respective `application-*.properties` file

### "Topic not found"
- Run: `cd backend && ./scripts/create-kafka-topics.sh`

### "Cannot connect to Kafka"
- Ensure Zookeeper is running first
- Wait 5-10 seconds after starting Zookeeper before starting Kafka

### "Consumer group has no active members"
- This is normal if no servers are running
- Start Server 1, 2, 3 to see partition assignments

---

## Shutdown Sequence

Stop in reverse order:
1. Server 3 (Ctrl+C)
2. Server 2 (Ctrl+C)
3. Server 1 (Ctrl+C)
4. Ingress Server (Ctrl+C)
5. Kafka Server (Ctrl+C)
6. Zookeeper (Ctrl+C)

---

## Summary

✅ **Your startup sequence is correct!**

**Commands:**
- **Zookeeper:** `cd backend/kafka_2.13-3.9.1 && bin/zookeeper-server-start.sh config/zookeeper.properties`
- **Kafka:** `cd backend/kafka_2.13-3.9.1 && bin/kafka-server-start.sh config/server.properties`
- **Ingress:** `cd backend && java -jar build/libs/matchingengine-0.0.1-SNAPSHOT.jar --spring.profiles.active=ingress`
- **Server 1:** `cd backend && java -jar build/libs/matchingengine-0.0.1-SNAPSHOT.jar --spring.profiles.active=server1`
- **Server 2:** `cd backend && java -jar build/libs/matchingengine-0.0.1-SNAPSHOT.jar --spring.profiles.active=server2`
- **Server 3:** `cd backend && java -jar build/libs/matchingengine-0.0.1-SNAPSHOT.jar --spring.profiles.active=server3`
