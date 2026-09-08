# HNBLUE 本地运行基线验收台账

更新时间：2026-07-14
角色：中央本地联调管理员

## 状态说明
- PENDING：待实施或待验证。
- IMPLEMENTED：代码或配置已完成，等待真实运行验证。
- VERIFIED：已有真实命令、接口响应、页面截图或浏览器运行结果。
- BLOCKED：受本机环境、服务、凭据或外部状态阻塞，并记录证据。

## 本轮边界
仅处理本地版本；不编写云部署、生产 Docker Compose、云端 Nginx、域名 HTTPS、阿里云安全组、云端数据库/Redis/AnythingLLM 配置，也不在云端分支编写实际代码。

## 任务台账

| 编号 | 任务 | 状态 | 证据 / 备注 |
|---|---|---|---|
| NAV-01 | 导航栏严格居中 | VERIFIED | 本地 Edge 在 1440px、1920px 下分别验证访客、普通用户、管理员；三种状态 `.nav-links` 中心坐标完全一致，相对布局视口中心偏差 0，左右区域无重叠。768px 管理员状态 `scrollWidth=clientWidth=768`、无溢出；截图位于 `docs/context/evidence/nav-*.png`。 |
| REDIS-01 | Redis 现有代码与配置审计 | IMPLEMENTED | 已执行 `rg` 搜索 `backend/frontend/src/docs/redis_cluster/scripts`；确认旧集群仅来自 `application.properties` 自动配置，Redis 业务入口包括 `AccessLimitInterceptor`、`LoginController#initiateReset/checkResetStatus`、`RedisController`、`RedisUtil`、`RedisConfig`。 |
| REDIS-02 | 旧六节点集群退出运行配置 | IMPLEMENTED | 已从运行配置移除 `spring.data.redis.cluster.*` 和 `192.168.204.128:6379-6384`；旧 `redis_cluster` 已移动到 `docs/archive/redis-cluster-legacy/redis_cluster`。 |
| REDIS-03 | 本机单节点 Redis 部署 | VERIFIED | Docker Desktop 4.81.0 / Engine 29.6.1；`hnblue-redis` 使用 `redis:6.2.7`，仅绑定 `127.0.0.1:6379`，`unless-stopped`，挂载 `hnblue_redis_data:/data`。实际返回 `PONG`、`redis_mode:standalone`、`role:master`、`connected_slaves:0`。 |
| REDIS-04 | Spring Boot single-node Redis config | VERIFIED | 2026-07-13 CMD evidence: application.properties uses spring.data.redis.host=${REDIS_HOST:127.0.0.1}, port ${REDIS_PORT:6379}; Spring Boot jar listened on 8088; /setRedisData?key=hnblue:final:redis&value=ok then /getRedisData returned hnblue:final:redis --->>> ok; log search found no Access denied, 192.168.204, or RedisCluster matches. |
| REDIS-05 | Login rate-limit validation | VERIFIED | 2026-07-13 CMD/Python 100-request probe wrote docs/context/evidence/login-rate-limit-100.csv without credentials/tokens. Result: total 100; status counts 200=10, 401=30, 429=60; first 429 at request 11; Redis key 127.0.0.1:/api/login reached count 10 and ban key TTL 59; recovery admin login after cleanup returned 200. |
| REDIS-06 | Password-reset state validation | BLOCKED | Safe failure path verified: POST /api/initiateReset for non-existing probe user returned 404 and checkResetStatus returned 401. Real initiateReset for an existing user is BLOCKED because it sends email and writes reset state for a real account. Isolated RedisTemplate read verified by writing resetPassword:reset_probe_fake through /setRedisData and checkResetStatus returned 200 in progress; direct redis-cli string caused serializer 500, so business keys must be written by Spring RedisTemplate. |
| REDIS-07 | Personal-center Redis dependency validation | VERIFIED | 2026-07-14 CMD/Python probe: zhangsansan login returned 200 with User token; /user/info returned id/name/email/birthdate/avatar/role/lastLoginAt; temporary birthdate update returned 200 and was restored to the original value. No password, role, username or account status was modified. |
| REDIS-08 | User-management Redis dependency validation | VERIFIED | 2026-07-13 CMD/Python probe: admin candidate Admin_100000 login returned 200 with admin role token; /admin/users?page=0&size=5 returned 200 with total=1000,count=5; /admin/search returned 200 with total=996,count=5; /admin/users/{id} returned 200 with user detail and logCount=0; normal/no-auth admin attempts returned 401. No password hash/sensitive credential field/token values were queried or printed. |
| REDIS-09 | Redis 数据持久化与重启验证 | VERIFIED | `CONFIG GET appendonly` 返回 `yes`；volume 为 `hnblue_redis_data`；写入健康测试键后重启容器，再次读取返回 `ok`，测试键已删除。 |
| MODEL-01 | Python 虚拟环境修复 | VERIFIED | `.venv/Scripts/python.exe --version` 返回 Python 3.11.3；模型在该虚拟环境内成功加载，未使用全局包替代。 |
| MODEL-02 | 模型依赖版本锁定 | VERIFIED | `requirements.txt` 已锁定 scikit-learn 1.6.1 及实际可运行组合；`pip install -r requirements.txt` 全部满足，`pip check` 返回 `No broken requirements found`。 |
| MODEL-03 | CatBoost 单条预测验证 | VERIFIED | 直接 Python 调用与 `POST /api/predict-carbon` 均返回有限数值 `5.69`。 |
| MODEL-04 | CatBoost 批量预测验证 | VERIFIED | `POST /api/predict-carbon/batch` 返回 `[5.69,5.126]`；无 NaN、Infinity 或空字符串。 |
| MODEL-05 | Spring Boot model proxy validation | VERIFIED | 2026-07-13 CMD: GET /api/model/health returned ok=true and upstream pong; POST /api/model/predict-carbon with treeHeight=12, dbh=18, canopy=4.5 returned prediction 5.69; Flask direct POST returned 5.69; batch proxy returned predictions [5.69,6.122]. |
| MODEL-06 | 结构参数页面端到端验证 | BLOCKED | 2026-07-14 CMD evidence: Flask direct prediction and Spring Boot proxy with treeHeight=12, dbh=18, canopy=4.5 both returned prediction 5.69 and repeated 3 times stably. Browser E2E was not completed because browser automation runtime failed with Windows sandbox helper error and AI SSE has no visible output without current-process ANYTHINGLLM_API_KEY. |
| MODEL-07 | 虚拟样地页面端到端验证 | BLOCKED | 2026-07-14 CMD evidence: Spring Boot batch proxy returned real predictions [5.69, 5.337, 5.69]. Browser E2E was not completed because browser automation runtime failed with Windows sandbox helper error and AI SSE remains blocked by missing current-process API key. |
| AI-01 | AnythingLLM 本地连接验证 | BLOCKED | 2026-07-14 CMD/Python probe: http://127.0.0.1:3001/api/docs and /api/docs/ returned 200, and wrong API key against /api/v1/workspaces returned 403 No valid api key found. Current Codex/Spring restart process has API Key 未配置, so valid-key workspace/query/stream verification remains blocked. |
| AI-02 | AI 碳助手 SSE 验证 | BLOCKED | 2026-07-14 CMD/Python SSE probe: /api/chat/stream-carbon returned HTTP 200 with 3 events, types=[status,error,done], first fragment 97.49ms, total 163.8ms, visibleChars=0, sensitiveFieldSeen=false. No multi-delta answer because current backend process has no valid AnythingLLM key. |
| AI-03 | 实时 Markdown 渲染验证 | BLOCKED | Browser automation runtime failed twice with Windows sandbox helper error before page interaction. Backend SSE currently emits no visible Markdown content because valid AnythingLLM key is not available to the backend process. |
| AI-04 | 模型结果 AI 解释验证 | BLOCKED | 2026-07-14 CMD evidence: model result explanation request containing prediction=5.69 reached /api/chat/stream-carbon and returned status,error,done with visibleChars=0. CatBoost/Spring proxy is verified; AI explanation is blocked only by missing current-process AnythingLLM key. |
| AUTH-01 | Normal-user login validation | VERIFIED | 2026-07-14 CMD/Python probe: zhangsansan with the corrected password returned 200, userType=User and a JWT. /user/info showed lastLoginAt present after login. The token itself was not printed or written to evidence. |
| AUTH-02 | 个人中心验证 | VERIFIED | 2026-07-14 API validation: /user/info returned real user fields; /user/updateInfo updated birthdate to a temporary value and then restored the original birthdate; no sensitive fields were changed. Frontend fixes align Login.vue, useAuth.js and UserInfo.vue with this response shape. |
| AUTH-03 | 退出登录验证 | VERIFIED | 2026-07-14 API/Frontend validation: useAuth.logout now clears token, role and cached userInfo; GET /user/info without Authorization now returns 401 after UserController header fix. Stateless old JWT server-side invalidation is not implemented by this app. |
| ADMIN-01 | Admin role audit | VERIFIED | 2026-07-13 read-only DB evidence: admin table exists with 3 records (Admin_100000, Admin_100001, Admin_100002); user table exists with 1009 records; user_activity_log exists with 2 records. Controller audit confirms requireAdmin(token) checks adminRepository.findByName(username). No sensitive credential fields were queried or printed. |
| ADMIN-02 | 用户管理验证 | VERIFIED | 2026-07-14 CMD/Python regression: Admin_100000 login returned 200; /admin/users?page=0&size=10 returned count=10,total=1000; /admin/search keyword=zhang returned count=1,total=1; /admin/users/{id} returned 200 with user detail. UserManage.vue now normalizes content/users/records response shapes and shows loading/error states. |
| TEST-01 | 前端构建 | VERIFIED | `npm run audit:delivery` 通过；`npm run build` 成功，仅有包体积和 Browserslist 数据陈旧警告。 |
| TEST-02 | Backend compile and tests | VERIFIED | 2026-07-14 CMD: mvn clean test with process-only DB env completed BUILD SUCCESS; Results: Tests run 34, Failures 0, Errors 0, Skipped 0. mvn clean package -DskipTests also BUILD SUCCESS and rebuilt backend/target/JPAspringboot-0.0.1-SNAPSHOT.jar. |
| TEST-03 | Local complete startup | VERIFIED | 2026-07-14 CMD/Python evidence: Docker Engine 29.6.1 available; hnblue-redis Up with 127.0.0.1:6379 and PONG; AnythingLLM docs on 3001 returned 200; Flask /ping returned 200; Spring Boot 8088 /api/v2/health and /api/model/health returned 200; project Vue dev server runs on 8090 because port 8080 is occupied by a non-Vue Java process returning 404 at /. |
| TEST-04 | 浏览器端到端验收 | BLOCKED | Previous navigation screenshots remain verified. 2026-07-14 current browser automation could not start: node_repl browser runtime exited twice with Windows sandbox helper_unknown_error. API-level regression for zhangsansan, Admin_100000, model proxy and cache succeeded; visual page screenshots were not produced in this pass. |
| GIT-01 | 敏感信息审计 | VERIFIED | 脱敏扫描未发现工作区真实密钥值；已移除硬编码测试密码，实体 `toString` 不再输出 sensitive credential field/hash；生成产物已清理。Git 基线历史中的数据库、邮箱和第三方 API 配置曾含值，必须轮换，未输出值、未改写历史。 |
| GIT-02 | 本地稳定版提交 | BLOCKED | 未满足提交前置条件：Redis、后端、Flask、模型代理、页面验收均未 VERIFIED。 |
| GIT-03 | 推送 GitHub | BLOCKED | 未创建本地稳定提交，禁止推送。 |
| GIT-04 | 创建云端开发分支 | BLOCKED | GitHub 推送未完成，禁止创建云端开发分支。 |

