#!/usr/bin/env python3

"""
Multi-Server Matching Engine Performance Test
==============================================
Tests performance of the 3 matching servers (8080, 8081, 8082) and Ingress (8085).

ARCHITECTURE (per API_ROUTING_MAP.md):
- Auth: Server 1 (8080) only. Register/login once per user.
- Order submission: ALL orders -> Ingress (8085) -> Kafka -> Matching servers consume by partition.
- Read operations: Symbol-based routing to 8080/8081/8082 (e.g. GET /api/stocks/{symbol}).

TEST SCENARIOS:
1. Quick smoke test (5 threads, 50 orders):
     python multi_process_test.py --threads 5 --orders 50

2. Full load test with reads and health monitoring:
     python multi_process_test.py --threads 20 --orders 1000 --read-phase 100 --monitor

3. Max throughput (no delay between orders):
     python multi_process_test.py --threads 50 --orders 5000 --no-sleep

4. High-concurrency stress test (100 threads x 200 orders = 20,000 total):
     python multi_process_test.py --threads 100 --orders 20000 --no-sleep --read-phase 50 --monitor

WHAT THIS TEST DOES:
1. Pre-flight health check: Verifies all servers are reachable.
2. Order submission phase: N threads, each a unique user. Auth on 8080, submit orders to
   Ingress 8085 only. Measures latency (avg, p50, p95, p99) and throughput (orders/sec).
   Uses a mix of symbols so Kafka partitions (and thus all 3 matching servers) get load.
3. Optional read phase (--read-phase): Stress GET /api/stocks/{symbol} on symbol-specific
   servers. Reports per-server latency and throughput.
4. Optional health monitor (--monitor): Polls /api/health on all servers during the test
   to detect degradation under load.

CAVEATS:
- No seeding -- add-balance/add-stocks are disabled in backend. Orders are accepted;
  matching may fail for insufficient funds. We measure "accept" throughput.
- Ensure Ingress (8085), Kafka, and all 3 servers (8080/8081/8082) are running.
"""

import argparse
import random
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from statistics import mean, median, stdev
from typing import List, Dict, Any, Optional

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

# ===============================
# Configuration
# ===============================

AUTH_URL = "http://localhost:8080"
INGRESS_URL = "http://localhost:8085"

DEFAULT_THREADS = 50
DEFAULT_ORDERS = 5000
DEFAULT_TIMEOUT = 10


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


def _percentile(sorted_values: List[float], p: float) -> float:
    if not sorted_values:
        return 0.0
    k = (len(sorted_values) - 1) * (p / 100.0)
    f = int(k)
    c = f + 1 if f + 1 < len(sorted_values) else f
    return sorted_values[f] + (k - f) * (sorted_values[c] - sorted_values[f])


def _make_session(retries: int = 2, backoff: float = 0.1) -> requests.Session:
    """Create a requests session with connection pooling and retry logic."""
    session = requests.Session()
    retry_strategy = Retry(
        total=retries,
        backoff_factor=backoff,
        status_forcelist=[502, 503, 504],
        allowed_methods=["GET", "POST"],
    )
    adapter = HTTPAdapter(
        max_retries=retry_strategy,
        pool_connections=20,
        pool_maxsize=50,
    )
    session.mount("http://", adapter)
    session.mount("https://", adapter)
    return session


# ===============================
# Health Check
# ===============================


def check_health(timeout: int) -> bool:
    """Verify all servers are reachable before running the full test."""
    session = _make_session(retries=1)
    all_ok = True

    targets = [
        ("Ingress (8085)", f"{INGRESS_URL}/api/health"),
    ]
    for srv in SERVERS:
        targets.append((f"Server {srv.port}", f"{srv.base_url}/api/health"))

    for label, url in targets:
        try:
            r = session.get(url, timeout=timeout)
            status = "OK" if r.status_code == 200 else f"HTTP {r.status_code}"
            print(f"    {label:20s} {url:40s} {status}")
        except Exception as e:
            print(f"    {label:20s} {url:40s} FAILED ({e})")
            all_ok = False
    return all_ok


# ===============================
# Auth
# ===============================


