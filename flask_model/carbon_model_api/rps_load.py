#!/usr/bin/env python3
"""
HNBLUE - 恒定RPS负载生成器 (基于asyncio/aiohttp)

功能概述：
• 模拟恒定请求速率对目标API进行压力测试
• 支持优雅停止（Ctrl+C信号处理）
• 生成详细的请求级别和分钟级别统计报告
• 提供完整的性能指标汇总分析

测试配置：
• 目标接口：POST /api/login
• 请求速率：10 RPS (每秒请求数)
• 测试时长：1小时 (3600秒)
• 超时设置：20秒
• 测试数据：固定错误密码（触发认证失败流程）

输出文件：
• results.csv - 每个请求的详细记录（时间戳、状态码、延迟、错误信息）
• results_per_min.csv - 每分钟聚合统计（请求数、状态分布、延迟分位数）
• results_summary.txt - 测试总结报告（总体指标和文件路径）

技术特性：
• 高精度定时控制（微秒级调度）
• 连接池优化（最大1000并发连接）
• 百分位数计算（P50/P95/P99延迟）
• 实时进度显示（每100请求输出进度）
"""
import asyncio, aiohttp, time, csv, signal, sys
from pathlib import Path

# 全局停止标志，用于优雅终止测试
stop_flag = False

def handle_sig(*_):
    """信号处理函数：捕获Ctrl+C等终止信号，设置停止标志"""
    global stop_flag
    stop_flag = True
    print("\n[!] Received stop signal. Finishing in-flight requests...", file=sys.stderr)

def sleep_until(target_ts):
    """高精度睡眠直到目标时间戳"""
    while True:
        rem = target_ts - time.perf_counter()
        if rem <= 0:
            return
        time.sleep(rem if rem > 0.01 else 0.0005)  # 最小睡眠间隔500微秒

def pct(vals, p):
    """计算百分位数（线性插值法）"""
    if not vals: return None
    k = (len(vals)-1) * (p/100)
    f = int(k)
    c = min(f+1, len(vals)-1)
    if f == c: return vals[f]
    return vals[f] + (vals[c]-vals[f]) * (k-f)

# ========== 测试配置参数 ==========
URL = "http://localhost:8088/api/login"  # 目标API地址
METHOD = "POST"                          # 请求方法
RPS = 10                                 # 请求速率（每秒请求数）
DURATION_S = 3600                        # 测试持续时间（秒）
TIMEOUT = 20.0                           # 单个请求超时时间（秒）
PAYLOAD = {"username":"zhangsansan","password":"wrongpass"}  # 测试负载（错误密码）
HEADERS = {"Content-Type":"application/json"}               # 请求头
OUT_BASENAME = "results"                 # 输出文件基础名

