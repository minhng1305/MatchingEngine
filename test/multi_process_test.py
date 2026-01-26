#!/usr/bin/env python3

"""
Multi-Server Matching Engine Performance Test
==============================================
Tests performance of the 3 matching servers (8080, 8081, 8082) and Ingress (8085).

ARCHITECTURE (per API_ROUTING_MAP.md):
- Auth: Server 1 (8080) only. Register/login once per user.
- Order submission: ALL orders → Ingress (8085) → Kafka → Matching servers consume by partition.
- Read operations: Symbol-based routing to 8080/8081/8082 (e.g. GET /api/stocks/{symbol}).

WHAT THIS TEST DOES:
1. Order submission phase: N threads, each a unique user. Auth on 8080, submit orders to
   Ingress 8085 only. Measures latency (avg, p50, p95, p99) and throughput (orders/sec).
   Uses a mix of symbols so Kafka partitions (and thus all 3 matching servers) get load.

2. Optional read phase (--read-phase): Stress GET /api/stocks/{symbol} on symbol-specific
   servers. Reports per-server latency and throughput.

IS THIS A GOOD WAY TO TEST?
- Good: Exercises real flow (Ingress → Kafka → matching engines). Multi-user concurrency.
  Latency percentiles and throughput are standard metrics. Optional read-phase stresses
  the 3 servers directly.
- Caveats: (1) No seeding—add-balance/add-stocks are disabled in backend. Orders are
  accepted; matching may fail for insufficient funds. We measure "accept" throughput.
  (2) Ensure Ingress (8085), Kafka, and all 3 servers (8080/8081/8082) are running.
"""

import argparse
import random
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from statistics import mean, median, stdev
from typing import List, Dict, Any, Optional

import requests

# ===============================
# Configuration
# ===============================

AUTH_URL = "http://localhost:8080"
INGRESS_URL = "http://localhost:8085"


@dataclass
class ServerConfig:
    """Configuration for each matching server (for read-phase routing)."""
    port: int
    base_url: str
    assigned_symbols: List[str]


SERVERS = [
    ServerConfig(8080, "http://localhost:8080", ["AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NFLX"]),
    ServerConfig(8081, "http://localhost:8081", ["NVDA", "AMD", "INTC", "IBM", "ORCL", "CSCO", "SAP"]),
    ServerConfig(8082, "http://localhost:8082", ["ADOBE", "CRM", "TWTR", "SNAP", "BABA", "TCEHY"]),
]

ALL_SYMBOLS: List[str] = []
for s in SERVERS:
    ALL_SYMBOLS.extend(s.assigned_symbols)

ORDER_SIDES = ["BUY", "SELL"]
ORDER_TYPES = ["MARKET", "LIMIT"]

# Defaults
DEFAULT_THREADS = 50
DEFAULT_ORDERS = 5000
DEFAULT_READ_REQUESTS = 0  # Set via --read-phase


def _percentile(sorted_values: List[float], p: float) -> float:
    if not sorted_values:
        return 0.0
    k = (len(sorted_values) - 1) * (p / 100.0)
    f = int(k)
    c = f + 1 if f + 1 < len(sorted_values) else f
    return sorted_values[f] + (k - f) * (sorted_values[c] - sorted_values[f])


# ===============================
# Auth
# ===============================


class AuthManager:
    """Register + login on AUTH_URL (8080). Holds token and userId for Ingress requests."""

    def __init__(self, thread_id: int):
        self.thread_id = thread_id
        self.username = f"user_t{thread_id}_{int(time.time())}"
        self.email = f"u{thread_id}_{int(time.time())}@test.local"
        self.password = "password123"
        self.token: Optional[str] = None
        self.user_id: Optional[str] = None

    def register_and_login(self) -> bool:
        try:
            r = requests.post(
                f"{AUTH_URL}/api/auth/register",
                json={"username": self.username, "email": self.email, "password": self.password},
                headers={"Content-Type": "application/json"},
                timeout=10,
            )
            if r.status_code == 200:
                data = r.json()
                self.token = data["token"]
                self.user_id = data["user"]["userId"]
                return True
            return self._login()
        except Exception as e:
            print(f"❌ [T{self.thread_id}] Auth failed: {e}")
            return False

    def _login(self) -> bool:
        try:
            r = requests.post(
                f"{AUTH_URL}/api/auth/login",
                json={"username": self.username, "password": self.password},
                headers={"Content-Type": "application/json"},
                timeout=10,
            )
            if r.status_code == 200:
                data = r.json()
                self.token = data["token"]
                self.user_id = data["user"]["userId"]
                return True
            return False
        except Exception as e:
            return False

    def headers(self) -> Dict[str, str]:
        if not self.token or not self.user_id:
            raise ValueError("Not authenticated")
        return {
            "Authorization": f"Bearer {self.token}",
            "Content-Type": "application/json",
        }


