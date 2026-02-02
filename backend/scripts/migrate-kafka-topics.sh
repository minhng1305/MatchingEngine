#!/bin/bash

# Combined script to migrate from old architecture to new architecture
# 1. Deletes old per-symbol topics (order-aapl, order-msft, etc.)
# 2. Creates new topics (orders, orders-dlq)

KAFKA_BIN_DIR="kafka_2.13-3.9.1/bin"
BOOTSTRAP_SERVER="localhost:9092"

echo "=========================================="
echo "Kafka Topic Migration Script"
echo "=========================================="
echo ""
echo "This script will:"
echo "  1. Delete old per-symbol topics (order-*)"
echo "  2. Create new topics (orders, orders-dlq)"
echo ""

# Step 1: List and delete old topics
echo "Step 1: Finding old topics..."
echo "-----------------------------"

# Get all topics that start with "order-"
OLD_TOPICS=$($KAFKA_BIN_DIR/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVER --list | grep "^order-")

if [ -z "$OLD_TOPICS" ]; then
    echo "No old topics found (starting with 'order-')"
else
    echo "Found old topics:"
    echo "$OLD_TOPICS" | while read topic; do
        echo "  - $topic"
    done
    echo ""
    
    read -p "Delete these old topics? (yes/no): " confirm
    if [ "$confirm" = "yes" ]; then
        echo ""
        echo "Deleting old topics..."
        DELETED=0
        FAILED=0
        
        while IFS= read -r topic; do
            if [ -n "$topic" ]; then
                echo -n "  Deleting $topic... "
                if $KAFKA_BIN_DIR/kafka-topics.sh --delete \
                    --topic "$topic" \
                    --bootstrap-server $BOOTSTRAP_SERVER 2>/dev/null; then
                    echo "✓"
                    DELETED=$((DELETED + 1))
                else
                    echo "✗"
                    FAILED=$((FAILED + 1))
                fi
            fi
        done <<< "$OLD_TOPICS"
        
        echo ""
        echo "Deleted: $DELETED, Failed: $FAILED"
        echo "Waiting 5 seconds for Kafka to process deletions..."
        sleep 5
    else
        echo "Skipping deletion of old topics."
    fi
fi

echo ""
echo "=========================================="
echo "Step 2: Creating new topics"
echo "=========================================="
echo ""

# Step 2: Create new topics
echo "Creating 'orders' topic with 12 partitions..."
if $KAFKA_BIN_DIR/kafka-topics.sh --create \
    --topic orders \
    --partitions 12 \
    --replication-factor 1 \
    --bootstrap-server $BOOTSTRAP_SERVER 2>/dev/null; then
    echo "✅ Successfully created 'orders' topic"
elif $KAFKA_BIN_DIR/kafka-topics.sh --describe --topic orders --bootstrap-server $BOOTSTRAP_SERVER >/dev/null 2>&1; then
    echo "⚠️  Topic 'orders' already exists"
else
    echo "❌ Failed to create 'orders' topic"
fi

echo ""
echo "Creating 'orders-dlq' topic with 3 partitions..."
if $KAFKA_BIN_DIR/kafka-topics.sh --create \
    --topic orders-dlq \
    --partitions 3 \
    --replication-factor 1 \
    --bootstrap-server $BOOTSTRAP_SERVER 2>/dev/null; then
    echo "✅ Successfully created 'orders-dlq' topic"
elif $KAFKA_BIN_DIR/kafka-topics.sh --describe --topic orders-dlq --bootstrap-server $BOOTSTRAP_SERVER >/dev/null 2>&1; then
    echo "⚠️  Topic 'orders-dlq' already exists"
else
    echo "❌ Failed to create 'orders-dlq' topic"
fi

echo ""
echo "=========================================="
echo "Step 3: Verifying new topics"
echo "=========================================="
echo ""

echo "Topic: orders"
$KAFKA_BIN_DIR/kafka-topics.sh --describe \
    --topic orders \
    --bootstrap-server $BOOTSTRAP_SERVER

echo ""
echo "Topic: orders-dlq"
$KAFKA_BIN_DIR/kafka-topics.sh --describe \
    --topic orders-dlq \
    --bootstrap-server $BOOTSTRAP_SERVER

echo ""
echo "=========================================="
echo "Migration Complete!"
echo "=========================================="
echo ""
echo "All topics:"
$KAFKA_BIN_DIR/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVER --list
