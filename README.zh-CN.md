# HNBLUE — 海南蓝碳数字化应用系统

[English](README.md) · 简体中文

HNBLUE 是面向海南蓝碳数据治理、区域专题展示、生物量模型推理、治理业务协同和私域知识问答的全栈研究与管理原型。

系统由 Vue 3 前端、Spring Boot 应用与 Agent 编排层、MySQL、Redis、Flask/CatBoost 推理服务和 HNBLUE 自建混合 RAG 组成。DeepSeek 只作为通过检索与证据门控后的可选生成层；未配置生成模型时仍会返回可核验的证据摘录。

> **适用边界：** HNBLUE 用于科研、教学、项目演示和辅助研判，不是官方碳核算、碳信用核证或生产监测平台。模型结果、文献统计、遥感代理指标和 AI 回答在科研或业务使用前都需要专业复核。

## 核心能力

- 建立带区域、年份、指标、质量、代理值、模拟值、引用和许可信息的蓝碳数据目录；
- 提供公开数据中心和治理工作台，展示红树林面积、区域指标、文献证据和工作单；
- 提供海南市县级基础地图、公开边界几何和明确标注的合成指标图层；
- 提供 BAAD、Tallo、ChinAllomeTree、GWM 目录结构的外部数据浏览接口；
- 通过 Spring Boot 统一代理 CatBoost 单条和批量推理；
- 提供虚拟样地、碳价值换算和离线模型解释产物；
- 提供 BM25、字符 TF-IDF、LSA、标题召回、加权 RRF 融合、证据门控、提示注入检测、引用校验、会话隔离和 SSE 输出的 Agentic RAG；
- 使用 Redis 完成热点查询缓存、登录限流和密码重置状态管理，并实现分级工作单流转。

## 这个项目重点展示什么

- **Agent 开发：** 先检索后生成、确定性安全门、有限上下文、可审计引用、生成失败后的摘录回退、按用户隔离的对话记忆。
- **后端开发：** 浏览器只面对一个信任边界；模型、RAG、密钥、缓存、限流和角色工作流都由服务端统一治理。
- **产品思维：** 明确区分观测、文献、代理、模型估算和情景模拟；先展示证据状态，再展示结论；能力不可用时宁可明确降级，也不伪造完整性。
- **公开发布纪律：** 公开代码、架构和合成测试样例；私有知识、授权数据、原始影像、部署包、对话、凭据和运行痕迹全部留在 Git 之外。

## 系统架构

核心 Agent 链路采用纵向分层，明确每一次信任边界转换：

```mermaid
flowchart TB
    A[Vue 3 客户端]
    B[Spring Boot API 与 Agent 编排器]
    C[身份、会话、输入与策略检查]
    D[HNBLUE 混合检索器]
    E[证据充分性与冲突门控]
    F[可选的 DeepSeek 受证据约束生成]
    G[引用校验与摘录式回退]
    H[SSE 返回答案、来源和证据状态]
    A --> B
    B --> C
    C --> D
    D --> E
    E --> F
    F --> G
    G --> H
```

| 层次 | 技术 | 职责 |
|---|---|---|
| 前端 | Vue 3、Vue Router、Element Plus、ECharts、Axios | 路由、表单、地图、图表、表格和 AI 流式展示 |
| 应用层 | Java 17、Spring Boot 3.1.4、JPA、JDBC | 身份、权限、工作单、数据接口、缓存和服务代理 |
| 持久化 | MySQL 8 | 用户、治理记录、来源、区域、指标和结构化事实 |
| 缓存/状态 | Redis | 查询缓存、登录计数、临时封禁和密码重置状态 |
| 模型 | Python、Flask、CatBoost、scikit-learn | 模型加载、单条/批量推理和分析接口 |
| Agent/RAG | Python 检索、Spring 编排、可选 DeepSeek、SSE | 混合检索、策略门控、受约束生成、引用、记忆和流式输出 |

## 三条主要调用链

**普通数据查询**

```text
Vue -> Spring Controller/Service -> Redis 命中直接返回
                               -> 未命中时查询 MySQL -> 回填 Redis
```

**模型推理**

```text
StructurePredictor / VirtualPlotDesigner
  -> /api/model/*
  -> Spring ModelProxyController
  -> Flask
  -> catboost_pipeline.pkl
```

**证据型 Agent 问答**

