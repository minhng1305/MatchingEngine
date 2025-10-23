#!/usr/bin/env python3

"""
Multi-Server Matching Engine Stress Test
========================================

This script performs comprehensive stress testing against 3 parallel matching engine servers:
- Server 1: localhost:8080 (handles symbols: AAPL, MSFT, GOOGL, AMZN, TSLA, etc.)
- Server 2: localhost:8081 (handles symbols: CVX, PFE, KO, PEP, COST, etc.) 
- Server 3: localhost:8082 (handles symbols: BKNG, GILD, ADP, MDLZ, REGN, etc.)

Features:
- JWT Authentication
- Load balancing across 3 servers
- 50 concurrent threads
- 5000 total orders
- Comprehensive performance metrics
- Server-specific routing based on symbols
"""

import requests
import json
import uuid
import random
import time
import threading
from datetime import datetime, timedelta
from statistics import mean, median, stdev
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from typing import List, Dict, Any
import sys

# ===============================
# Configuration
# ===============================

@dataclass
class ServerConfig:
    """Configuration for each server instance"""
    port: int
    base_url: str
    assigned_symbols: List[str]

# Server configurations based on your setup
SERVERS = [
    ServerConfig(
        port=8080,
        base_url="http://localhost:8080",
        assigned_symbols=["AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NFLX"]
    ),
    ServerConfig(
        port=8081, 
        base_url="http://localhost:8081",
        assigned_symbols=["NVDA", "AMD", "INTC", "IBM", "ORCL", "CSCO", "SAP"]  # Add more symbols as per your server2 config
    ),
    ServerConfig(
        port=8082,
        base_url="http://localhost:8082", 
        assigned_symbols=["ADOBE", "CRM", "TWTR", "SNAP", "BABA", "TCEHY"]  # Add more symbols as per your server3 config
    )
]

# All possible symbols from your Stock enum
ALL_SYMBOLS = ["AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NFLX",
               "NVDA", "AMD", "INTC", "IBM", "ORCL", "CSCO", "SAP",
               "ADOBE", "CRM", "TWTR", "SNAP", "BABA", "TCEHY"]

# Order configuration
ORDER_SIDES = ["BUY", "SELL"]
ORDER_TYPES = ["MARKET", "LIMIT"]

# Test configuration
NUM_THREADS = 50
TOTAL_ORDERS = 5000
ORDERS_PER_THREAD = TOTAL_ORDERS // NUM_THREADS

# Authentication configuration
TEST_USER = {
    "username": f"testuser_{int(time.time())}",
    "email": f"test_{int(time.time())}@example.com", 
    "password": "testpassword123"
}

# ===============================
# Helper Classes
# ===============================