# ===============================
# Symbol → Server (for read-phase)
# ===============================


class SymbolRouter:
    def __init__(self, servers: List[ServerConfig]):
        self.symbol_to_server: Dict[str, ServerConfig] = {}
        for s in servers:
            for sym in s.assigned_symbols:
                self.symbol_to_server[sym] = s

    def get_server(self, symbol: str) -> ServerConfig:
        return self.symbol_to_server.get(symbol, SERVERS[0])


# ===============================
# Workers
# ===============================


def _order_worker(
    thread_id: int,
    num_orders: int,
    delay_min: float,
    delay_max: float,
    results_list: List[Dict],
    results_lock: threading.Lock,
) -> None:
    """Single user: auth on 8080, submit orders to Ingress 8085."""
    auth = AuthManager(thread_id)
    if not auth.register_and_login():
        print(f"❌ [T{thread_id}] Auth failed. Skipping.")
        return

    successful = 0
    failed = 0
    response_times: List[float] = []
    errors: List[Dict] = []

    for _ in range(num_orders):
        symbol = random.choice(ALL_SYMBOLS)
        side = random.choice(ORDER_SIDES)
        type_ = random.choice(ORDER_TYPES)
        price = round(random.uniform(10.0, 1000.0), 2)
        quantity = random.randint(1, 100)
        payload = {
            "userId": auth.user_id,
            "symbol": symbol,
            "side": side,
            "type": type_,
            "price": price,
            "quantity": quantity,
        }
        try:
            t0 = time.perf_counter()
            r = requests.post(
                f"{INGRESS_URL}/api/orders/submit",
                json=payload,
                headers=auth.headers(),
                timeout=10,
            )
            dur_ms = (time.perf_counter() - t0) * 1000
            if r.status_code == 200:
                successful += 1
                response_times.append(dur_ms)
            else:
                failed += 1
                errors.append({"status": r.status_code, "text": (r.text or "")[:80]})
        except Exception as e:
            failed += 1
            errors.append({"error": str(e)})
        if delay_max > 0:
            time.sleep(random.uniform(delay_min, delay_max))

    with results_lock:
        results_list.append({
            "thread_id": thread_id,
            "successful": successful,
            "failed": failed,
            "response_times": response_times,
            "errors": errors[:5],
        })
    print(f"✅ [T{thread_id}] Orders: {successful} ok, {failed} fail.")


def _read_worker(
    thread_id: int,
    num_requests: int,
    router: SymbolRouter,
    results_list: List[Dict],
    results_lock: threading.Lock,
) -> None:
    """Hit GET /api/stocks/{symbol} on symbol-specific servers. Uses same auth as order workers."""
    auth = AuthManager(thread_id)
    if not auth.register_and_login():
        return

    server_stats: Dict[str, Dict[str, Any]] = {s.base_url: {"success": 0, "failed": 0, "times": []} for s in SERVERS}
    for _ in range(num_requests):
        symbol = random.choice(ALL_SYMBOLS)
        server = router.get_server(symbol)
        url = f"{server.base_url}/api/stocks/{symbol}"
        try:
            t0 = time.perf_counter()
            r = requests.get(url, headers=auth.headers(), timeout=10)
            dur_ms = (time.perf_counter() - t0) * 1000
            if r.status_code == 200:
                server_stats[server.base_url]["success"] += 1
                server_stats[server.base_url]["times"].append(dur_ms)
            else:
                server_stats[server.base_url]["failed"] += 1
        except Exception:
            server_stats[server.base_url]["failed"] += 1

    with results_lock:
        results_list.append({"thread_id": thread_id, "server_stats": server_stats})
    print(f"✅ [T{thread_id}] Read phase done.")


# ===============================
# Reporting
# ===============================