## Redis 依赖矩阵

| 文件 | 类/方法 | Redis 用途 | 关键业务 | 当前配置 | 本轮处理 |
|---|---|---|---|---|---|
| `backend/src/main/resources/application.properties` | Spring Boot 自动配置 | Redis 连接 | 全局 RedisTemplate、缓存、限流、密码重置 | 已从旧 Cluster 改为 `127.0.0.1:6379` | 容器 VERIFIED；应用启动被 MySQL 凭据 BLOCKED |
| `backend/src/main/java/com/example/jpaspringboot/config/RedisConfig.java` | `redisTemplate`、`cacheManager` | 序列化和缓存管理 | RedisTemplate 注入、Spring Cache | 使用 Boot 注入的 `RedisConnectionFactory` | 保持兼容，不新增集群工厂 |
| `backend/src/main/java/com/example/jpaspringboot/util/RedisUtil.java` | `get/set/expire/incr` 等 | 统一 Redis 操作 | 限流、密码重置、调试接口 | 注入 `RedisTemplate<String,Object>` | 保持键名和序列化方式 |
| `backend/src/main/java/com/example/jpaspringboot/interceptor/AccessLimitInterceptor.java` | `preHandle` | Redis 计数、ban key、TTL | 登录限流、接口限流 | 使用 `RedisUtil` | 待 Redis 可用后验证 429 与 TTL |
| `backend/src/main/java/com/example/jpaspringboot/controller/LoginController.java` | `initiateReset`、`checkResetStatus` | `resetPassword:{username}` 状态和 TTL | 密码重置申请、状态查询 | 使用 `RedisUtil` | 待 Redis 可用后验证状态和 TTL |
| `backend/src/main/java/com/example/jpaspringboot/controller/RedisController.java` | `/setRedisData`、`/getRedisData` | Redis 测试读写 | 调试接口 | 使用 `RedisTemplate` | 保留，待本地验证 |
| `frontend/src/components/common/UnifiedNav.vue` | 导航角色显示 | 不直接依赖 Redis | 登录态、个人中心、用户管理入口 | 前端 localStorage/JWT | 导航布局已改，业务逻辑未改 |
| `docs/archive/redis-cluster-legacy/redis_cluster` | 旧脚本和配置 | 旧六节点集群参考 | 历史资产 | 已归档，不参与启动 | 不作为本地运行配置 |

