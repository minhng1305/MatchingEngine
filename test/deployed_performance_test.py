#!/usr/bin/env python3

"""
Deployed Matching Engine Performance Test
==========================================
Tests performance of the production deployment.

ARCHITECTURE (production):
- Auth server (Server1): https://api.greentrader.org
  Handles /api/auth/register, /api/auth/login, read endpoints
- Ingress server: https://matchingengine-ingress-production.up.railway.app
  Handles /api/orders/submit -> produces to Kafka
- Server1 consumes from Kafka and processes orders

WHAT THIS TEST DOES:
1. Health check: Verifies the API is reachable before starting.
2. Order submission phase: N threads, each a unique user.
   - Auth (register/login) against the auth server (Server1)
   - Order submission against the ingress server
   Measures latency (avg, p50, p95, p99) and throughput (orders/sec).
3. Optional read phase (--read-phase): Stress GET endpoints for stocks/prices.
4. Optional health-monitor (--monitor): Polls /api/health during the test.

DIFFERENCES FROM LOCAL multi_process_test.py:
- Separate auth and ingress URLs instead of single localhost ports.
- HTTPS with real network latency — timeouts are longer.
- Configurable via --auth-url and --ingress-url for staging/custom deployments.
"""

import argparse
import random
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field
from statistics import mean, median, stdev
from typing import List, Dict, Any, Optional

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

DEFAULT_AUTH_URL = "https://api.greentrader.org"
DEFAULT_INGRESS_URL = "https://matchingengine-ingress-production.up.railway.app"

ALL_SYMBOLS = [
    "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NFLX",
    "NVDA", "AMD", "INTC", "IBM", "ORCL", "CSCO", "SAP",
    "ADOBE", "CRM", "TWTR", "SNAP", "BABA", "TCEHY",
]

ORDER_SIDES = ["BUY", "SELL"]
ORDER_TYPES = ["MARKET", "LIMIT"]

DEFAULT_THREADS = 20
DEFAULT_ORDERS = 1000
DEFAULT_TIMEOUT = 30


def _percentile(sorted_values: List[float], p: float) -> float:
    if not sorted_values:
        return 0.0
    k = (len(sorted_values) - 1) * (p / 100.0)
    f = int(k)
    c = f + 1 if f + 1 < len(sorted_values) else f
    return sorted_values[f] + (k - f) * (sorted_values[c] - sorted_values[f])


def _make_session(retries: int = 2, backoff: float = 0.3) -> requests.Session:
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
    session.mount("https://", adapter)
    session.mount("http://", adapter)
    return session


# ===============================
# Health Check
# ===============================


def check_health(base_url: str, timeout: int) -> bool:
    """Verify the API is reachable before running the full test."""
    endpoints = ["/api/health", "/api/health/ready", "/api/health/live"]
    session = _make_session(retries=1)
    all_ok = True
    for ep in endpoints:
        url = f"{base_url}{ep}"
        try:
            r = session.get(url, timeout=timeout)
            status = "OK" if r.status_code == 200 else f"HTTP {r.status_code}"
            print(f"  {ep}: {status}")
            if r.status_code != 200:
                all_ok = False
        except Exception as e:
            print(f"  {ep}: FAILED ({e})")
            return False
    return all_ok


# ===============================
# Auth
# ===============================


