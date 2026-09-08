# 海南蓝碳平台 Redis 缓存设计与验证

更新时间：2026-07-14

## 实施边界

- 仅缓存 `/api/v2/**` 中高频、低更新、无用户隐私、无权限差异的读取接口。
- 不缓存登录、Token、权限判断、个人中心、管理员用户列表、用户详情、密码重置、操作日志、SSE 流和 AnythingLLM 最终回答。
- 统一 Redis 命名空间：`hnblue:cache:v2:`
- 使用 Spring Cache + RedisCacheManager，禁用空值缓存。
- 缓存清理验证仅使用 `SCAN --pattern hnblue:cache:v2:<cache>:*` 定位专属键，并对专属键执行 `DEL` 或 `EXPIRE`；未执行 `KEYS *`、`FLUSHALL`、`FLUSHDB`。

## 代码入口

- `backend/src/main/java/com/example/jpaspringboot/config/RedisConfig.java`
- `backend/src/main/java/com/example/jpaspringboot/service/V2DataService.java`
- `backend/src/main/java/com/example/jpaspringboot/service/V2CacheKeys.java`

## 缓存策略

| 接口 | Cache Name | Key | TTL | 空值策略 | 失效策略 |
|---|---|---|---:|---|---|
| `/api/v2/dashboard/summary` | `dashboard-summary` | `all` | 10 min | 不缓存 null / success=false | 数据导入或 v2 基础表更新后删除 `hnblue:cache:v2:dashboard-summary:*` |
| `/api/v2/sources` | `sources` | 归一化查询参数 | 6 h | 不缓存 null / success=false | 来源数据导入或修订后删除 `hnblue:cache:v2:sources:*` |
| `/api/v2/mangrove-cover` | `mangrove-cover` | 归一化查询参数 | 30 min | 不缓存 null / success=false | 红树林覆盖数据导入后删除 `hnblue:cache:v2:mangrove-cover:*` |
| `/api/v2/region-metrics` | `region-metrics` | 归一化查询参数 | 30 min | 不缓存 null / success=false | 区域指标数据导入后删除 `hnblue:cache:v2:region-metrics:*` |
| `/api/v2/literature-carbon` | `literature-carbon` | 归一化查询参数 | 2 h | 不缓存 null / success=false | 文献碳数据导入后删除 `hnblue:cache:v2:literature-carbon:*` |
| `/api/v2/region-overview/{regionId}` | `region-overview` | 归一化 regionId | 15 min | 不缓存 null / success=false | 区域相关数据导入后删除 `hnblue:cache:v2:region-overview:*` |
| `/api/v2/ai/context` | `ai-context` | 归一化查询参数 | 10 min | 不缓存 null / success=false | v2 AI 上下文来源表更新后删除 `hnblue:cache:v2:ai-context:*` |

## 参数归一化

- null 或空参数归一为 `none`。
- 参数值 trim，并折叠连续空白。
- Map 参数按 key 排序后拼接，避免相同查询因参数顺序不同产生多个缓存键。
- 与限流键隔离：限流键仍使用业务原键，例如 `127.0.0.1:/api/login`；查询缓存均位于 `hnblue:cache:v2:`。

## 验证命令

- `python logs\cache_validation_20260714.py`
- `python -c "<region-overview/16 cache probe>"`
- `docker exec hnblue-redis redis-cli --scan --pattern hnblue:cache:v2:<cache>:*`
- `docker exec hnblue-redis redis-cli TTL <key>`
- `docker exec hnblue-redis redis-cli INFO memory`
- `docker exec hnblue-redis redis-cli DBSIZE`

## 性能结果

| Cache | 冷请求 ms | 热请求 P50 ms | 热请求 P95 ms | 并发 P50 ms | 并发 P95 ms | 键数 | TTL |
|---|---:|---:|---:|---:|---:|---:|---:|
| dashboard-summary | 42.54 | 9.04 | 29.95 | 12.92 | 15.10 | 1 | 600 |
| sources | 49.31 | 9.16 | 32.08 | 11.93 | 17.68 | 1 | 21600 |
| mangrove-cover | 29.80 | 8.73 | 30.15 | 13.25 | 15.76 | 1 | 1800 |
| region-metrics | 25.16 | 8.72 | 30.55 | 13.76 | 16.45 | 1 | 1800 |
| literature-carbon | 16.61 | 7.64 | 32.06 | 13.15 | 17.65 | 1 | 7200 |
| ai-context | 32.19 | 8.34 | 31.39 | 14.01 | 28.48 | 1 | 600 |
| region-overview/16 | 66.56 | 7.45 | 30.95 | 9.14 | 13.38 | 1 | 900 |

## Redis 资源变化

- 验证前 used_memory：900456 bytes
- 验证后 used_memory：945152 bytes
- memory delta：44696 bytes
- 验证前 DBSIZE：2
- 验证后 DBSIZE：7
- DBSIZE delta：5

## 过期与重查

对每个缓存接口的专属键执行 `EXPIRE <key> 1`，等待后重新请求：

- 接口均返回 HTTP 200。
- 缓存键重新生成。
- TTL 恢复为对应配置值。
- 响应内容中的 `generated_at` 会更新，因此完整 JSON 哈希会变化；业务 `success` 与数据结构保持正常。

## 写操作失效

本轮审计到的 `/api/v2/**` 接口为读取接口，没有对应的 v2 写 Controller 可直接挂接 `@CacheEvict`。失效策略如下：

- 后续若新增 v2 写入或导入接口，应在写入成功后按表域删除对应 `hnblue:cache:v2:<cache>:*` 专属键。
- 不得使用 `FLUSHALL`、`FLUSHDB` 或 `KEYS *`。
- 批量导入脚本应只清理受影响 cache name 的命名空间。

## 回归结论

- Redis 查询缓存已接入并通过冷/热/并发/TTL/过期重查验证。
- 登录限流轻量回归通过：错误登录后 Redis 出现 `127.0.0.1:/api/login`，TTL 正常递减。
- AnythingLLM SSE 和前端 AI 页面仍受有效 API Key 未进入当前后端进程、浏览器自动化 runtime 失败阻塞，不影响 Redis 查询缓存验收。