## 当前阻塞

1. MySQL blocker resolved on 2026-07-13: localhost:3306 and 127.0.0.1:3306 both connect as root@localhost to the same MySQL 8.0.30 instance; target DB hnblue_v2_dev_control is readable. Password was injected only into current CMD processes and was not written to tracked files.
2. Normal-user protected browser flows remain blocked because the provided normal-user credential returned 401; admin login and user-management APIs are now verified with a real admin account. No real profile/password/email/permission data was modified.
3. AnythingLLM localhost:3001 is listening and /api/docs/ works, but API/SSE validation remains blocked because the current Spring Boot process has no ANYTHINGLLM_API_KEY and extracting the stored local key was blocked by security review.
4. BAAD_cleaned.csv is still missing; only Flask /api/literature-range is blocked by that file.
5. Git commit/push/cloud branch remain out of scope for this round by user instruction; dirty worktree and diff-check cleanup are deferred.

## 阶段记录

### 2026-07-12
- 已建立中央本地联调管理员台账。
- 导航栏在 1440px、1920px 的访客/普通用户/管理员状态和 768px 窄屏下均完成真实浏览器验收。
- 已完成 Redis 源码与配置审计。
- 已退出旧 VMware 六节点 Redis Cluster 运行配置，并归档旧目录。
- 已将 Spring Boot 本地 Redis 配置改为单节点 `127.0.0.1:6379`。
- 已清理配置文件中的默认敏感密钥值。
- Redis 6.2.7 standalone、AOF、volume 与重启持久化 VERIFIED。
- Flask Python 3.11.3 虚拟环境和锁定依赖 VERIFIED；单条、批量与敏感性接口返回有限数值，负波动范围缺陷已修复。
- 前端交付审计与构建 VERIFIED；后端打包成功，完整测试因 MySQL 凭据失效 BLOCKED。
- AnythingLLM 3001 未运行，AI 链路 BLOCKED。
- 敏感信息工作区审计 VERIFIED；历史配置值需轮换。因验收门槛未满足，未提交、未推送、未创建云端分支。