```text
Vue POST + 流式响应
  -> /api/chat/stream-carbon
  -> 身份/会话/注入攻击检查
  -> 本地混合检索 -> 证据门控
  -> 可选 DeepSeek，仅接收 [S1..Sn] 证据
  -> 引用校验或摘录式回退
  -> status / delta / meta / done 事件
```

## 项目目录

```text
HNBLUE/
├─ frontend/        Vue 应用和公开地图/数据资产
├─ backend/         Spring Boot API、持久化、缓存、安全控制和测试
├─ flask_model/     训练脚本、模型制品和 Flask 推理 API
├─ rag/             混合检索、证据策略、合成知识样例和测试
├─ data/examples/   提交到 Git 的小型合成外部数据包
├─ data/raw/        可选完整数据集；Git 默认忽略
├─ sql/             数据模型、seed 预览和受控导入脚本
├─ scripts/         数据审计、规范化、导入和地理空间工具
├─ docs/            架构、API、验收和交付证据
└─ docs/            架构、Agent、API、产品与公开数据策略
```

## 公开数据策略

公开仓库不会上传私有 RAG 知识、生成索引、完整授权数据、数据库备份、Redis/MySQL 数据卷、对话记录、部署包、压测明细或本机运行痕迹。

`data/examples/` 包含 15 个很小的**合成 CSV**，目录和解析契约与 `ExternalDatasetService` 预期一致。所有行均为演示构造，不是从上述数据集抽取的真实观测，不能用于科研分析、模型评价或碳核算。

取得合法授权的完整数据后，可按以下目录放入已被 Git 忽略的 `data/raw/`：

```text
data/raw/
├─ baad/BAAD_cleaned.csv
├─ tallo/Tallo.csv
├─ chinallometree/
│  ├─ ChinAllomeTree_cleaned.csv
│  ├─ ChinAllomeTree.xlsx - General.csv
│  └─ ChinAllomeTree.xlsx - Equation.csv
└─ gwm/
   ├─ Mangrove_country.csv
   ├─ Mangrove_global.csv
   └─ ... 其余八个生态系统/层级文件
```

取得合法授权的完整数据只保存在私有环境；其行数、样本、派生索引和验证证据均不在公开仓库披露。

## 环境要求

- JDK 17
- Node.js 18+ 与 npm
- Python 3.11
- MySQL 8
- Redis 6+
- 可选：用于受证据约束生成的 DeepSeek API Key；没有 Key 时检索和摘录式回答仍可运行

## 配置

将 `.env.example` 复制到本地的私密配置流程中。Spring Boot 会读取环境变量，但不会自动加载根目录 `.env`；需要由 IDE、PowerShell、Docker Compose 或进程管理器导出变量。

完整联调需要：

| 环境变量 | 用途 |
|---|---|
| `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` | MySQL 连接 |
| `REDIS_HOST`、`REDIS_PORT`、`REDIS_DATABASE` | Redis 连接 |
| `MODEL_API_URL` | Flask 地址，默认 `http://127.0.0.1:8880` |
| `HNBLUE_EXTERNAL_DATA_ROOT` | 完整或样例外部数据根目录 |
| `HNBLUE_JWT_SECRET` | JWT HMAC 密钥，至少 32 个 UTF-8 字节 |
| `HNBLUE_SECRET_ENCRYPTION_KEY` | 管理员保存 API Key 时使用的 32 字节 Base64 加密主密钥 |
| `CORS_ALLOWED_ORIGINS` | 允许访问后端的浏览器来源白名单 |

可选集成：

| 环境变量 | 用途 |
|---|---|
| `HNBLUE_RAG_SOURCE`、`HNBLUE_RAG_CHUNKS` | 私有知识挂载目录和本地生成索引；公开默认只用合成样例 |
| `RAG_API_URL` | Spring Boot 调用的本地 Flask RAG 地址 |
| `DEEPSEEK_API_KEY` | 可选生成模型密钥，只保留在服务端 |
| `MAIL_HOST`、`MAIL_PORT`、`MAIL_USERNAME`、`MAIL_PASSWORD` | 密码重置邮件 |
| `HNBLUE_FIELD_ENCRYPTION_SECRET` | 可选 JPA 字段加密转换器密钥 |

未设置 `HNBLUE_JWT_SECRET` 时，后端会为本地开发生成进程级临时密钥，进程重启后旧 Token 全部失效。共享或部署环境必须设置稳定且安全的密钥。

## 本地运行

### 1. 基础设施

