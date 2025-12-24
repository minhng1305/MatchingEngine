#!/usr/bin/env python3

"""
Multi-Server Matching Engine Stress Test
========================================
Features:
- 50 Concurrent Threads representing 50 Unique Users.
- Automatic User Registration & Authentication.
- **Data Seeding**: Automatically adds funds ($1M) and stocks (5000 qty) to each user.
- Load Balancing: Routes orders to specific servers based on symbol.
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
import traceback

# ===============================
# Configuration
# ===============================

@dataclass
class ServerConfig:
    """Configuration for each server instance"""
    port: int
    base_url: str
    assigned_symbols: List[str]

# Server configurations
SERVERS = [
    ServerConfig(
        port=8080,
        base_url="http://localhost:8080",
        assigned_symbols=["AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NFLX"]
    ),
    ServerConfig(
        port=8081,
        base_url="http://localhost:8081",
        assigned_symbols=["NVDA", "AMD", "INTC", "IBM", "ORCL", "CSCO", "SAP"]
    ),
    ServerConfig(
        port=8082,
        base_url="http://localhost:8082",
        assigned_symbols=["ADOBE", "CRM", "TWTR", "SNAP", "BABA", "TCEHY"]
    )
]

# All possible symbols
ALL_SYMBOLS = []
for s in SERVERS:
    ALL_SYMBOLS.extend(s.assigned_symbols)

# Order configuration
ORDER_SIDES = ["BUY", "SELL"]
ORDER_TYPES = ["MARKET", "LIMIT"]

# Test configuration
NUM_THREADS = 50
TOTAL_ORDERS = 5000
ORDERS_PER_THREAD = TOTAL_ORDERS // NUM_THREADS

# ===============================
# Helper Classes
# ===============================

class AuthenticationManager:
    """Manages JWT authentication for a SINGLE user across all servers"""

    def __init__(self, thread_id):
        self.tokens = {}    # server_url -> token
        self.user_ids = {}  # server_url -> user_id
        self.thread_id = thread_id
        # Unique user credentials for this thread
        self.username = f"user_thread_{thread_id}_{int(time.time())}"
        self.email = f"user_{thread_id}_{int(time.time())}@example.com"
        self.password = "password123"

    def register_and_login(self, server_config: ServerConfig) -> bool:
        """Register and login to get JWT token for a server"""
        try:
            # Register user
            register_url = f"{server_config.base_url}/api/auth/register"
            register_data = {
                "username": self.username,
                "email": self.email,
                "password": self.password
            }

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
                return True
            else:
                # If registration fails, try login (in case user persists from previous run)
                return self._login(server_config)

        except Exception as e:
            print(f"❌ [Thread {self.thread_id}] Auth failed for {server_config.base_url}: {e}")
            return False

    def _login(self, server_config: ServerConfig) -> bool:
        """Login with existing credentials"""
        try:
            login_url = f"{server_config.base_url}/api/auth/login"
            login_data = {
                "username": self.username,
                "password": self.password
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
                return True
            else:
                print(f"❌ [Thread {self.thread_id}] Login failed: {response.status_code}")
                return False

        except Exception as e:
            print(f"❌ [Thread {self.thread_id}] Login exception: {e}")
            return False

    def get_auth_headers(self, server_url: str) -> Dict[str, str]:
        token = self.tokens.get(server_url)
        if not token:
            raise ValueError(f"No authentication token for {server_url}")
        return {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }

    def get_user_id(self, server_url: str) -> str:
        user_id = self.user_ids.get(server_url)
        if not user_id:
            raise ValueError(f"No user ID for {server_url}")
        return user_id

class LoadBalancer:
    """Routes orders to appropriate servers based on symbol"""
    def __init__(self, servers: List[ServerConfig]):
        self.servers = servers
        self.symbol_to_server = {}
        
        # Map symbols to servers
        for server in servers:
            for symbol in server.assigned_symbols:
                self.symbol_to_server[symbol] = server

    def get_server_for_symbol(self, symbol: str) -> ServerConfig:
        return self.symbol_to_server.get(symbol, self.servers[0])  # Default to first if not found

# ===============================
# Data Seeding Helper
# ===============================

def seed_user_account(auth_manager: AuthenticationManager):
    """
    Grants the user massive balance and a portfolio of stocks
    so they can actually trade.
    """
    initial_cash = 1_000_000.00  # Give them $1 Million
    initial_stock_qty = 5000     # Give them 5000 shares of each stock
    
    # Iterate through all servers to seed data on every shard/server
    # Note: In a real distributed system, user data might be centralized.
    # Here we assume we just need to hit one valid endpoint or all if sharded.
    # We will try to seed via the first successful connection.
    
    seeded_any = False
    
    for server in SERVERS:
        try:
            base_url = server.base_url
            if base_url not in auth_manager.tokens:
                continue

            user_id = auth_manager.get_user_id(base_url)
            headers = auth_manager.get_auth_headers(base_url)
            
            # ---------------------------------------------------
            # 1. SEED CASH (for Buy Orders)
            # ---------------------------------------------------
            deposit_payload = {
                "userId": user_id,
                "amount": initial_cash
            }
            
            resp_cash = requests.post(
                f"{base_url}/api/user/add-balance", 
                json=deposit_payload, 
                headers=headers,
                timeout=5
            )
            
            if resp_cash.status_code == 200:
                 seeded_any = True
            
            # ---------------------------------------------------
            # 2. SEED STOCKS (for Sell Orders)
            # ---------------------------------------------------
            # We give the user stocks for every symbol managed by THIS server
            for symbol in server.assigned_symbols:
                portfolio_payload = {
                    "userId": user_id,
                    "symbol": symbol,
                    "quantity": initial_stock_qty
                }
                
                requests.post(
                    f"{base_url}/api/user/add-stocks", 
                    json=portfolio_payload, 
                    headers=headers,
                    timeout=5
                )
                
            # If your user DB is shared, seeding once might be enough. 
            # If sharded, you might need to seed on all. 
            # Assuming shared DB for Auth/User, we assume cash is global, but stocks might be per engine?
            # Safe bet: Seed stocks on the server that manages them.
            
        except Exception as e:
            print(f"⚠️ [Thread {auth_manager.thread_id}] Failed to seed on {server.base_url}: {e}")

    if seeded_any:
        pass # print(f"💰 [Thread {auth_manager.thread_id}] Seeded successfully.")

# ===============================
# Worker Thread
# ===============================

def worker_thread(thread_id: int, num_orders: int, load_balancer: LoadBalancer, results_queue: List[Dict]):
    """
    Worker thread that simulates a SINGLE unique user.
    1. Authenticates as a new user.
    2. Seeds account with funds/stocks.
    3. Sends orders.
    """
    
    # 1. Initialize per-thread Authentication Manager (Unique User)
    auth_manager = AuthenticationManager(thread_id)
    
    # Authenticate with ALL servers
    auth_success_count = 0
    for server in SERVERS:
        if auth_manager.register_and_login(server):
            auth_success_count += 1
            
    if auth_success_count == 0:
        print(f"❌ [Thread {thread_id}] Failed to auth with ANY server. Aborting.")
        return

    # 2. Seed Data (Money & Stocks)
    seed_user_account(auth_manager)

    # 3. Prepare Stats
    stats = {
        "successful": 0,
        "failed": 0,
        "response_times": [],
        "server_stats": {s.base_url: {"success": 0, "failed": 0} for s in SERVERS},
        "errors": []
    }

    # 4. Send Orders
    for i in range(num_orders):
        try:
            # Generate random order details
            symbol = random.choice(ALL_SYMBOLS)
            side = random.choice(ORDER_SIDES)
            type_ = random.choice(ORDER_TYPES)
            price = round(random.uniform(10.0, 1000.0), 2)
            quantity = random.randint(1, 100)

            # Route to correct server
            target_server = load_balancer.get_server_for_symbol(symbol)
            server_url = target_server.base_url
            
            # Prepare payload
            try:
                user_id = auth_manager.get_user_id(server_url)
                headers = auth_manager.get_auth_headers(server_url)
            except ValueError:
                stats["failed"] += 1
                stats["server_stats"][server_url]["failed"] += 1
                continue

            order_payload = {
                "userId": user_id,
                "symbol": symbol,
                "price": price,
                "quantity": quantity,
                "side": side,
                "type": type_
            }

            # Measure request latency
            req_start = time.time()
            response = requests.post(
                f"{server_url}/api/orders/submit",
                json=order_payload,
                headers=headers,
                timeout=5
            )
            req_end = time.time()
            duration_ms = (req_end - req_start) * 1000

            if response.status_code == 200:
                stats["successful"] += 1
                stats["response_times"].append(duration_ms)
                stats["server_stats"][server_url]["success"] += 1
            else:
                stats["failed"] += 1
                stats["server_stats"][server_url]["failed"] += 1
                stats["errors"].append({
                    "thread_id": thread_id,
                    "status": response.status_code,
                    "msg": response.text[:100]
                })

        except Exception as e:
            stats["failed"] += 1
            stats["errors"].append({"thread_id": thread_id, "error": str(e)})
            
        time.sleep(random.uniform(0.001, 0.01))

    results_queue.append(stats)
    print(f"✅ [Thread {thread_id}] Finished {num_orders} orders.")

# ===============================
# Main Execution
# ===============================

def main():
    print("🚀 Multi-Server Matching Engine Stress Test (Unique Users Mode)")
    print("=" * 60)
    print(f"Configuration:")
    print(f" • Threads (Unique Users): {NUM_THREADS}")
    print(f" • Orders per Thread: {ORDERS_PER_THREAD}")
    print(f" • Total Orders: {TOTAL_ORDERS}")
    
    load_balancer = LoadBalancer(SERVERS)
    results_queue = []
    
    start_time = time.time()

    print(f"\n🔥 Starting {NUM_THREADS} threads...")

    with ThreadPoolExecutor(max_workers=NUM_THREADS) as executor:
        futures = []
        for thread_id in range(NUM_THREADS):
            future = executor.submit(
                worker_thread,
                thread_id + 1,
                ORDERS_PER_THREAD,
                load_balancer,
                results_queue
            )
            futures.append(future)

        for future in as_completed(futures):
            try:
                future.result()
            except Exception as e:
                print(f"❌ Thread exception: {e}")

    end_time = time.time()
    total_duration = end_time - start_time

    # --- RESULTS REPORTING ---
    print("\n" + "=" * 60)
    print("📊 STRESS TEST RESULTS")
    print("=" * 60)

    total_successful = sum(r["successful"] for r in results_queue)
    total_failed = sum(r["failed"] for r in results_queue)
    all_response_times = []
    
    for r in results_queue:
        all_response_times.extend(r["response_times"])

    if all_response_times:
        avg_response = mean(all_response_times)
    else:
        avg_response = 0

    throughput = total_successful / total_duration if total_duration > 0 else 0
    
    print(f"⏱️ Total Duration: {total_duration:.2f}s")
    print(f"📈 Throughput: {throughput:.2f} orders/sec")
    print(f"✅ Successful: {total_successful}")
    print(f"❌ Failed: {total_failed}")
    print(f"📊 Avg Latency: {avg_response:.2f}ms")
    print("=" * 60)

if __name__ == "__main__":
    main()