class AuthManager:
    """Register + login on AUTH_URL (8080). Holds token and userId for Ingress requests."""

    def __init__(self, thread_id: int, timeout: int):
        self.thread_id = thread_id
        self.timeout = timeout
        self.username = f"user_t{thread_id}_{int(time.time())}"
        self.email = f"u{thread_id}_{int(time.time())}@test.local"
        self.password = "password123"
        self.token: Optional[str] = None
        self.user_id: Optional[str] = None
        self.session = _make_session()

    def register_and_login(self) -> bool:
        try:
            r = self.session.post(
                f"{AUTH_URL}/api/auth/register",
                json={"username": self.username, "email": self.email, "password": self.password},
                headers={"Content-Type": "application/json"},
                timeout=self.timeout,
            )
            if r.status_code == 200:
                data = r.json()
                self.token = data["token"]
                self.user_id = data["user"]["userId"]
                return True
            return self._login()
        except Exception as e:
            print(f"    [T{self.thread_id}] Auth failed: {e}")
            return False

    def _login(self) -> bool:
        try:
            r = self.session.post(
                f"{AUTH_URL}/api/auth/login",
                json={"username": self.username, "password": self.password},
                headers={"Content-Type": "application/json"},
                timeout=self.timeout,
            )
            if r.status_code == 200:
                data = r.json()
                self.token = data["token"]
                self.user_id = data["user"]["userId"]
                return True
            return False
        except Exception:
            return False

    def headers(self) -> Dict[str, str]:
        if not self.token or not self.user_id:
            raise ValueError("Not authenticated")
        return {
            "Authorization": f"Bearer {self.token}",
            "Content-Type": "application/json",
        }


# ===============================
# Symbol -> Server (for read-phase)
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
    timeout: int,
    delay_min: float,
    delay_max: float,
    results_list: List[Dict],
    results_lock: threading.Lock,
) -> None:
    """Single user: auth on 8080, submit orders to Ingress 8085."""
    auth = AuthManager(thread_id, timeout)
    if not auth.register_and_login():
        print(f"    [T{thread_id}] Auth failed. Skipping.")
        return

    successful = 0
    failed = 0
    response_times: List[float] = []
    status_codes: Dict[int, int] = {}
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
            r = auth.session.post(
                f"{INGRESS_URL}/api/orders/submit",
                json=payload,
                headers=auth.headers(),
                timeout=timeout,
            )
            dur_ms = (time.perf_counter() - t0) * 1000
            status_codes[r.status_code] = status_codes.get(r.status_code, 0) + 1

            if r.status_code == 200:
                successful += 1
                response_times.append(dur_ms)
            else:
                failed += 1
                if len(errors) < 5:
                    errors.append({"status": r.status_code, "text": (r.text or "")[:120]})
        except requests.exceptions.Timeout:
            failed += 1
            if len(errors) < 5:
                errors.append({"error": "timeout"})
        except Exception as e:
            failed += 1
            if len(errors) < 5:
                errors.append({"error": str(e)[:120]})

        if delay_max > 0:
            time.sleep(random.uniform(delay_min, delay_max))

    with results_lock:
        results_list.append({
            "thread_id": thread_id,
            "successful": successful,
            "failed": failed,
            "response_times": response_times,
            "status_codes": status_codes,
            "errors": errors,
        })
    print(f"    [T{thread_id}] Done: {successful} ok, {failed} fail")


def _read_worker(
    thread_id: int,
    num_requests: int,
    timeout: int,
    router: SymbolRouter,
    results_list: List[Dict],
    results_lock: threading.Lock,
) -> None:
    """Hit GET /api/stocks/{symbol} on symbol-specific servers."""
    auth = AuthManager(thread_id, timeout)
    if not auth.register_and_login():
        return

    server_stats: Dict[str, Dict[str, Any]] = {s.base_url: {"success": 0, "failed": 0, "times": []} for s in SERVERS}
    for _ in range(num_requests):
        symbol = random.choice(ALL_SYMBOLS)
        server = router.get_server(symbol)
        url = f"{server.base_url}/api/stocks/{symbol}"
        try:
            t0 = time.perf_counter()
            r = auth.session.get(url, headers=auth.headers(), timeout=timeout)
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
    print(f"    [T{thread_id}] Read phase done.")


