import requests
import json
import uuid
import random
import time
from datetime import datetime, timedelta
from statistics import mean, median, stdev
import multiprocessing

# --- Configuration ---

# API endpoint
API_URL = "http://localhost:8080/api/orders"

# List of possible stock symbols
SYMBOLS = ["AAPL", "MSFT", "GOOGL", "AMZN"]

# List of possible user IDs
USER_IDS = ["u1", "u2", "u3", "u4", "u5", "m1", "m2", "m3", "m4", "m5"]

# Order sides
SIDES = ["BUY", "SELL"]

# --- Helper Function to Generate Orders ---

def generate_order():
    """Generate a random MARKET order"""
    symbol = random.choice(SYMBOLS)
    price = round(random.uniform(50, 500), 2)
    quantity = random.randint(10, 1000)
    side = random.choice(SIDES)
    
    # Generate timestamp within the last 7 days
    random_days = random.randint(0, 7)
    random_hours = random.randint(0, 23)
    random_minutes = random.randint(0, 59)
    order_time = datetime.now() - timedelta(days=random_days, hours=random_hours, minutes=random_minutes)
    timestamp = order_time.strftime("%Y-%m-%dT%H:%M:%S.000+00:00")
    
    # Create order data
    order = {
        "orderId": str(uuid.uuid4()),
        "userId": random.choice(USER_IDS),
        "symbol": symbol,
        "price": price,
        "originalQuantity": quantity,
        "currentQuantity": quantity,
        "side": side,
        "type": "MARKET",
        "limitPrice": 0,
        "orderTimestamp": timestamp,
        "status": "PENDING"
    }
    return order

# --- Worker Function for Each Process ---

def worker_submit_orders(num_orders, process_id, results_queue):
    """
    This function is executed by each process.
    It submits a specified number of orders and puts the results into a queue.
    """
    print(f"[Process {process_id}] Starting, will send {num_orders} orders.")
    
    successful = 0
    failed = 0
    response_times = []
    
    for i in range(num_orders):
        order = generate_order()
        
        try:
            headers = {'Content-Type': 'application/json'}
            start_time = time.time()
            
            response = requests.post(API_URL, data=json.dumps(order), headers=headers)
            
            end_time = time.time()
            response_time_ms = (end_time - start_time) * 1000
            response_times.append(response_time_ms)
            
            if response.status_code in [200, 201]:
                successful += 1
            else:
                failed += 1
                # Optional: Log detailed failures for debugging
                # print(f"[Process {process_id}] Order {i+1} failed with status {response.status_code}: {response.text}")
                
        except Exception as e:
            failed += 1
            # Optional: Log detailed exceptions
            # print(f"[Process {process_id}] Order {i+1} failed with exception: {str(e)}")

    print(f"[Process {process_id}] Finished. Success: {successful}, Failed: {failed}.")
    
    # Put the collected results for this process into the shared queue
    results_queue.put({
        "successful": successful,
        "failed": failed,
        "response_times": response_times
    })

# --- Main Orchestrator ---

if __name__ == "__main__":
    # --- Test Parameters ---
    NUM_PROCESSES = 50
    ORDERS_PER_PROCESS = 100
    
    total_orders_to_send = NUM_PROCESSES * ORDERS_PER_PROCESS
    print(f"Starting concurrency test with {NUM_PROCESSES} processes, each sending {ORDERS_PER_PROCESS} orders.")
    print(f"Total orders to be sent: {total_orders_to_send}")
    print("-" * 40)

    # A multiprocessing.Queue is used to safely collect results from all child processes.
    results_queue = multiprocessing.Queue()
    processes = []
    
    # Record the start time for the entire test
    global_start_time = time.time()

    # Create and start all the processes
    for i in range(NUM_PROCESSES):
        process_id = i + 1
        process = multiprocessing.Process(
            target=worker_submit_orders, 
            args=(ORDERS_PER_PROCESS, process_id, results_queue)
        )
        processes.append(process)
        process.start()

    # Wait for all processes to complete their execution
    for process in processes:
        process.join()

    # Record the end time
    global_end_time = time.time()
    total_duration = global_end_time - global_start_time
    
    print("-" * 40)
    print("All processes have finished. Aggregating results...")

    # --- Aggregate Results from the Queue ---
    total_successful = 0
    total_failed = 0
    all_response_times = []

    while not results_queue.empty():
        result = results_queue.get()
        total_successful += result['successful']
        total_failed += result['failed']
        all_response_times.extend(result['response_times'])

    # --- Calculate and Display Final Statistics ---
    if all_response_times:
        avg_response = mean(all_response_times)
        median_response = median(all_response_times)
        min_response = min(all_response_times)
        max_response = max(all_response_times)
        
        std_dev = stdev(all_response_times) if len(all_response_times) > 1 else 0
        
        # Throughput is a key metric for a stress test
        throughput = total_orders_to_send / total_duration if total_duration > 0 else 0
    else:
        avg_response = median_response = min_response = max_response = std_dev = throughput = 0

    print("\n" + "="*20 + " Overall Performance Summary " + "="*20)
    print(f"Total time for all processes: {total_duration:.2f} seconds")
    print(f"Total orders sent: {total_orders_to_send}")
    print(f"Overall throughput (orders/sec): {throughput:.2f}")
    print("-" * 65)
    print(f"Average response time: {avg_response:.2f}ms")
    print(f"Median response time: {median_response:.2f}ms")
    print(f"Min response time: {min_response:.2f}ms")
    print(f"Max response time: {max_response:.2f}ms")
    print(f"Standard deviation of response times: {std_dev:.2f}ms")
    print("-" * 65)
    print(f"Results Summary: {total_successful} orders submitted successfully, {total_failed} failed")
    print("=" * 65)