class AuthManager:
    """Register + login against the auth server (Server1), then submit orders to ingress."""

    def __init__(self, thread_id: int, auth_url: str, timeout: int):
        self.thread_id = thread_id
        self.auth_url = auth_url
        self.timeout = timeout
        self.username = f"perftest_t{thread_id}_{int(time.time())}"
        self.email = f"pt{thread_id}_{int(time.time())}@perftest.local"
        self.password = "perftest_Passw0rd!"
        self.token: Optional[str] = None
        self.user_id: Optional[str] = None
        self.session = _make_session()

    def register_and_login(self) -> bool:
        try:
            r = self.session.post(
                f"{self.auth_url}/api/auth/register",
                json={
                    "username": self.username,
                    "email": self.email,
                    "password": self.password,
                },
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
            print(f"  [T{self.thread_id}] Auth error: {e}")
            return False

    def _login(self) -> bool:
        try:
            r = self.session.post(
                f"{self.auth_url}/api/auth/login",
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
# Workers
# ===============================


def _order_worker(
    thread_id: int,
    num_orders: int,
    auth_url: str,
    ingress_url: str,
    timeout: int,
    delay_min: float,
    delay_max: float,
    results_list: List[Dict],
    results_lock: threading.Lock,
) -> None:
    """Single user: auth against Server1, then submit orders to Ingress."""
    auth = AuthManager(thread_id, auth_url, timeout)
    if not auth.register_and_login():
        print(f"  [T{thread_id}] Auth failed. Skipping.")
        return

    successful = 0
    failed = 0
    response_times: List[float] = []
    status_codes: Dict[int, int] = {}
    errors: List[Dict] = []

    for i in range(num_orders):
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
                f"{ingress_url}/api/orders/submit",
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
    print(f"  [T{thread_id}] Done: {successful} ok, {failed} fail")


def _read_worker(
    thread_id: int,
    num_requests: int,
    auth_url: str,
    timeout: int,
    results_list: List[Dict],
    results_lock: threading.Lock,
) -> None:
    """Hit various read endpoints on Server1 (reads are not on ingress)."""
    auth = AuthManager(thread_id, auth_url, timeout)
    if not auth.register_and_login():
        return

    endpoint_stats: Dict[str, Dict[str, Any]] = {}

    read_endpoints = [
        ("/api/stocks/all", False),
        ("/api/prices/all", False),
    ]
    for symbol in random.sample(ALL_SYMBOLS, min(5, len(ALL_SYMBOLS))):
        read_endpoints.append((f"/api/stocks/{symbol}", True))
        read_endpoints.append((f"/api/prices/current/{symbol}", False))

    for _ in range(num_requests):
        path, needs_auth = random.choice(read_endpoints)
        url = f"{auth_url}{path}"
        key = path.split("/")[3]  # "stocks" or "prices"

        if key not in endpoint_stats:
            endpoint_stats[key] = {"success": 0, "failed": 0, "times": []}

        try:
            hdrs = auth.headers() if needs_auth else {"Content-Type": "application/json"}
            t0 = time.perf_counter()
            r = auth.session.get(url, headers=hdrs, timeout=timeout)
            dur_ms = (time.perf_counter() - t0) * 1000
            if r.status_code == 200:
                endpoint_stats[key]["success"] += 1
                endpoint_stats[key]["times"].append(dur_ms)
            else:
                endpoint_stats[key]["failed"] += 1
        except Exception:
            endpoint_stats[key]["failed"] += 1

    with results_lock:
        results_list.append({"thread_id": thread_id, "endpoint_stats": endpoint_stats})
    print(f"  [T{thread_id}] Read phase done.")


def _health_monitor(
    base_url: str,
    timeout: int,
    stop_event: threading.Event,
    interval: float,
    results: List[Dict],
) -> None:
    """Poll /api/health at fixed intervals during the test."""
    session = _make_session(retries=0)
    while not stop_event.is_set():
        try:
            t0 = time.perf_counter()
            r = session.get(f"{base_url}/api/health", timeout=timeout)
            dur_ms = (time.perf_counter() - t0) * 1000
            results.append({
                "ts": time.time(),
                "status": r.status_code,
                "latency_ms": dur_ms,
            })
        except Exception as e:
            results.append({"ts": time.time(), "status": -1, "latency_ms": 0, "error": str(e)[:80]})
        stop_event.wait(interval)


# ===============================
# Reporting
# ===============================


def _report_orders(results_list: List[Dict], total_duration_sec: float, ingress_url: str) -> None:
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
    print(f"  ORDER SUBMISSION — {ingress_url}/api/orders/submit")
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
    agg: Dict[str, Dict[str, Any]] = {}
    for r in results_list:
        for key, st in r["endpoint_stats"].items():
            if key not in agg:
                agg[key] = {"success": 0, "failed": 0, "times": []}
            agg[key]["success"] += st["success"]
            agg[key]["failed"] += st["failed"]
            agg[key]["times"].extend(st["times"])

    total_ok = sum(agg[k]["success"] for k in agg)
    throughput = total_ok / total_duration_sec if total_duration_sec > 0 else 0

    print("\n" + "=" * 65)
    print("  READ PHASE (GET /api/stocks/*, /api/prices/*)")
    print("=" * 65)
    print(f"  Duration:      {total_duration_sec:.2f}s")
    print(f"  Throughput:    {throughput:.2f} req/sec (total)")

    for key in sorted(agg.keys()):
        st = agg[key]
        ok, fail, times = st["success"], st["failed"], st["times"]
        if times:
            times.sort()
            lat = f"avg={mean(times):.2f}ms  p95={_percentile(times, 95):.2f}ms"
        else:
            lat = "n/a"
        print(f"  /api/{key}: ok={ok}  fail={fail}  {lat}")
    print("=" * 65)


def _report_health(health_results: List[Dict]) -> None:
    if not health_results:
        return
    ok = sum(1 for h in health_results if h["status"] == 200)
    fail = len(health_results) - ok
    times = [h["latency_ms"] for h in health_results if h["status"] == 200]

    print("\n" + "=" * 65)
    print("  HEALTH MONITOR (/api/health)")
    print("=" * 65)
    print(f"  Checks:        {len(health_results)} ({ok} ok, {fail} fail)")
    if times:
        print(f"  Latency (ms):  avg={mean(times):.2f}  max={max(times):.2f}")
    print("=" * 65)


# ===============================
# Main
# ===============================


def main() -> None:
    ap = argparse.ArgumentParser(
        description="Performance test for deployed Matching Engine",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""Examples:
  # Quick smoke test (5 threads, 50 orders)
  python deployed_performance_test.py --threads 5 --orders 50

  # Full load test with reads and health monitoring
  python deployed_performance_test.py --threads 20 --orders 1000 --read-phase 100 --monitor

  # Max throughput (no delay between orders)
  python deployed_performance_test.py --threads 50 --orders 5000 --no-sleep

  # Custom deployment URLs
  python deployed_performance_test.py --auth-url https://staging-api.greentrader.org --ingress-url https://staging-ingress.up.railway.app --threads 10 --orders 200
""",
    )
    ap.add_argument("--auth-url", type=str, default=DEFAULT_AUTH_URL,
                     help=f"Auth server URL for register/login and reads (default: {DEFAULT_AUTH_URL})")
    ap.add_argument("--ingress-url", type=str, default=DEFAULT_INGRESS_URL,
                     help=f"Ingress server URL for order submission (default: {DEFAULT_INGRESS_URL})")
    ap.add_argument("--threads", type=int, default=DEFAULT_THREADS,
                     help=f"Concurrent user threads (default: {DEFAULT_THREADS})")
    ap.add_argument("--orders", type=int, default=DEFAULT_ORDERS,
                     help=f"Total orders split across threads (default: {DEFAULT_ORDERS})")
    ap.add_argument("--no-sleep", action="store_true",
                     help="No delay between orders (max throughput)")
    ap.add_argument("--read-phase", type=int, default=0, metavar="N",
                     help="After orders, run N read requests per thread")
    ap.add_argument("--monitor", action="store_true",
                     help="Poll /api/health during the test")
    ap.add_argument("--monitor-interval", type=float, default=2.0,
                     help="Health poll interval in seconds (default: 2.0)")
    ap.add_argument("--timeout", type=int, default=DEFAULT_TIMEOUT,
                     help=f"HTTP request timeout in seconds (default: {DEFAULT_TIMEOUT})")
    ap.add_argument("--skip-health", action="store_true",
                     help="Skip initial health check")
    args = ap.parse_args()

    auth_url = args.auth_url.rstrip("/")
    ingress_url = args.ingress_url.rstrip("/")
    num_threads = max(1, args.threads)
    total_orders = max(1, args.orders)
    orders_per_thread = total_orders // num_threads
    delay_min, delay_max = (0.0, 0.0) if args.no_sleep else (0.005, 0.05)

    print()
    print("  Deployed Matching Engine Performance Test")
    print("=" * 65)
    print(f"  Auth server:   {auth_url}")
    print(f"  Ingress:       {ingress_url}")
    print(f"  Threads:       {num_threads}")
    print(f"  Orders:        {total_orders} ({orders_per_thread} per thread)")
    print(f"  Delay:         {'none' if args.no_sleep else '5-50ms'}")
    print(f"  Read phase:    {args.read_phase} req/thread" if args.read_phase else "  Read phase:    off")
    print(f"  Health monitor:{'on' if args.monitor else 'off'}")
    print(f"  Timeout:       {args.timeout}s")
    print("=" * 65)

    # --- Pre-flight health check (against auth server which has full endpoints) ---
    if not args.skip_health:
        print("\n[1/3] Health check (auth server)...")
        if not check_health(auth_url, args.timeout):
            print("\n  Auth server is not reachable. Use --skip-health to bypass.")
            sys.exit(1)
        print("  Health check passed.\n")

    # --- Health monitor thread ---
    health_results: List[Dict] = []
    stop_monitor = threading.Event()
    monitor_thread = None
    if args.monitor:
        monitor_thread = threading.Thread(
            target=_health_monitor,
            args=(auth_url, args.timeout, stop_monitor, args.monitor_interval, health_results),
            daemon=True,
        )
        monitor_thread.start()

    # --- Order phase ---
    print("[2/3] Order submission phase...")
    print(f"  Auth via:   {auth_url}")
    print(f"  Orders via: {ingress_url}")
    order_results: List[Dict] = []
    results_lock = threading.Lock()

    t0 = time.perf_counter()
    with ThreadPoolExecutor(max_workers=num_threads) as ex:
        futures = [
            ex.submit(
                _order_worker,
                tid,
                orders_per_thread,
                auth_url,
                ingress_url,
                args.timeout,
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
                print(f"  Thread error: {e}")
    order_duration = time.perf_counter() - t0
    _report_orders(order_results, order_duration, ingress_url)

    # --- Optional read phase ---
    if args.read_phase > 0:
        print("\n[3/3] Read phase...")
        read_results: List[Dict] = []
        t0 = time.perf_counter()
        with ThreadPoolExecutor(max_workers=num_threads) as ex:
            futures = [
                ex.submit(
                    _read_worker,
                    tid,
                    args.read_phase,
                    auth_url,
                    args.timeout,
                    read_results,
                    results_lock,
                )
                for tid in range(1, num_threads + 1)
            ]
            for f in as_completed(futures):
                try:
                    f.result()
                except Exception as e:
                    print(f"  Read thread error: {e}")
        read_duration = time.perf_counter() - t0
        _report_reads(read_results, read_duration)

    # --- Stop health monitor ---
    if monitor_thread:
        stop_monitor.set()
        monitor_thread.join(timeout=5)
        _report_health(health_results)

    print("\nDone.\n")


if __name__ == "__main__":
    main()