### 2026-07-13
- MySQL verified via CMD mysql client: localhost and 127.0.0.1 both returned VERSION 8.0.30, CURRENT_USER root@localhost, same server_uuid, and database hnblue_v2_dev_control.
- Spring Boot restored on 8088 with process-only DB_PASSWORD; HikariPool started; /api/v2/health returned database readable and core table counts.
- Redis business checks: Spring RedisTemplate read/write succeeded; 100-login probe produced 200 x10, 401 x30, 429 x60, first 429 at request 11, with Redis count and ban TTL evidence saved to docs/context/evidence/login-rate-limit-100.csv.
- Password reset real initiateReset remains BLOCKED because it sends email and writes reset state for an existing user; safe failure path and RedisTemplate status read were verified.
- Real account checks: provided normal user exists but login returned 401 without updating last_login_at/activity_log; Admin_100000 login succeeded and admin list/search/detail APIs returned 200.
- AnythingLLM checks: 3001 is listening and /api/docs/ returns 200; direct workspace API returns 403 without a key, and API/SSE/front AI remain blocked pending an approved temporary key.
- Model proxy verified: /api/model/health ok=true; single prediction 5.69 matched Flask direct result; batch proxy returned [5.69,6.122].
- Backend tests verified: mvn test BUILD SUCCESS, 34 run, 0 failures, 0 errors, 0 skipped; mvn package -DskipTests BUILD SUCCESS.


