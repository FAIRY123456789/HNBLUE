# HNBLUE API 参考

本文以当前 Spring Boot 与 Flask 代码为准。默认 Spring Boot 地址为 `http://127.0.0.1:8088`，Flask 内部地址为 `http://127.0.0.1:8880`。

## 1. 约定与安全边界

- 浏览器应只访问 Spring Boot；模型与本地 RAG 服务由 Spring Boot 代理。
- 需要身份的接口使用 `Authorization: Bearer <JWT>`。
- 当前代码没有统一的全局响应包：不同控制器会返回业务对象、`{data: ...}`、`{success: ...}` 或错误字符串，调用方应按具体接口处理。
- 当前也没有覆盖全部路由的统一认证过滤链。用户、管理员和工作单接口在控制器中校验 Token；部分查询、模型和 AI 接口面向受控演示环境开放。
- 查询参数和中文路径值应使用 UTF-8 URL 编码。

## 2. 健康检查

### `GET /api/v2/health`

检查 Spring 数据服务状态。返回 `V2DataService` 汇总的状态对象。

### `GET /api/model/health`

由 Spring Boot 请求 Flask `/ping`。即使上游不可用也返回 HTTP 200，并通过 `ok: false` 表达不可用，便于前端保持参数可编辑状态。

```json
{"ok": true, "message": "模型服务已连接", "upstream": {"msg": "pong"}}
```

## 3. 登录、注册与用户

### `POST /api/login`

请求：

```json
{"username": "demo-user", "password": "local-password"}
```

登录接口使用 Redis 按客户端地址和路径做限流：3 秒固定窗口最多 10 次，超限临时封禁 60 秒并返回 HTTP 429。Redis 不可用时当前实现降级放行。

### `POST /api/register`

请求字段：`username`、`password`、`email`、`birthdate`。

### 密码重置

- `POST /api/initiateReset`：发起重置，请求包含 `username`、`email` 与 `newPassword`。
- `GET /api/verifyToken?token=...&newPassword=...`：校验邮件 Token，并使用 Base64 编码的新密码执行当前重置流程。
- `GET /api/checkResetStatus?username=...`：查询短期重置状态。

当前密码重置流程适合受控演示，部署到公网前应改为一次性短期 Token + HTTPS 表单提交，不能在 URL 中传递新密码。

### 用户接口

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/user/info` | 获取当前用户信息 |
| POST | `/user/avatar` | 上传头像；`multipart/form-data` |
| PUT | `/user/updateInfo` | 修改当前用户资料 |

以上接口需要 Bearer Token。

## 4. 管理员接口

基础路径为 `/admin`，控制器会验证 Token 和管理员身份。

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/admin/users?page=0&size=10` | 分页列出用户 |
| GET | `/admin/search?page=0&size=10&keyword=...` | 搜索用户 |
| GET | `/admin/users/{id}` | 用户详情 |
| POST | `/admin/add` | 新增用户 |
| PUT | `/admin/update` | 更新用户 |
| DELETE | `/admin/delete/{id}` | 删除单个用户 |
| DELETE | `/admin/delete` | 按 JSON ID 数组批量删除 |

`GET /admin/hello` 是连通性演示，不代表权限能力。

## 5. 数据中心与区域查询

### `GET /api/v2/dashboard/summary`

返回仪表盘汇总。Redis 缓存 10 分钟。

### 可筛选数据接口

| 方法 | 路径 | Redis TTL |
|---|---|---:|
| GET | `/api/v2/sources` | 6 小时 |
| GET | `/api/v2/mangrove-cover` | 30 分钟 |
| GET | `/api/v2/region-metrics` | 30 分钟 |
| GET | `/api/v2/literature-carbon` | 2 小时 |
| GET | `/api/v2/ai/context` | 10 分钟 |

查询参数会作为筛选条件传给 `V2DataService`。前端常用区域、年份、指标和来源等参数；无效筛选应根据响应中的 `success`/错误字段判断。

### `GET /api/v2/region-overview/{regionId}`

返回某地区汇总，Redis 缓存 15 分钟。

### `GET /api/v2/optional/status`

返回可选数据/服务的可用状态。

### `GET /api/devisual/{regionName}`

聚合某个合法海南市县的九类区域数据：基础信息、碳趋势、通量组成、多碳指标、年度趋势、生态分区、影响因子、物种组成和经济预测。非法区域返回 HTTP 400。

### 可视化指标

- `GET /api/indicators/current`：读取当前年度指标。
- `PUT /api/indicators/update?region=...&indicator=...&value=...`：更新指标。

`/api/indicators/update` 当前没有控制器级身份校验，只能在受控环境使用；公网部署前必须纳入管理员授权。

## 6. 外部数据浏览

基础路径：`/api/v2/external-datasets`。

### `GET /api/v2/external-datasets`

列出 `baad`、`tallo`、`chinallometree`、`gwm` 四个数据包及运行时记录数。服务优先读取数据库中已确认的表；否则读取本地 CSV。

### `GET /api/v2/external-datasets/{datasetId}/tables`

返回该数据包的表、字段数、记录数和实际来源类型。

### `GET /api/v2/external-datasets/{datasetId}/schema`

参数：

- `tableName`：必填；表 ID、运行时表名或文件名；
- `page`：默认 1；
- `size`：允许 20、50、100；其他值归一化为 20；
- `keyword`：可选字段字典搜索。

### `GET /api/v2/external-datasets/{datasetId}/records`

参数：`tableName`、`page=1`、`size=20`、可选 `keyword`、`sortField`、`sortDirection=asc|desc`。

服务只接受实际 Schema 中的安全字段名，拒绝密码、Token、密钥等敏感字段名；CSV 结果在服务端分页，前端不会一次加载完整文件。