def _health_monitor(
    stop_event: threading.Event,
    interval: float,
    timeout: int,
    results: List[Dict],
) -> None:
    """Poll /api/health on all servers at fixed intervals during the test."""
    session = _make_session(retries=0)
    targets = [INGRESS_URL] + [s.base_url for s in SERVERS]
    while not stop_event.is_set():
        for base in targets:
            try:
                t0 = time.perf_counter()
                r = session.get(f"{base}/api/health", timeout=timeout)
                dur_ms = (time.perf_counter() - t0) * 1000
                results.append({
                    "ts": time.time(),
                    "server": base,
                    "status": r.status_code,
                    "latency_ms": dur_ms,
                })
            except Exception as e:
                results.append({
                    "ts": time.time(),
                    "server": base,
                    "status": -1,
                    "latency_ms": 0,
                    "error": str(e)[:80],
                })
        stop_event.wait(interval)


# ===============================
# Reporting
# ===============================


def _report_orders(results_list: List[Dict], total_duration_sec: float) -> None:
    total_ok = sum(r["successful"] for r in results_list)
    total_fail = sum(r["failed"] for r in results_list)
    all_times: List[float] = []
    all_status: Dict[int, int] = {}
    sample_errors: List[Dict] = []

    for r in results_list:
        all_times.extend(r["response_times"])
        for code, cnt in r["status_codes"].items():
            all_status[code] = all_status.get(code, 0) + cnt
        sample_errors.extend(r["errors"][:2])

    throughput = total_ok / total_duration_sec if total_duration_sec > 0 else 0

    print("\n" + "=" * 65)
    print(f"  ORDER SUBMISSION (Ingress {INGRESS_URL})")
    print("=" * 65)
    print(f"  Duration:      {total_duration_sec:.2f}s")
    print(f"  Throughput:    {throughput:.2f} orders/sec")
    print(f"  Successful:    {total_ok}")
    print(f"  Failed:        {total_fail}")
    print(f"  Status codes:  {dict(sorted(all_status.items()))}")

    if all_times:
        all_times.sort()
        s = stdev(all_times) if len(all_times) >= 2 else 0.0
        print(f"  Latency (ms):  avg={mean(all_times):.2f}  median={median(all_times):.2f}  stdev={s:.2f}")
        print(f"                 min={all_times[0]:.2f}  max={all_times[-1]:.2f}")
        print(f"                 p50={_percentile(all_times, 50):.2f}  p95={_percentile(all_times, 95):.2f}  p99={_percentile(all_times, 99):.2f}")
    else:
        print("  Latency:       no successful requests")

    if sample_errors:
        print(f"  Sample errors: {sample_errors[:3]}")
    print("=" * 65)


def _report_reads(results_list: List[Dict], total_duration_sec: float) -> None:
    agg: Dict[str, Dict[str, Any]] = {s.base_url: {"success": 0, "failed": 0, "times": []} for s in SERVERS}
    for r in results_list:
        for url, st in r["server_stats"].items():
            agg[url]["success"] += st["success"]
            agg[url]["failed"] += st["failed"]
            agg[url]["times"].extend(st["times"])

    total_ok = sum(agg[u]["success"] for u in agg)
    throughput = total_ok / total_duration_sec if total_duration_sec > 0 else 0

    print("\n" + "=" * 65)
    print("  READ PHASE (GET /api/stocks/{symbol} per server)")
    print("=" * 65)
    print(f"  Duration:      {total_duration_sec:.2f}s")
    print(f"  Throughput:    {throughput:.2f} req/sec (all servers)")

    for url in agg:
        st = agg[url]
        ok, fail, times = st["success"], st["failed"], st["times"]
        if times:
            times.sort()
            lat = f"avg={mean(times):.2f}ms  p95={_percentile(times, 95):.2f}ms"
        else:
            lat = "n/a"
        print(f"    {url}: ok={ok}  fail={fail}  {lat}")
    print("=" * 65)


def _report_health(health_results: List[Dict]) -> None:
    if not health_results:
        return

    by_server: Dict[str, Dict[str, Any]] = {}
    for h in health_results:
        srv = h["server"]
        if srv not in by_server:
            by_server[srv] = {"ok": 0, "fail": 0, "times": []}
        if h["status"] == 200:
            by_server[srv]["ok"] += 1
            by_server[srv]["times"].append(h["latency_ms"])
        else:
            by_server[srv]["fail"] += 1

    print("\n" + "=" * 65)
    print("  HEALTH MONITOR (/api/health)")
    print("=" * 65)
    for srv in sorted(by_server.keys()):
        st = by_server[srv]
        total = st["ok"] + st["fail"]
        lat = f"avg={mean(st['times']):.2f}ms  max={max(st['times']):.2f}ms" if st["times"] else "n/a"
        print(f"    {srv}: {total} checks ({st['ok']} ok, {st['fail']} fail)  {lat}")
    print("=" * 65)


