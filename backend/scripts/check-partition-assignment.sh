#!/bin/bash

# Script to check which server is processing which Kafka partition
# This shows the partition assignment across all servers in the consumer group

KAFKA_BIN_DIR="kafka_2.13-3.9.1/bin"
BOOTSTRAP_SERVER="localhost:9092"
CONSUMER_GROUP="matching-engine-group"

echo "=========================================="
echo "Kafka Partition Assignment Check"
echo "=========================================="
echo ""
echo "Consumer Group: $CONSUMER_GROUP"
echo "Topic: orders"
echo ""

# Check consumer group status
echo "Partition Assignment:"
echo "-------------------"
$KAFKA_BIN_DIR/kafka-consumer-groups.sh \
  --bootstrap-server $BOOTSTRAP_SERVER \
  --group $CONSUMER_GROUP \
  --describe

echo ""
echo "=========================================="
echo "Interpretation:"
echo "=========================================="
echo ""
echo "COLUMNS:"
echo "  PARTITION: Partition number (0-11)"
echo "  CURRENT-OFFSET: Messages processed"
echo "  LAG: Messages waiting (0 = caught up)"
echo "  CONSUMER-ID: Which consumer thread is processing"
echo "  HOST: Which server (IP address)"
echo ""
echo "Example:"
echo "  If you see 'orders 5' with HOST '/127.0.0.1:8081'"
echo "  → Partition 5 is being processed by Server2 (port 8081)"
echo ""
echo "Note: Kafka automatically distributes partitions."
echo "You don't need to manually assign them."
