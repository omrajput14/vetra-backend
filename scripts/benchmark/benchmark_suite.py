#!/usr/bin/env python3
"""
Vetra Enterprise Cache Performance Benchmark Suite — Stage 12.3.4
=================================================================
Automated empirical load driver measuring cold-cache latency, warm-cache latency,
cache warm-up curves, concurrent throughput, Redis telemetry, and Spring Actuator metrics.

Usage:
    python3 scripts/benchmark/benchmark_suite.py
"""

import json
import statistics
import time
import urllib.request
import subprocess
import os
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed


BASE_URL = os.environ.get("VETRA_BASE_URL", "http://localhost:8080")
LOGIN_URL = f"{BASE_URL}/api/v1/auth/farmer/login"
REDIS_CLI = ["docker", "compose", "exec", "-T", "redis", "redis-cli", "-a", "vetra_redis_password_secret"]

ENDPOINTS = [
    {"name": "Farmer Dashboard", "path": "/api/v1/dashboard", "method": "GET"},
    {"name": "User Profile", "path": "/api/v1/auth/me", "method": "GET"},
    {"name": "Unread Notifications", "path": "/api/v1/notifications/unread", "method": "GET"},
]

CONCURRENCY = 20
WARM_REQUESTS = 500
WARMUP_ITERATIONS = 20


def redis_cmd(*args):
    """Execute a redis-cli command and return output."""
    try:
        result = subprocess.run(REDIS_CLI + list(args), capture_output=True, text=True, cwd=os.getcwd())
        return result.stdout.strip()
    except Exception as e:
        return ""


def redis_stats():
    """Collect Redis INFO stats."""
    stats_raw = redis_cmd("INFO", "stats")
    mem_raw = redis_cmd("INFO", "memory")
    dbsize = redis_cmd("DBSIZE")

    stats = {}
    for line in (stats_raw + "\n" + mem_raw).split("\n"):
        line = line.strip()
        if ":" in line and not line.startswith("#"):
            k, v = line.split(":", 1)
            stats[k] = v
    stats["dbsize"] = dbsize
    return stats


