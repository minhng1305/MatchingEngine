#!/bin/bash

# Script to delete old per-symbol Kafka topics from previous architecture
# This removes topics like "order-aapl", "order-msft", etc.

KAFKA_BIN_DIR="kafka_2.13-3.9.1/bin"
BOOTSTRAP_SERVER="localhost:9092"

echo "=========================================="
echo "Deleting Old Kafka Topics"
echo "=========================================="
echo ""

# List of old topics to delete (from previous architecture)
OLD_TOPICS=(
    "order-aapl"
    "order-amzn"
    "order-amd"
    "order-adobe"
    "order-baba"
    "order-crm"
    "order-csco"
    "order-googl"
    "order-ibm"
    "order-intc"
    "order-meta"
    "order-msft"
    "order-nflx"
    "order-nvda"
    "order-orcl"
    "order-sap"
    "order-snap"
    "order-tcehy"
    "order-tsla"
    "order-twtr"
)

echo "Topics to delete:"
for topic in "${OLD_TOPICS[@]}"; do
    echo "  - $topic"
done
echo ""

# Confirm deletion
read -p "Are you sure you want to delete these topics? (yes/no): " confirm
if [ "$confirm" != "yes" ]; then
    echo "Deletion cancelled."
    exit 0
fi

echo ""
echo "Deleting topics..."
echo "-------------------"

DELETED_COUNT=0
FAILED_COUNT=0

for topic in "${OLD_TOPICS[@]}"; do
    echo -n "Deleting $topic... "
    
    # Check if topic exists before trying to delete
    if $KAFKA_BIN_DIR/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVER --list | grep -q "^${topic}$"; then
        if $KAFKA_BIN_DIR/kafka-topics.sh --delete \
            --topic "$topic" \
            --bootstrap-server $BOOTSTRAP_SERVER 2>/dev/null; then
            echo "✓ Deleted"
            ((DELETED_COUNT++))
        else
            echo "✗ Failed"
            ((FAILED_COUNT++))
        fi
    else
        echo "⊘ Not found (already deleted or never existed)"
    fi
done

echo ""
echo "=========================================="
echo "Summary:"
echo "  Deleted: $DELETED_COUNT"
echo "  Failed: $FAILED_COUNT"
echo "  Skipped: $(( ${#OLD_TOPICS[@]} - DELETED_COUNT - FAILED_COUNT ))"
echo "=========================================="
echo ""
echo "Note: Topic deletion is asynchronous in Kafka."
echo "It may take a few seconds for topics to be fully removed."
echo ""
echo "To verify, run:"
echo "  $KAFKA_BIN_DIR/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVER --list"
