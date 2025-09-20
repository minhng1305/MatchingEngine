import requests
import json
import uuid
import random
import time
from datetime import datetime, timedelta
from statistics import mean, median, stdev

# API endpoint
url = "http://localhost:8080/api/orders/submitOrder"

# List of possible stock symbols
symbols = ["AAPL", "MSFT", "GOOGL", "AMZN"]

# List of possible user IDs
user_ids = ["u1", "u2", "u3", "u4", "u5", "m1", "m2", "m3", "m4", "m5"]

# Order sides
sides = ["BUY", "SELL"]

def generate_order():
    """Generate a random MARKET order"""
    symbol = random.choice(symbols)
    price = round(random.uniform(50, 500), 2)
    quantity = random.randint(10, 1000)
    side = random.choice(sides)
    
    # Generate timestamp within the last 7 days
    random_days = random.randint(0, 7)
    random_hours = random.randint(0, 23)
    random_minutes = random.randint(0, 59)
    order_time = datetime.now() - timedelta(days=random_days, hours=random_hours, minutes=random_minutes)
    timestamp = order_time.strftime("%Y-%m-%dT%H:%M:%S.000+00:00")
    
    # Create order data - include orderId as the server expects it
    order = {
        "orderId": str(uuid.uuid4()),  # Generate UUID on the client side
        "userId": random.choice(user_ids),
        "symbol": symbol,
        "price": price,
        "originalQuantity": quantity,
        "currentQuantity": quantity,
        "side": side,
        "type": "MARKET",
        "limitPrice": 0,  # Always 0 for MARKET orders
        "orderTimestamp": timestamp,
        "status": "PENDING"  # Default to PENDING for new orders
    }
    
    return order

def submit_orders(num_orders):
    """Submit a specified number of orders to the API and measure time"""
    successful = 0
    failed = 0
    response_times = []
    
    # Start timing the entire batch
    start_time_total = time.time()
    
    for i in range(num_orders):
        order = generate_order()
        
        try:
            headers = {'Content-Type': 'application/json'}
            
            # Start timing this individual request
            start_time = time.time()
            
            response = requests.post(url, data=json.dumps(order), headers=headers)
            
            # Calculate and store response time for this request
            end_time = time.time()
            response_time = (end_time - start_time) * 1000  # Convert to milliseconds
            response_times.append(response_time)
            
            if response.status_code == 200 or response.status_code == 201:
                successful += 1
                print(f"Order {i+1} submitted successfully: {order['orderId']} - {response_time:.2f}ms")
            else:
                failed += 1
                print(f"Order {i+1} failed with status code {response.status_code}: {response.text} - {response_time:.2f}ms")
                
        except Exception as e:
            failed += 1
            print(f"Order {i+1} failed with exception: {str(e)}")
    
    # Calculate total time for the batch
    end_time_total = time.time()
    total_time = end_time_total - start_time_total

    # Number of trades persecond
    trades_per_second = num_orders / total_time if total_time > 0 else 0
    
    # Calculate statistics on response times
    if response_times:
        avg_response = mean(response_times)
        median_response = median(response_times)
        min_response = min(response_times)
        max_response = max(response_times)
        
        # Calculate standard deviation if we have more than one sample
        if len(response_times) > 1:
            std_dev = stdev(response_times)
        else:
            std_dev = 0
            
        throughput = num_orders / total_time if total_time > 0 else 0
    else:
        avg_response = median_response = min_response = max_response = std_dev = throughput = 0
    
    # Print summary with timing information
    print(f"\nPerformance Summary:")
    print(f"Total time: {total_time:.2f} seconds")
    print(f"Orders per second: {throughput:.2f}")
    print(f"Average response time: {avg_response:.2f}ms")
    print(f"Median response time: {median_response:.2f}ms")
    print(f"Min response time: {min_response:.2f}ms")
    print(f"Max response time: {max_response:.2f}ms")
    print(f"Standard deviation: {std_dev:.2f}ms")
    print(f"Trades per second: {trades_per_second:.2f}")
    print(f"\nResults Summary: {successful} orders submitted successfully, {failed} failed")

if __name__ == "__main__":
    num_orders = 100
    
    print(f"Starting performance test with {num_orders} orders...")
    submit_orders(num_orders)