def get_token():
    """Authenticate and return JWT bearer token."""
    payload = json.dumps({
        "identifier": "cachetest.farmer@vetra.app",
        "password": "Password123!"
    }).encode("utf-8")
    req = urllib.request.Request(LOGIN_URL, data=payload, headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(req) as resp:
        body = json.loads(resp.read())
    return body["data"]["accessToken"]


def timed_request(url, headers):
    """Make a single GET request and return (latency_ms, status_code)."""
    req = urllib.request.Request(url, headers=headers, method="GET")
    start = time.perf_counter()
    try:
        with urllib.request.urlopen(req) as resp:
            resp.read()
            status = resp.status
    except Exception:
        return (time.perf_counter() - start) * 1000, 0
    return (time.perf_counter() - start) * 1000, status


def percentile(data, p):
    """Calculate the p-th percentile of a list."""
    if not data:
        return 0
    sorted_data = sorted(data)
    k = (len(sorted_data) - 1) * p / 100
    f = int(k)
    c = f + 1 if f + 1 < len(sorted_data) else f
    return sorted_data[f] + (k - f) * (sorted_data[c] - sorted_data[f])


def run_cold_cache_test(token, endpoint):
    """Single cold-cache request (cache is empty)."""
    url = f"{BASE_URL}{endpoint['path']}"
    headers = {"Authorization": f"Bearer {token}"}
    latency, status = timed_request(url, headers)
    return {"latency_ms": latency, "status": status}


def run_warmup_curve(token, endpoint, iterations):
    """Measure latency for each sequential request to observe warm-up behavior."""
    url = f"{BASE_URL}{endpoint['path']}"
    headers = {"Authorization": f"Bearer {token}"}
    curve = []
    for i in range(iterations):
        latency, status = timed_request(url, headers)
        curve.append({"request_num": i + 1, "latency_ms": round(latency, 2), "status": status})
    return curve


def run_concurrent_load(token, endpoint, num_requests, concurrency):
    """Run concurrent load test and return latency statistics."""
    url = f"{BASE_URL}{endpoint['path']}"
    headers = {"Authorization": f"Bearer {token}"}
    latencies = []
    errors = 0

    def worker():
        return timed_request(url, headers)

    start_time = time.perf_counter()
    with ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = [executor.submit(worker) for _ in range(num_requests)]
        for future in as_completed(futures):
            latency, status = future.result()
            if status == 200:
                latencies.append(latency)
            else:
                errors += 1
    wall_time = time.perf_counter() - start_time

    if not latencies:
        return None

    return {
        "requests": num_requests,
        "concurrency": concurrency,
        "errors": errors,
        "wall_time_sec": round(wall_time, 3),
        "throughput_rps": round(len(latencies) / wall_time, 1),
        "avg_ms": round(statistics.mean(latencies), 2),
        "median_ms": round(statistics.median(latencies), 2),
        "min_ms": round(min(latencies), 2),
        "max_ms": round(max(latencies), 2),
        "stdev_ms": round(statistics.stdev(latencies), 2) if len(latencies) > 1 else 0,
        "p50_ms": round(percentile(latencies, 50), 2),
        "p90_ms": round(percentile(latencies, 90), 2),
        "p95_ms": round(percentile(latencies, 95), 2),
        "p99_ms": round(percentile(latencies, 99), 2),
    }


def get_actuator_metrics(token):
    """Fetch Actuator cache.gets and cache.puts metrics."""
    headers = {"Authorization": f"Bearer {token}"}
    metrics = {}
    for metric_name in ["cache.gets", "cache.puts"]:
        url = f"{BASE_URL}/actuator/metrics/{metric_name}"
        req = urllib.request.Request(url, headers=headers)
        try:
            with urllib.request.urlopen(req) as resp:
                data = json.loads(resp.read())
                metrics[metric_name] = data.get("measurements", [{}])[0].get("value", 0)
        except Exception:
            metrics[metric_name] = "N/A"
    return metrics


def main():
    print("=" * 72)
    print("  VETRA ENTERPRISE CACHE BENCHMARK SUITE — Stage 12.3.4")
    print("=" * 72)

    print("\n[1/6] Collecting Redis pre-benchmark baseline...")
    pre_stats = redis_stats()
    pre_hits = int(pre_stats.get("keyspace_hits", 0))
    pre_misses = int(pre_stats.get("keyspace_misses", 0))
    print(f"  Redis keyspace_hits={pre_hits} keyspace_misses={pre_misses} dbsize={pre_stats.get('dbsize', '?')}")

    print("\n[2/6] Authenticating...")
    token = get_token()
    print(f"  Token acquired (first 20 chars): {token[:20]}...")

    print("\n[3/6] COLD CACHE TEST (first request after flush)...")
    cold_results = {}
    for ep in ENDPOINTS:
        result = run_cold_cache_test(token, ep)
        cold_results[ep["name"]] = result
        print(f"  {ep['name']}: {result['latency_ms']:.2f} ms (HTTP {result['status']})")

    print(f"\n[4/6] CACHE WARM-UP CURVE ({WARMUP_ITERATIONS} sequential requests per endpoint)...")
    warmup_results = {}
    for ep in ENDPOINTS:
        curve = run_warmup_curve(token, ep, WARMUP_ITERATIONS)
        warmup_results[ep["name"]] = curve
        first_3 = [f"{c['latency_ms']:.1f}ms" for c in curve[:3]]
        last_3 = [f"{c['latency_ms']:.1f}ms" for c in curve[-3:]]
        print(f"  {ep['name']}: first 3 = {first_3}, last 3 = {last_3}")

    print(f"\n[5/6] CONCURRENT LOAD TEST ({WARM_REQUESTS} requests × C={CONCURRENCY})...")
    load_results = {}
    for ep in ENDPOINTS:
        result = run_concurrent_load(token, ep, WARM_REQUESTS, CONCURRENCY)
        load_results[ep["name"]] = result
        if result:
            print(f"  {ep['name']}:")
            print(f"    avg={result['avg_ms']:.2f}ms  median={result['median_ms']:.2f}ms  "
                  f"p95={result['p95_ms']:.2f}ms  p99={result['p99_ms']:.2f}ms")
            print(f"    throughput={result['throughput_rps']} req/sec  errors={result['errors']}")

    print("\n[6/6] Collecting post-benchmark Redis telemetry...")
    post_stats = redis_stats()
    post_hits = int(post_stats.get("keyspace_hits", 0))
    post_misses = int(post_stats.get("keyspace_misses", 0))
    total_ops = post_hits + post_misses
    hit_ratio = (post_hits / total_ops * 100) if total_ops > 0 else 0

    print(f"  keyspace_hits={post_hits}  keyspace_misses={post_misses}")
    print(f"  Cache Hit Ratio: {hit_ratio:.2f}%")
    print(f"  used_memory: {post_stats.get('used_memory_human', '?')}")
    print(f"  used_memory_peak: {post_stats.get('used_memory_peak_human', '?')}")
    print(f"  total_commands_processed: {post_stats.get('total_commands_processed', '?')}")
    print(f"  evicted_keys: {post_stats.get('evicted_keys', '?')}")
    print(f"  dbsize: {post_stats.get('dbsize', '?')}")

    actuator = get_actuator_metrics(token)
    print(f"\n  Actuator cache.gets: {actuator.get('cache.gets', 'N/A')}")
    print(f"  Actuator cache.puts: {actuator.get('cache.puts', 'N/A')}")

    print("\n" + "=" * 72)
    print("  BENCHMARK SUMMARY")
    print("=" * 72)
    print(f"\n{'Endpoint':<25} {'Cold (ms)':<12} {'Warm Avg (ms)':<15} {'Warm p95 (ms)':<15} {'Warm p99 (ms)':<15} {'RPS':<10}")
    print("-" * 92)
    for ep in ENDPOINTS:
        name = ep["name"]
        cold = cold_results[name]["latency_ms"]
        load = load_results[name]
        if load:
            improvement = ((cold - load["avg_ms"]) / cold * 100) if cold > 0 else 0
            print(f"{name:<25} {cold:<12.2f} {load['avg_ms']:<15.2f} {load['p95_ms']:<15.2f} {load['p99_ms']:<15.2f} {load['throughput_rps']:<10}")

    print(f"\n  Overall Redis Cache Hit Ratio: {hit_ratio:.2f}%")
    print("=" * 72)


if __name__ == "__main__":
    main()