def _report_orders(results_list: List[Dict], total_duration_sec: float) -> None:
    total_ok = sum(r["successful"] for r in results_list)
    total_fail = sum(r["failed"] for r in results_list)
    all_times: List[float] = []
    for r in results_list:
        all_times.extend(r["response_times"])

    throughput = total_ok / total_duration_sec if total_duration_sec > 0 else 0
    print("\n" + "=" * 60)
    print("📊 ORDER SUBMISSION (Ingress 8085)")
    print("=" * 60)
    print(f"⏱️  Duration:     {total_duration_sec:.2f}s")
    print(f"📈 Throughput:   {throughput:.2f} orders/sec")
    print(f"✅ Successful:   {total_ok}")
    print(f"❌ Failed:       {total_fail}")

    if all_times:
        all_times.sort()
        s = stdev(all_times) if len(all_times) >= 2 else 0.0
        print(f"📊 Latency (ms): avg={mean(all_times):.2f}  median={median(all_times):.2f}  stdev={s:.2f}")
        print(f"   p50={_percentile(all_times, 50):.2f}  p95={_percentile(all_times, 95):.2f}  p99={_percentile(all_times, 99):.2f}")
    else:
        print("📊 Latency:     no successful requests")
    print("=" * 60)


def _report_reads(results_list: List[Dict], total_duration_sec: float) -> None:
    agg: Dict[str, Dict[str, Any]] = {s.base_url: {"success": 0, "failed": 0, "times": []} for s in SERVERS}
    for r in results_list:
        for url, st in r["server_stats"].items():
            agg[url]["success"] += st["success"]
            agg[url]["failed"] += st["failed"]
            agg[url]["times"].extend(st["times"])

    total_ok = sum(agg[u]["success"] for u in agg)
    throughput = total_ok / total_duration_sec if total_duration_sec > 0 else 0
    print("\n" + "=" * 60)
    print("📊 READ PHASE (GET /api/stocks/{symbol} per server)")
    print("=" * 60)
    print(f"⏱️  Duration:     {total_duration_sec:.2f}s")
    print(f"📈 Throughput:   {throughput:.2f} req/sec (all servers)")
    for url in agg:
        st = agg[url]
        ok, fail, times = st["success"], st["failed"], st["times"]
        lat = f"avg={sum(times)/len(times):.2f}ms" if times else "n/a"
        print(f"   {url}: ok={ok} fail={fail}  {lat}")
    print("=" * 60)


# ===============================
# Main
# ===============================


def main() -> None:
    ap = argparse.ArgumentParser(description="Multi-server matching engine performance test")
    ap.add_argument("--threads", type=int, default=DEFAULT_THREADS, help="Concurrent user threads")
    ap.add_argument("--orders", type=int, default=DEFAULT_ORDERS, help="Total orders (split across threads)")
    ap.add_argument("--no-sleep", action="store_true", help="No delay between orders (max throughput)")
    ap.add_argument("--read-phase", type=int, default=0, metavar="N", help="After orders, run N read requests per thread (GET /api/stocks/{symbol})")
    args = ap.parse_args()

    num_threads = max(1, args.threads)
    total_orders = max(1, args.orders)
    orders_per_thread = total_orders // num_threads
    delay_min, delay_max = (0.0, 0.0) if args.no_sleep else (0.001, 0.01)

    print("🚀 Multi-Server Matching Engine Performance Test")
    print("=" * 60)
    print(f"  Auth:        {AUTH_URL}")
    print(f"  Ingress:     {INGRESS_URL}")
    print(f"  Threads:     {num_threads}")
    print(f"  Orders:      {total_orders} ({orders_per_thread} per thread)")
    print(f"  Delay:       {'none' if args.no_sleep else '0.001–0.01s'}")
    print(f"  Read phase:  {args.read_phase} req/thread" if args.read_phase else "  Read phase:  off")
    print("=" * 60)

    results_list: List[Dict] = []
    results_lock = threading.Lock()

    # --- Order phase ---
    t0 = time.perf_counter()
    with ThreadPoolExecutor(max_workers=num_threads) as ex:
        fs = [
            ex.submit(
                _order_worker,
                tid,
                orders_per_thread,
                delay_min,
                delay_max,
                results_list,
                results_lock,
            )
            for tid in range(1, num_threads + 1)
        ]
        for f in as_completed(fs):
            try:
                f.result()
            except Exception as e:
                print(f"❌ Thread error: {e}")
    order_duration = time.perf_counter() - t0
    _report_orders(results_list, order_duration)

    # --- Optional read phase ---
    if args.read_phase > 0:
        read_results: List[Dict] = []
        router = SymbolRouter(SERVERS)
        t0 = time.perf_counter()
        with ThreadPoolExecutor(max_workers=num_threads) as ex:
            fs = [
                ex.submit(_read_worker, tid, args.read_phase, router, read_results, results_lock)
                for tid in range(1, num_threads + 1)
            ]
            for f in as_completed(fs):
                try:
                    f.result()
                except Exception as e:
                    print(f"❌ Read thread error: {e}")
        read_duration = time.perf_counter() - t0
        _report_reads(read_results, read_duration)


if __name__ == "__main__":
    main()