### 2026-07-14
- 普通用户 zhangsansan 使用修正后的口令登录成功：/api/login 返回 200、userType=User、JWT 存在但未输出。
- 个人中心接口修复并验证：UserInfoDTO 增加 id、username、role、lastLoginAt；/user/info 返回真实字段；生日临时修改后已恢复原值。
- 前端会话修复：Login.vue 改为 useAuth.login；useAuth.logout 现在会清空 token、role、userInfo；UserInfo.vue 增加字段归一化；UserManage.vue 增加分页响应归一化、加载和错误状态。
- 用户管理回归：Admin_100000 登录成功，/admin/users 返回 count=10,total=1000，/admin/search keyword=zhang 返回 count=1,total=1，详情接口返回 200。
- 管理关系语义：后端 /admin/users 按 administrator_user_relation 关系表返回当前管理员关联用户；AdministratorUserRelationServiceImpl#findUsersByAdmin 已从错误的 null 返回修复为真实 users。
- AnythingLLM 阻塞：3001 未监听，常见本地可执行文件路径和本地 DB 路径未找到；application.properties 中 anythingllm.api.key 是环境变量占位符。未进入 AI/SSE 和 Redis 查询缓存阶段。
- Docker/Redis 环境阻塞：com.docker.service stopped，sc start 返回 Access denied，docker start hnblue-redis 无法连接 Docker daemon；本轮无法继续 Redis 查询缓存实施与验证。
- 回归：mvn test 在注入 DB_PASSWORD 后通过；mvn package -DskipTests 通过；npm run audit:delivery 通过；npm run build 通过，仅原有包体积和 Browserslist 警告；pip check 通过。


## 2026-07-14 补充联调与 Redis 查询缓存验收