# ===============================
# Scenario Runner
# ===============================


@dataclass
class ScenarioConfig:
    name: str
    threads: int
    orders: int
    no_sleep: bool
    read_phase: int
    monitor: bool


PRESET_SCENARIOS: Dict[str, ScenarioConfig] = {
    "smoke": ScenarioConfig(
        name="Quick Smoke Test",
        threads=5,
        orders=50,
        no_sleep=False,
        read_phase=0,
        monitor=False,
    ),
    "load": ScenarioConfig(
        name="Full Load Test (with reads + health monitor)",
        threads=20,
        orders=1000,
        no_sleep=False,
        read_phase=100,
        monitor=True,
    ),
    "throughput": ScenarioConfig(
        name="Max Throughput (no delay)",
        threads=50,
        orders=5000,
        no_sleep=True,
        read_phase=0,
        monitor=False,
    ),
    "stress": ScenarioConfig(
        name="High-Concurrency Stress Test (100 threads x 200 orders)",
        threads=100,
        orders=20000,
        no_sleep=True,
        read_phase=50,
        monitor=True,
    ),
}


def _run_scenario(
    scenario_name: str,
    num_threads: int,
    total_orders: int,
    no_sleep: bool,
    read_phase: int,
    monitor: bool,
    monitor_interval: float,
    timeout: int,
    skip_health: bool,
) -> None:
    """Execute a single test scenario end-to-end."""
    orders_per_thread = max(1, total_orders // num_threads)
    delay_min, delay_max = (0.0, 0.0) if no_sleep else (0.001, 0.01)

    print()
    print("#" * 65)
    print(f"  SCENARIO: {scenario_name}")
    print("#" * 65)
    print(f"  Auth:          {AUTH_URL}")
    print(f"  Ingress:       {INGRESS_URL}")
    print(f"  Servers:       {', '.join(s.base_url for s in SERVERS)}")
    print(f"  Threads:       {num_threads}")
    print(f"  Orders:        {total_orders} ({orders_per_thread} per thread)")
    print(f"  Delay:         {'none' if no_sleep else '1-10ms'}")
    print(f"  Read phase:    {read_phase} req/thread" if read_phase else "  Read phase:    off")
    print(f"  Health monitor:{'on' if monitor else 'off'}")
    print(f"  Timeout:       {timeout}s")
    print("#" * 65)

    # --- Pre-flight health check ---
    if not skip_health:
        print("\n  [1/3] Health check...")
        if not check_health(timeout):
            print("\n  Some servers are unreachable. Use --skip-health to bypass.\n")
            return
        print("  Health check passed.")

    # --- Health monitor thread ---
    health_results: List[Dict] = []
    stop_monitor = threading.Event()
    monitor_thread = None
    if monitor:
        monitor_thread = threading.Thread(
            target=_health_monitor,
            args=(stop_monitor, monitor_interval, timeout, health_results),
            daemon=True,
        )
        monitor_thread.start()

    # --- Order phase ---
    print(f"\n  [2/3] Order submission phase ({total_orders} orders across {num_threads} threads)...")
    order_results: List[Dict] = []
    results_lock = threading.Lock()

    t0 = time.perf_counter()
    with ThreadPoolExecutor(max_workers=num_threads) as ex:
        futures = [
            ex.submit(
                _order_worker,
                tid,
                orders_per_thread,
                timeout,
                delay_min,
                delay_max,
                order_results,
                results_lock,
            )
            for tid in range(1, num_threads + 1)
        ]
        for f in as_completed(futures):
            try:
                f.result()
            except Exception as e:
                print(f"    Thread error: {e}")
    order_duration = time.perf_counter() - t0
    _report_orders(order_results, order_duration)

    # --- Optional read phase ---
    if read_phase > 0:
        print(f"\n  [3/3] Read phase ({read_phase} req/thread)...")
        read_results: List[Dict] = []
        router = SymbolRouter(SERVERS)
        t0 = time.perf_counter()
        with ThreadPoolExecutor(max_workers=num_threads) as ex:
            futures = [
                ex.submit(_read_worker, tid, read_phase, timeout, router, read_results, results_lock)
                for tid in range(1, num_threads + 1)
            ]
            for f in as_completed(futures):
                try:
                    f.result()
                except Exception as e:
                    print(f"    Read thread error: {e}")
        read_duration = time.perf_counter() - t0
        _report_reads(read_results, read_duration)

    # --- Stop health monitor ---
    if monitor_thread:
        stop_monitor.set()
        monitor_thread.join(timeout=5)
        _report_health(health_results)


# ===============================
# Main
# ===============================


def main() -> None:
    ap = argparse.ArgumentParser(
        description="Multi-server matching engine performance test",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""Examples:
  # Quick smoke test (5 threads, 50 orders)
  python multi_process_test.py --threads 5 --orders 50

  # Full load test with reads and health monitoring
  python multi_process_test.py --threads 20 --orders 1000 --read-phase 100 --monitor

  # Max throughput (no delay between orders)
  python multi_process_test.py --threads 50 --orders 5000 --no-sleep

  # High-concurrency stress test (100 threads x 200 orders = 20,000 total)
  python multi_process_test.py --threads 100 --orders 20000 --no-sleep --read-phase 50 --monitor

  # Run all four preset scenarios back-to-back
  python multi_process_test.py --scenario all

  # Run only the smoke preset
  python multi_process_test.py --scenario smoke

  # Run smoke + throughput presets
  python multi_process_test.py --scenario smoke,throughput

  # Run the stress test preset
  python multi_process_test.py --scenario stress
""",
    )

    ap.add_argument("--scenario", type=str, default=None, metavar="NAME",
                     help="Run preset scenario(s): smoke, load, throughput, stress, all, or comma-separated combo")
    ap.add_argument("--threads", type=int, default=DEFAULT_THREADS,
                     help=f"Concurrent user threads (default: {DEFAULT_THREADS})")
    ap.add_argument("--orders", type=int, default=DEFAULT_ORDERS,
                     help=f"Total orders split across threads (default: {DEFAULT_ORDERS})")
    ap.add_argument("--no-sleep", action="store_true",
                     help="No delay between orders (max throughput)")
    ap.add_argument("--read-phase", type=int, default=0, metavar="N",
                     help="After orders, run N read requests per thread (GET /api/stocks/{symbol})")
    ap.add_argument("--monitor", action="store_true",
                     help="Poll /api/health on all servers during the test")
    ap.add_argument("--monitor-interval", type=float, default=2.0,
                     help="Health poll interval in seconds (default: 2.0)")
    ap.add_argument("--timeout", type=int, default=DEFAULT_TIMEOUT,
                     help=f"HTTP request timeout in seconds (default: {DEFAULT_TIMEOUT})")
    ap.add_argument("--skip-health", action="store_true",
                     help="Skip initial health check")
    args = ap.parse_args()

    print()
    print("  Multi-Server Matching Engine Performance Test")
    print("=" * 65)

    # --- Preset scenario mode ---
    if args.scenario:
        scenario_keys: List[str] = []
        if args.scenario.lower() == "all":
            scenario_keys = list(PRESET_SCENARIOS.keys())
        else:
            scenario_keys = [s.strip().lower() for s in args.scenario.split(",")]

        invalid = [k for k in scenario_keys if k not in PRESET_SCENARIOS]
        if invalid:
            print(f"  Unknown scenario(s): {invalid}")
            print(f"  Available: {list(PRESET_SCENARIOS.keys())}")
            sys.exit(1)

        print(f"  Running {len(scenario_keys)} scenario(s): {', '.join(scenario_keys)}")

        for key in scenario_keys:
            sc = PRESET_SCENARIOS[key]
            _run_scenario(
                scenario_name=sc.name,
                num_threads=sc.threads,
                total_orders=sc.orders,
                no_sleep=sc.no_sleep,
                read_phase=sc.read_phase,
                monitor=sc.monitor,
                monitor_interval=args.monitor_interval,
                timeout=args.timeout,
                skip_health=args.skip_health,
            )

        print("\n  All scenarios complete.\n")
        return

    # --- Manual / custom mode ---
    _run_scenario(
        scenario_name="Custom",
        num_threads=max(1, args.threads),
        total_orders=max(1, args.orders),
        no_sleep=args.no_sleep,
        read_phase=args.read_phase,
        monitor=args.monitor,
        monitor_interval=args.monitor_interval,
        timeout=args.timeout,
        skip_health=args.skip_health,
    )

    print("\n  Done.\n")


if __name__ == "__main__":
    main()