async def main():
    """主测试逻辑"""
    total_planned = int(RPS * DURATION_S)  # 计划总请求数

    # ========== 初始化输出文件 ==========
    per_req_path = Path(f"{OUT_BASENAME}.csv")        # 请求级别详细数据
    per_min_path = Path(f"{OUT_BASENAME}_per_min.csv") # 分钟级别聚合数据
    summary_txt = Path(f"{OUT_BASENAME}_summary.txt")  # 测试总结报告

    # 请求级别CSV文件（每个请求一行记录）
    per_req_fp = per_req_path.open("w", newline="", encoding="utf-8")
    per_req = csv.writer(per_req_fp)
    per_req.writerow(["ts_iso","status","latency_ms","error","bytes"])

    # 分钟级别CSV文件（每分钟聚合统计）
    per_min_fp = per_min_path.open("w", newline="", encoding="utf-8")
    per_min = csv.writer(per_min_fp)
    per_min.writerow(["minute_iso","count","2xx","4xx","429","5xx","errors","p50_ms","p95_ms","p99_ms"])

    # ========== 信号处理设置 ==========
    for sig in (signal.SIGINT, signal.SIGTERM):
        try: signal.signal(sig, handle_sig)
        except Exception: pass

    # ========== HTTP客户端配置 ==========
    connector = aiohttp.TCPConnector(limit=1000)  # 连接池限制
    timeout = aiohttp.ClientTimeout(total=TIMEOUT)  # 请求超时设置
    session = aiohttp.ClientSession(connector=connector, timeout=timeout)

    # ========== 测试数据记录 ==========
    start_wall = time.time()      # 墙上时钟开始时间
    start_perf = time.perf_counter()  # 高精度计时开始时间
    latencies_ms = []             # 所有请求延迟记录
    buckets = {}                  # 按分钟分桶的统计数据

    async def do_request(i):
        """执行单个HTTP请求并记录结果"""
        nonlocal latencies_ms, buckets
        t0 = time.perf_counter()  # 请求开始时间
        err_msg = ""; status = -1; nbytes = 0
        try:
            # 发送HTTP POST请求
            async with session.post(URL, json=PAYLOAD, headers=HEADERS) as resp:
                status = resp.status
                body = await resp.read()
                nbytes = len(body)
        except Exception as e:
            err_msg = str(e)  # 记录异常信息

        t1 = time.perf_counter()
        lat = (t1 - t0) * 1000.0  # 计算延迟（毫秒）

        # 记录请求级别数据
        ts_iso = time.strftime("%Y-%m-%dT%H:%M:%S", time.localtime())
        per_req.writerow([ts_iso, status, f"{lat:.3f}", err_msg, nbytes])

        # 聚合到分钟级别桶
        minute_key = time.strftime("%Y-%m-%dT%H:%M", time.localtime())
        b = buckets.get(minute_key)
        if b is None:
            # 初始化分钟桶
            b = {"lats": [], "count":0,"2xx":0,"4xx":0,"429":0,"5xx":0,"errors":0}
            buckets[minute_key] = b

        b["count"] += 1
        if err_msg: b["errors"] += 1  # 网络错误
        else:
            # 统计HTTP状态码
            if 200 <= status <= 299:
                b["2xx"] += 1  # 成功响应
            elif status == 429:
                b["429"] += 1  # 限流响应
            elif 400 <= status <= 499:
                b["4xx"] += 1  # 客户端错误
            elif 500 <= status <= 599:
                b["5xx"] += 1  # 服务端错误

        b["lats"].append(lat)    # 记录延迟
        latencies_ms.append(lat) # 全局延迟记录

        # 进度显示（每100个请求）
        if (i+1) % 100 == 0:
            print(f"[{i+1}/{total_planned}] done")

    # ========== 主测试循环 ==========
    tasks = []
    for i in range(total_planned):
        if stop_flag: break  # 检查停止标志
        target_time = start_perf + (i / RPS)  # 精确调度：计算下一个请求的目标时间
        sleep_until(target_time)
        tasks.append(asyncio.create_task(do_request(i)))  # 创建并执行异步任务

    # 等待所有请求完成
    await asyncio.gather(*tasks, return_exceptions=True)

    # ========== 清理资源 ==========
    await session.close()
    per_req_fp.close()

    # ========== 生成分钟级别报告 ==========
    for minute, data in sorted(buckets.items()):
        lats = sorted(data["lats"])
        p50, p95, p99 = pct(lats,50), pct(lats,95), pct(lats,99)
        per_min.writerow([
            minute, data["count"], data["2xx"], data["4xx"], data["429"], data["5xx"], data["errors"],
            f"{p50:.3f}" if p50 else "",
            f"{p95:.3f}" if p95 else "",
            f"{p99:.3f}" if p99 else "",
        ])
    per_min_fp.close()

    # ========== 生成测试总结报告 ==========
    l_sorted = sorted(latencies_ms)
    total = sum(b["count"] for b in buckets.values())
    s_2xx = sum(b["2xx"] for b in buckets.values())
    s_4xx = sum(b["4xx"] for b in buckets.values())
    s_429 = sum(b["429"] for b in buckets.values())
    s_5xx = sum(b["5xx"] for b in buckets.values())
    s_err = sum(b["errors"] for b in buckets.values())
    p50, p95, p99 = pct(l_sorted,50), pct(l_sorted,95), pct(l_sorted,99)

    summary = [
        f"url           : {URL}",
        f"method        : {METHOD}",
        f"planned reqs  : {total_planned}",
        f"sent reqs     : {total}",
        f"rps (achieved): {total / max(1.0, (time.time() - start_wall)):.2f}",
        f"2xx           : {s_2xx}",
        f"4xx           : {s_4xx} (429={s_429})",
        f"5xx           : {s_5xx}",
        f"errors        : {s_err}",
        f"latency p50   : {p50:.3f} ms" if p50 else "latency p50   : n/a",
        f"latency p95   : {p95:.3f} ms" if p95 else "latency p95   : n/a",
        f"latency p99   : {p99:.3f} ms" if p99 else "latency p99   : n/a",
        f"per-request csv : {per_req_path.resolve()}",
        f"per-minute csv  : {per_min_path.resolve()}",
    ]
    # 输出总结到控制台和文件
    print("\n".join(summary))
    with summary_txt.open("w", encoding="utf-8") as fp:
        fp.write("\n".join(summary) + "\n")

if __name__ == "__main__":
    asyncio.run(main())