- REDIS 查询缓存：已在 Service 层为 /api/v2/dashboard/summary、/api/v2/sources、/api/v2/mangrove-cover、/api/v2/region-metrics、/api/v2/literature-carbon、/api/v2/region-overview/{regionId}、/api/v2/ai/context 接入 Spring Cache + RedisCacheManager，统一前缀 hnblue:cache:v2:，禁用空值缓存。
- 缓存验证命令：python logs\cache_validation_20260714.py；6 个通用接口全部完成冷请求、20 次热请求、20 并发、SCAN 键检查、TTL、EXPIRE 后重查和 Redis 内存变化验证；
egion-overview/16 以单独命令完成同等验证。
- 缓存关键结果：Redis memory 900456 -> 945152，增量 44696 bytes；DBSIZE 2 -> 7，增量 5；各接口热请求 P50 均约 7.45-9.16ms。
- 登录限流轻量回归：一次错误登录返回 401，SCAN --pattern *login* 发现 127.0.0.1:/api/login，TTL 样本为 3 秒；未重复 100 次压力测试。
- AnythingLLM 当前状态：3001 文档页可达，错误 Key 返回 403；有效 Key 未进入当前 Codex/Spring 重启进程，因此直连问答、Markdown 流、前端 AI 页面和模型结果 AI 解释均保持 BLOCKED。
- 前端当前状态：8080 被非 Vue Java 进程占用；本项目 Vue CLI 已在 8090 启动并返回首页。浏览器自动化 runtime 因 Windows sandbox helper_unknown_error 失败，未能保存本轮截图。
- 最终回归：mvn clean test 34/0/0/0 通过；mvn clean package -DskipTests 通过；
pm run audit:delivery 通过；
pm run build 通过但有包体积和 Browserslist 陈旧警告；.venv\Scripts\python.exe -m pip check 返回 No broken requirements found。

## 2026-07-16 ?????????????????

- ???`LoginController.register` ?? `UserRepository.findByEmail(...)` ???????????? `hnblue_v2_dev_control.user` ????????JPA ?????????? `IncorrectResultSizeDataAccessException`?
- ??????`user` ??? 1020????? 1020???? 0??????? 0?? `LOWER(TRIM(email))` ?????? 105 ??218 ???????/????? 0??????? 0?
- ????`user_email_dedup_backup_20260716`????? `user_id`?`username`?`original_email`?`normalized_email`?`backup_time`????? 117 ??
- ???????? `TRIM + LOWER`????????? `user_id` ???????????? `_u<user_id>` ??????????????????????? salt??? token?
- ?????????? 117 ?????????????? 0???? 0??????? 0?`user` ????????
- ???????? `uk_user_email(email)` ? `uk_user_username(name)`?
- ??????`sql/20260716_user_email_dedup_and_constraints.sql`??????????? `user_id` ?? `original_email`?????????????
- ??????????? `existsByName` ? `existsByEmailIgnoreCase`?????????????????????? 400 ?????? SQL??????????
- ?????????????????????????? 8 ?????????????????????????????????/???????????????????????????????????????
- ???????`/api/register` ?? `2002/09/07` ???? `2002-09-07`?????? 200?????? 200?????? 400 `??????`??????????????????? 400 `?????`????????????? 400 ??????????
- ?????6 ?????/??????????1 ????5 ?? 400 ???
- ???????????????????? `Admin_100000` ??????????????????????????????????????????????
- ???????`zhangsansan` ? `zhangsan` ?????????????`test_user100002` ????????????????????
- ?????`mvn clean test` ???34 tests?0 failures?0 errors?`mvn clean package -DskipTests` ???`npm run build` ????????????? Browserslist ?????
- ???????AUTH-01=VERIFIED?ADMIN-01=VERIFIED?TEST-02=VERIFIED?????????????
## 2026-07-16 市县级地图与遥感影像资产接入

- 已新增海南19个市县级行政单元 profile、来源目录和前端静态地图资产。
- 已接入四张已核验的公开论文图件：清澜港 UAV 高光谱研究区图、海南红树林冠层高度图、地上生物量图和野外调查场景。
- 数据资产页新增“无人机遥感与野外调查影像”专题，地图页新增省级/市县级切换和右侧真实资料面板。
- 图件均按 CC BY 4.0 展示署名，制图产品不作为实时监测或正式碳核算结果。