公开仓库默认回退到 `data/examples/` 的合成数据。完整数据可通过 `HNBLUE_EXTERNAL_DATA_ROOT` 指向本地授权目录。

## 7. 工作单

所有工作单接口都需要 Bearer Token。

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/work-orders` | 按当前身份列出可见工作单 |
| GET | `/api/work-orders/{id}` | 详情及动作日志 |
| POST | `/api/work-orders` | 创建工作单 |
| PUT | `/api/work-orders/{id}` | 更新非终态工作单 |
| POST | `/api/work-orders/{id}/accept` | 管理员受理 |
| POST | `/api/work-orders/{id}/submit-approval` | 二级管理员提交终审 |
| POST | `/api/work-orders/{id}/approve` | 一级管理员批准 |
| POST | `/api/work-orders/{id}/reject` | 一级管理员驳回 |
| POST | `/api/work-orders/{id}/return` | 一级管理员退回 |
| POST | `/api/work-orders/{id}/close` | 一级管理员关闭 |
| POST | `/api/work-orders/{id}/comments` | 有查看权限的参与者评论 |

创建/更新主体字段包括 `code`、`title`、`changeType`、`tableType`、`sourceCode`、`reason`、`payload`、`notes`、`assignedAdminName`、`testCode`。创建时 `title`、`changeType`、`tableType` 必填。

动作请求可传：

```json
{"comment": "审核说明"}
```

主要状态：`SUBMITTED`、`PROCESSING`、`PENDING_APPROVAL`、`APPROVED`、`REJECTED`、`RETURNED`、`CLOSED`。非法角色、不可见记录和非法状态跳转分别返回 403、404/403 或 409。

## 8. CatBoost 模型代理

### `POST /api/model/predict-carbon`

Spring Boot 将请求转发到 Flask `/api/predict-carbon`。

```json
{
  "latitude": 19.0,
  "longitude": 109.5,
  "mat": 25.5,
  "map": 1800,
  "age": 25,
  "treeHeight": 8.2,
  "dbh": 16.5,
  "canopy": 4.1,
  "c.d": 0.7,
  "vegetation": "mangrove",
  "growingcondition": "natural",
  "pft": "broadleaf"
}
```

`treeHeight`、`dbh`、`canopy` 在当前实现中必须存在且可转换为数字；其余字段有默认值。成功响应：

```json
{"prediction": 123.456}
```

预测值是模型训练目标 `m.so` 的输出，不应在缺少面积、密度、碳比例和单位换算时直接解释为 `tC/ha`。

### `POST /api/model/predict-carbon/batch`

请求主体是上述对象的 JSON 数组；成功响应为 `{"predictions": [...]}`。上游不可用时 Spring Boot 返回 HTTP 502。

Flask 还实现 `/api/literature-range`、`/api/sensitivity-analysis`、`/api/sensitivity-plot` 和 `/api/sensitivity-explain`，用于内部模型分析；当前 Spring Boot 没有代理这些路由，因此不属于推荐的浏览器公开 API。`HNBLUE_BAAD_FILE` 可指定文献统计 CSV，默认使用合成样例。

## 9. AI 碳助手 SSE

### `POST /api/chat/stream-carbon`

- 请求：`{"message":"问题","sessionId":"浏览器会话标识"}`；
- `message` 必填，最长 4000 字；
- `sessionId` 会被规范化，未提供时由服务端生成；
- `Authorization` 可选；登录用户按 JWT 绑定历史，匿名用户按不可预测会话标识隔离；
- 响应类型：`text/event-stream`；
- 缓存：`no-cache`，并设置 `X-Accel-Buffering: no` 防止反向代理缓冲。

前端使用 `fetch` 读取流式 JSON 数据事件。事件对象的 `type` 可能为：

| type | 含义 |
|---|---|
| `status` | 连接、检索或分析阶段提示 |
| `delta` | 可直接追加到界面的可见文本增量 |
| `meta` | 会话或来源元数据 |
| `done` | 正常结束 |
| `error` | 上游连接或解析错误 |

Spring 先调用本地 `/api/rag/context`，再根据证据状态决定拒答、摘录式回答或可选 DeepSeek 生成。生成模型只能引用本轮提供的 `[S1]...[Sn]`；未知引用或无引用事实会触发摘录式回退。`DEEPSEEK_API_KEY` 只存在服务端，私有知识与索引分别由 `HNBLUE_RAG_SOURCE`、`HNBLUE_RAG_CHUNKS` 指向仓库外目录。

`GET /api/chat/stream-carbon?message=...&sessionId=...` 是只使用本地摘录回答的兼容入口；新页面使用 POST。`GET /api/chat/history` 恢复当前身份/会话历史，`DELETE /api/chat/history` 只清理当前身份/会话。

当前 Agent 已实现消息长度限制、会话隔离、提示注入检测、证据门控和引用校验；匿名访问仍被允许。受控演示以外的部署还应补充统一鉴权、并发/配额控制、入口限流、保留期清理和最小化安全审计。

## 10. 开发 Profile 接口

以下接口只在 Spring `dev` Profile 下注册，不应部署到公网：

- `GET /setRedisData?key=...&value=...`
- `GET /getRedisData?key=...`
- `GET /hello`
- `GET /access/accessLimit`

## 11. 代码入口

- Spring 控制器：`backend/src/main/java/com/example/jpaspringboot/controller/`
- 数据与缓存：`backend/src/main/java/com/example/jpaspringboot/service/V2DataService.java`
- 外部数据：`backend/src/main/java/com/example/jpaspringboot/service/ExternalDatasetService.java`
- Flask：`flask_model/carbon_model_api/app.py`
- 前端 API 封装：`frontend/src/api/`
