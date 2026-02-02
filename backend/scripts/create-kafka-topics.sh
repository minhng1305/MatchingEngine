#!/bin/bash

# Phase 1: Kafka Topic Setup Script
# This script creates the required Kafka topics for the new architecture

KAFKA_BIN_DIR="kafka_2.13-3.9.1/bin"
BOOTSTRAP_SERVER="localhost:9092"

echo "=========================================="
echo "Creating Kafka Topics for New Architecture"
echo "=========================================="

# Create main orders topic with 12 partitions
echo "Creating 'orders' topic with 12 partitions..."
$KAFKA_BIN_DIR/kafka-topics.sh --create \
  --topic orders \
  --partitions 12 \
  --replication-factor 1 \
  --bootstrap-server $BOOTSTRAP_SERVER

if [ $? -eq 0 ]; then
    echo "✅ Successfully created 'orders' topic"
else
    echo "⚠️  Topic 'orders' may already exist or error occurred"
fi

# Create Dead Letter Queue topic
echo ""
echo "Creating 'orders-dlq' topic for error handling..."
$KAFKA_BIN_DIR/kafka-topics.sh --create \
  --topic orders-dlq \
  --partitions 3 \
  --replication-factor 1 \
  --bootstrap-server $BOOTSTRAP_SERVER

if [ $? -eq 0 ]; then
    echo "✅ Successfully created 'orders-dlq' topic"
else
    echo "⚠️  Topic 'orders-dlq' may already exist or error occurred"
fi

# Verify topics
echo ""
echo "=========================================="
echo "Verifying Topics"
echo "=========================================="
$KAFKA_BIN_DIR/kafka-topics.sh --describe \
  --topic orders \
  --bootstrap-server $BOOTSTRAP_SERVER

echo ""
$KAFKA_BIN_DIR/kafka-topics.sh --describe \
  --topic orders-dlq \
  --bootstrap-server $BOOTSTRAP_SERVER

echo ""
echo "=========================================="
echo "Topic Creation Complete"
echo "=========================================="