class AuthenticationManager:
    """Manages JWT authentication across all servers"""
    
    def __init__(self):
        self.tokens = {}  # server_url -> token
        self.user_ids = {}  # server_url -> user_id
        
    def register_and_login(self, server_config: ServerConfig) -> bool:
        """Register and login to get JWT token for a server"""
        try:
            # Register user
            register_url = f"{server_config.base_url}/api/auth/register"
            register_data = TEST_USER.copy()
            
            response = requests.post(
                register_url,
                json=register_data,
                headers={"Content-Type": "application/json"},
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                self.tokens[server_config.base_url] = data["token"]
                self.user_ids[server_config.base_url] = data["user"]["userId"]
                print(f"✅ Successfully authenticated with {server_config.base_url}")
                return True
            else:
                # Try login if registration failed (user might already exist)
                return self._login(server_config)
                
        except Exception as e:
            print(f"❌ Authentication failed for {server_config.base_url}: {e}")
            return False
    
    def _login(self, server_config: ServerConfig) -> bool:
        """Login with existing credentials"""
        try:
            login_url = f"{server_config.base_url}/api/auth/login"
            login_data = {
                "username": TEST_USER["username"],
                "password": TEST_USER["password"]
            }
            
            response = requests.post(
                login_url,
                json=login_data,
                headers={"Content-Type": "application/json"},
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                self.tokens[server_config.base_url] = data["token"]
                self.user_ids[server_config.base_url] = data["user"]["userId"]
                print(f"✅ Successfully logged in to {server_config.base_url}")
                return True
            else:
                print(f"❌ Login failed for {server_config.base_url}: {response.status_code}")
                return False
                
        except Exception as e:
            print(f"❌ Login failed for {server_config.base_url}: {e}")
            return False
    
    def get_auth_headers(self, server_url: str) -> Dict[str, str]:
        """Get authentication headers for a server"""
        token = self.tokens.get(server_url)
        if not token:
            raise ValueError(f"No authentication token for {server_url}")
        
        return {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }
    
    def get_user_id(self, server_url: str) -> str:
        """Get user ID for a server"""
        user_id = self.user_ids.get(server_url)
        if not user_id:
            raise ValueError(f"No user ID for {server_url}")
        return user_id

class LoadBalancer:
    """Routes orders to appropriate servers based on symbol"""
    
    def __init__(self, servers: List[ServerConfig]):
        self.servers = servers
        self.symbol_to_server = {}
        
        # Build symbol routing table
        for server in servers:
            for symbol in server.assigned_symbols:
                self.symbol_to_server[symbol] = server
        
        print("📊 Symbol-to-Server Routing:")
        for symbol, server in self.symbol_to_server.items():
            print(f"  {symbol} -> {server.base_url}")
    
    def get_server_for_symbol(self, symbol: str) -> ServerConfig:
        """Get the appropriate server for a symbol"""
        server = self.symbol_to_server.get(symbol)
        if not server:
            # Fallback to first server if symbol not found
            print(f"⚠️  Symbol {symbol} not found in routing table, using fallback server")
            return self.servers[0]
        return server

# ===============================
# Order Generation
# ===============================

def generate_order(user_id: str) -> Dict[str, Any]:
    """Generate a random order"""
    symbol = random.choice(ALL_SYMBOLS)
    side = random.choice(ORDER_SIDES)
    order_type = random.choice(ORDER_TYPES)
    price = round(random.uniform(50, 500), 2)
    quantity = random.randint(10, 1000)
    
    order = {
        "symbol": symbol,
        "side": side,
        "type": order_type,
        "price": price,
        "quantity": quantity,
        "userId": user_id
    }
    
    return order

# ===============================
# Worker Functions
# ===============================

def worker_thread(
    thread_id: int,
    orders_count: int,
    auth_manager: AuthenticationManager,
    load_balancer: LoadBalancer,
    results_queue: List[Dict]
) -> None:
    """Worker thread that submits orders"""
    
    print(f"[Thread {thread_id}] Starting, will send {orders_count} orders")
    
    successful = 0
    failed = 0
    response_times = []
    server_stats = {server.base_url: {"success": 0, "failed": 0} for server in SERVERS}
    errors = []
    
    for i in range(orders_count):
        try:
            # Generate order
            # Use first server's user_id (all servers should have same user after registration)
            user_id = auth_manager.get_user_id(SERVERS[0].base_url)
            order = generate_order(user_id)
            
            # Route to appropriate server
            target_server = load_balancer.get_server_for_symbol(order["symbol"])
            submit_url = f"{target_server.base_url}/api/orders/submit"
            
            # Get auth headers
            headers = auth_manager.get_auth_headers(target_server.base_url)
            
            # Submit order
            start_time = time.time()
            response = requests.post(
                submit_url,
                json=order,
                headers=headers,
                timeout=30  # Increased timeout for stress test
            )
            end_time = time.time()
            
            response_time_ms = (end_time - start_time) * 1000
            response_times.append(response_time_ms)
            
            if response.status_code in [200, 201]:
                successful += 1
                server_stats[target_server.base_url]["success"] += 1
            else:
                failed += 1
                server_stats[target_server.base_url]["failed"] += 1
                errors.append({
                    "thread_id": thread_id,
                    "order_num": i + 1,
                    "status_code": response.status_code,
                    "server": target_server.base_url,
                    "symbol": order["symbol"],
                    "error": response.text[:200]  # First 200 chars of error
                })
                
        except Exception as e:
            failed += 1
            errors.append({
                "thread_id": thread_id,
                "order_num": i + 1,
                "exception": str(e),
                "server": target_server.base_url if 'target_server' in locals() else "unknown"
            })
    
    print(f"[Thread {thread_id}] Completed. Success: {successful}, Failed: {failed}")
    
    # Thread-safe results collection
    results_queue.append({
        "thread_id": thread_id,
        "successful": successful,
        "failed": failed,
        "response_times": response_times,
        "server_stats": server_stats,
        "errors": errors
    })

# ===============================
# Main Test Orchestrator
# ===============================

def main():
    """Main test execution function"""
    
    print("🚀 Multi-Server Matching Engine Stress Test")
    print("=" * 60)
    print(f"Configuration:")
    print(f"  • Threads: {NUM_THREADS}")
    print(f"  • Total Orders: {TOTAL_ORDERS}")
    print(f"  • Orders per Thread: {ORDERS_PER_THREAD}")
    print(f"  • Servers: {len(SERVERS)}")
    for server in SERVERS:
        print(f"    - {server.base_url} ({len(server.assigned_symbols)} symbols)")
    print()
    
    # Initialize components
    auth_manager = AuthenticationManager()
    load_balancer = LoadBalancer(SERVERS)
    
    # Step 1: Authenticate with all servers
    print("🔐 Authenticating with all servers...")
    
    authentication_failed = False
    for server in SERVERS:
        if not auth_manager.register_and_login(server):
            print(f"❌ Failed to authenticate with {server.base_url}")
            authentication_failed = True
    
    if authentication_failed:
        print("❌ Authentication failed for some servers. Exiting.")
        sys.exit(1)
    
    print(f"✅ Authentication successful for all {len(SERVERS)} servers\n")
    
    # Step 2: Start stress test
    print("🔥 Starting stress test...")
    results_queue = []  # Thread-safe list for results
    
    start_time = time.time()
    
    # Use ThreadPoolExecutor for better thread management
    with ThreadPoolExecutor(max_workers=NUM_THREADS) as executor:
        futures = []
        
        for thread_id in range(NUM_THREADS):
            future = executor.submit(
                worker_thread,
                thread_id + 1,
                ORDERS_PER_THREAD,
                auth_manager,
                load_balancer,
                results_queue
            )
            futures.append(future)
        
        # Wait for all threads to complete
        for future in as_completed(futures):
            try:
                future.result()  # This will raise any exceptions that occurred
            except Exception as e:
                print(f"❌ Thread failed with exception: {e}")
    
    end_time = time.time()
    total_duration = end_time - start_time
    
    # Step 3: Aggregate and display results
    print("\n" + "=" * 60)
    print("📊 STRESS TEST RESULTS")
    print("=" * 60)
    
    # Aggregate results
    total_successful = sum(result["successful"] for result in results_queue)
    total_failed = sum(result["failed"] for result in results_queue)
    all_response_times = []
    server_aggregate = {server.base_url: {"success": 0, "failed": 0} for server in SERVERS}
    all_errors = []
    
    for result in results_queue:
        all_response_times.extend(result["response_times"])
        all_errors.extend(result["errors"])
        
        for server_url, stats in result["server_stats"].items():
            server_aggregate[server_url]["success"] += stats["success"]
            server_aggregate[server_url]["failed"] += stats["failed"]
    
    # Calculate performance metrics
    if all_response_times:
        avg_response = mean(all_response_times)
        median_response = median(all_response_times)
        min_response = min(all_response_times)
        max_response = max(all_response_times)
        std_dev = stdev(all_response_times) if len(all_response_times) > 1 else 0
        
        # Percentiles
        sorted_times = sorted(all_response_times)
        p95 = sorted_times[int(0.95 * len(sorted_times))] if sorted_times else 0
        p99 = sorted_times[int(0.99 * len(sorted_times))] if sorted_times else 0
    else:
        avg_response = median_response = min_response = max_response = std_dev = p95 = p99 = 0
    
    throughput = TOTAL_ORDERS / total_duration if total_duration > 0 else 0
    success_rate = (total_successful / TOTAL_ORDERS) * 100 if TOTAL_ORDERS > 0 else 0
    
    # Display overall results
    print(f"⏱️  Total Duration: {total_duration:.2f} seconds")
    print(f"📈 Throughput: {throughput:.2f} orders/second")
    print(f"✅ Success Rate: {success_rate:.1f}% ({total_successful}/{TOTAL_ORDERS})")
    print(f"❌ Failed Orders: {total_failed}")
    
    print(f"\n📊 Response Time Statistics:")
    print(f"  Average: {avg_response:.2f}ms")
    print(f"  Median:  {median_response:.2f}ms")
    print(f"  Min:     {min_response:.2f}ms")
    print(f"  Max:     {max_response:.2f}ms")
    print(f"  95th %%:  {p95:.2f}ms")
    print(f"  99th %%:  {p99:.2f}ms")
    print(f"  Std Dev: {std_dev:.2f}ms")
    
    print(f"\n🏗️  Per-Server Statistics:")
    for server_url, stats in server_aggregate.items():
        total_server_orders = stats["success"] + stats["failed"]
        server_success_rate = (stats["success"] / total_server_orders * 100) if total_server_orders > 0 else 0
        print(f"  {server_url}:")
        print(f"    Success: {stats['success']} ({server_success_rate:.1f}%)")
        print(f"    Failed:  {stats['failed']}")
    
    # Display sample errors if any
    if all_errors and len(all_errors) > 0:
        print(f"\n❗ Sample Errors (showing first 5 of {len(all_errors)}):")
        for error in all_errors[:5]:
            print(f"  Thread {error.get('thread_id', '?')}: {error}")
    
    # Performance assessment
    print(f"\n🎯 Performance Assessment:")
    if success_rate >= 95:
        print("  🟢 Excellent: >95% success rate")
    elif success_rate >= 90:
        print("  🟡 Good: 90-95% success rate")  
    elif success_rate >= 80:
        print("  🟠 Fair: 80-90% success rate")
    else:
        print("  🔴 Poor: <80% success rate")
    
    if avg_response <= 100:
        print("  🟢 Excellent response time: <100ms average")
    elif avg_response <= 500:
        print("  🟡 Good response time: 100-500ms average")
    elif avg_response <= 1000:
        print("  🟠 Fair response time: 500-1000ms average") 
    else:
        print("  🔴 Poor response time: >1000ms average")
    
    if throughput >= 100:
        print("  🟢 Excellent throughput: >100 orders/second")
    elif throughput >= 50:
        print("  🟡 Good throughput: 50-100 orders/second")
    elif throughput >= 20:
        print("  🟠 Fair throughput: 20-50 orders/second")
    else:
        print("  🔴 Poor throughput: <20 orders/second")
    
    print("=" * 60)
    print("✅ Stress test completed!")

# ===============================
# Entry Point
# ===============================

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n⚠️  Test interrupted by user")
    except Exception as e:
        print(f"\n❌ Test failed with error: {e}")
        import traceback
        traceback.print_exc()