启动 MySQL 和 Redis，并根据已导出的环境变量建立本地数据库和用户。执行 `sql/` 中任何脚本前必须先阅读说明并备份：仓库中同时存在总体设计 DDL、受控导入脚本和迁移执行记录，不能全部对已有数据库重复运行。

### 2. 模型服务

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r flask_model\carbon_model_api\requirements.txt
.\.venv\Scripts\python.exe flask_model\carbon_model_api\app.py
```

公开推理服务默认运行在 `synthetic-demo` 模式，只返回用于前端联调的确定性合成值。私有部署可通过 `HNBLUE_MODEL_PATH` 挂载经过授权和信任的模型制品。Python pickle/joblib 文件加载时可以执行代码，禁止加载不可信文件。

### 3. Spring Boot 后端

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

默认地址：`http://127.0.0.1:8088`。

### 4. Vue 前端

```powershell
cd frontend
npm ci
npm run serve
```

开发代理会把 `/api`、`/user` 和 `/admin` 请求发送给 Spring Boot。

### 5. 构建公开合成 RAG 索引

```powershell
python rag/build_index.py `
  --source rag/examples/knowledge `
  --output rag/artifacts
```

私有部署应把有权使用的知识以只读目录挂载到仓库之外，并通过 `HNBLUE_RAG_SOURCE` 指向该目录。禁止把生产知识或索引复制回 Git。

## 健康检查

```powershell
Invoke-RestMethod http://127.0.0.1:8880/ping
Invoke-RestMethod http://127.0.0.1:8088/api/v2/health
Invoke-RestMethod http://127.0.0.1:8088/api/model/health
```

## 构建与测试

后端：

```powershell
cd backend
.\mvnw.cmd clean test
.\mvnw.cmd clean package -DskipTests
```

前端：

```powershell
cd frontend
npm ci
npm run audit:delivery
npm run build
```

Python 推理环境：

```powershell
python -m pip install -r flask_model/carbon_model_api/requirements.txt
python -m pip check
```

## 模型口径

部署的 CatBoost 管线使用 12 个结构、气候、位置和类别特征。训练代码的目标是 BAAD 字段 `m.so`，即该流程中的地上部干生物量。API 原始预测不能直接等同于已验证的单位面积碳储量；科研使用还要明确面积、林分密度、碳比例、单位、本地校准和不确定性。

本次公开版不发布私有训练行，也不宣称模型在独立海南实测数据上的部署精度。正式科研复现需要另行准备具有合法许可的版本化数据、无泄漏预处理、分组验证、机器可读指标和本地独立验证。

SHAP、敏感性分析、响应曲线和虚拟样地用于解释模型行为，不证明生态因果关系。

## 安全说明

- 禁止提交 API Key、JWT 密钥、数据库/邮件凭据、私有 `.env`、数据库备份和内部材料；
- CORS 默认只允许显式本地来源，部署时应通过 `CORS_ALLOWED_ORIGINS` 设置正式域名；
- Redis 任意读写演示接口和限流演示接口只在 Spring `dev` Profile 下启用；
- MySQL、Redis、Flask 模型/RAG 服务应只监听私网接口，对公网只开放 HTTPS 反向代理；
- 任何曾被复制到源码、日志、截图或 Git 历史中的凭据都应立即轮换；
- 当前认证和 AI 接口适合受控演示，未经安全复核不应直接作为公网生产系统。

## 验证证据和限制

公开验证由合成样例、自动化测试和 [`docs/PUBLIC_DATA_POLICY.md`](docs/PUBLIC_DATA_POLICY.md) 中的边界说明构成；私有运行证据不进入 Git。

当前边界包括：公开版主动排除了真实样地、通量、UAV 和遥感数据；生成模型可用性依赖环境；部分科研分析页面尚未形成完整公开交互闭环。

## 许可证与第三方材料

项目源代码采用 [Apache License 2.0](LICENSE)。第三方数据、论文、图片、地图边界和模型制品仍遵循各自的许可和署名要求；仓库的软件许可证不会自动授予外部数据的再分发权。

## 文档导航

- [系统架构](docs/ARCHITECTURE.md)
- [Agentic RAG 设计](docs/AGENTIC_RAG.md)
- [产品需求与设计决策](docs/PRODUCT_REQUIREMENTS.md)
- [公开数据与知识策略](docs/PUBLIC_DATA_POLICY.md)
- [API 说明](docs/API.md)
