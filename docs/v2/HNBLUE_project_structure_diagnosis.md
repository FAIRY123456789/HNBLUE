# HNBLUE 项目结构诊断

生成时间：2026-05-27

## 诊断结论

当前仓库是一个多服务 HNBLUE 研究原型工程，包含 Vue 前端、Spring Boot 后端、Flask 碳储模型服务、Redis Cluster 配置、SQL schema、公开数据结果包与文档资产。HNBLUE V2.0 公开数据展示页适合先作为静态专题页接入前端，直接读取 normalized JSON/CSV；后续再映射到 Spring Boot 的区域详情接口与 V2.0 数据库表。

关键边界：

- 本轮只做结构诊断与集成方案，不修改前端、后端、Flask、Redis、AnythingLLM。
- 旧库 `hnblue` 仍由 Spring Boot 配置引用，不作为 V2.0 公开数据写入目标。
- `data/results/hnblue_v2_public/normalized/` 已具备展示级 JSON/CSV，可支撑无数据库静态展示。

## 顶层目录职责

| 目录/文件 | 角色 | 证据 |
|---|---|---|
| `frontend/` | Vue 3 + ECharts 前端工程 | `frontend/package.json` 包含 `vue@3.5.13`、`vue-router@4.5.0`、`echarts@5.6.0`、`element-plus`；脚本为 `vue-cli-service serve/build` |
| `backend/` | Spring Boot 后端服务 | `backend/pom.xml` 使用 Spring Boot `3.1.4`、Java 17、JPA、Redis、MySQL、JWT、springdoc |
| `flask_model/` | Python 碳储模型与文献范围接口 | `flask_model/carbon_model_api/app.py` 暴露 `/api/predict-carbon`、`/api/literature-range`、`/api/sensitivity-analysis` 等接口 |
| `redis_cluster/` | Redis Cluster 配置与启动脚本 | 顶层包含 6379-6384 节点配置与 shell 脚本 |
| `sql/` | 数据库 schema 与种子 SQL | 存在 `sql/schema/hnblue_v2_schema.sql` 和 HNBLUE 相关种子脚本 |
| `data/results/hnblue_v2_public/` | HNBLUE V2.0 公开数据结果包 | 包含原始结果 CSV、manifest、`normalized/` 目录 |
| `docs/demo/` | 静态展示原型资产 | 存在 `docs/demo/hnblue_v2_public_data_demo.html` |
| `scripts/` | 数据处理与地理空间脚本资产 | 包含数据生成、校验、GEE/GDAL 相关脚本 |

## 前端现状

入口证据：

- `frontend/src/main.js` 引入 `App.vue`、`./router`、`./stores/counter.js`，挂载到 `#app`。
- `frontend/src/App.vue` 使用 `<router-view/>`，说明页面级切换依赖 Vue Router。
- `frontend/package.json` 已安装 ECharts，适合承载公开数据时序图、柱状图、来源质量面板。

当前缺口：

- 当前仓库未提供充分信息：`frontend/src/router` 目录缺失，无法确认真实路由表。
- 当前仓库未提供充分信息：`frontend/src/stores` 目录缺失，`main.js` 引入的 store 文件不在当前树中。
- 当前仓库未提供充分信息：`frontend/src/assets` 目录缺失，但多个组件引用 `@/assets/...`。
- `App.vue` 引入了多个当前树中不存在的组件，例如 `VisualPage.vue`、`UserInfo.vue`、`UserManage.vue`、`StructurePredictor.vue`、`VirtualPlotDesigner.vue`、`ShapVisualizer.vue`、`ResponseCurve.vue`、`ParamSensitivity.vue`。

判断：

- 当前前端代码具备页面集成形态，但当前仓库快照缺少路由、store、资源文件，直接运行成功性需要补充验证。
- V2.0 公开数据展示页首轮应选择静态专题页路径，减少对旧路由与旧接口的耦合。

## 后端现状

Spring Boot 配置证据：

- `backend/src/main/resources/application.properties` 配置 `server.port=8088`。
- 同文件配置 MySQL：`jdbc:mysql://localhost:3306/hnblue?...`，当前指向旧 `hnblue` 库。
- 同文件配置 Redis Cluster：`192.168.204.128:6379` 到 `6384`。
- 同文件配置 AnythingLLM：`anythingllm.api.url=http://localhost:3001/api`、`anythingllm.workspace=hnblue`。

核心接口：

| 接口 | 文件 | 数据流 |
|---|---|---|
| `/api/devisual/{regionName}` | `backend/src/main/java/com/example/jpaspringboot/controller/DevisualController.java` | 聚合区域基础信息、年度碳储、通量组成、多碳指标、年度趋势、生态分区、通量因子、物种组成、经济估值 |
| `/api/indicators/current` | `backend/src/main/java/com/example/jpaspringboot/controller/VisualIndicatorController.java` | 查询当前年度 `visual_indicator_data` |
| `/api/indicators/update` | 同上 | 更新当前年度已存在指标，服务层固定 `CURRENT_YEAR = 2024` |
| `/api/chat/stream` | `backend/src/main/java/com/example/jpaspringboot/controller/SeekController.java` | SSE 聊天流 |
| `/api/chat/stream-carbon` | `backend/src/main/java/com/example/jpaspringboot/controller/CarbonAssistantController.java` | AnythingLLM 工作区流式对话 |
| `/api/login`、`/api/register` | `backend/src/main/java/com/example/jpaspringboot/controller/LoginController.java` | 用户认证 |

区域详情表证据：

| JPA 实体 | 表 | 主要字段 |
|---|---|---|
| `RegionSummaryInfo` | `region_summary_info` | `regionName`、`dominantSpecies`、`mangroveArea`、`forestAge`、`cnRatio` |
| `RegionYearlyCarbon` | `region_yearly_carbon` | `regionName`、`year`、`carbonStorage`、`carbonFlux` |
| `RegionFluxComposition` | `region_flux_composition` | `region_name`、`year`、`co2`、`ch4`、`n2o`、`ghg` |
| `RegionMultiCarbonMetrics` | `region_multi_carbon_metrics` | `litterfall`、`deadwood`、`aerialRoot`、`soil`、`aboveground` |
| `RegionYearlyTrend` | `region_yearly_trend` | `region_name`、`year`、`soc`、`biomass`、`co2_flux`、`ch4_flux` |
| `RegionSpeciesComposition` | `region_species_composition` | `region_name`、`species_name`、`carbon_ratio` |
| `RegionEconomyProjection` | `region_economy_projection` | `region_name`、`scenario_name`、`value` |
| `RegionZoneMap` | `region_zone_map` | `coreZone`、`bufferZone`、`intertidalZone`、`riskZone` JSON |

判断：

- 后端已有区域详情聚合接口，适合作为 V2.0 入库后的展示 API 形态参照。
- 当前公开数据包目标是不连接数据库展示，首轮无需改动 Spring Boot。
- 后续入库映射需要避开旧 `hnblue` 库配置，转向 `sql/schema/hnblue_v2_schema.sql` 定义的 V2.0 表。

## Flask 模型服务现状

证据：

- `flask_model/carbon_model_api/app.py` 在 `8880` 暴露服务。
- `/api/literature-range` 从 `../BAAD_cleaned.csv` 读取 `m.so`，按碳分数 `0.48` 计算历史碳储范围。
- `frontend/src/components/LiteratureCompare.vue` 调用 `http://localhost:8880/api/literature-range`。

判断：

- Flask 服务承担模型预测与历史范围参考，不是 HNBLUE V2.0 公开数据包展示的必要依赖。
- 文献证据卡片应优先读取 normalized JSON，避免将 BAAD 统计范围混入海南公开文献证据。

## 公开数据包现状

目录：`data/results/hnblue_v2_public/normalized/`

只读统计基线：

| 文件 | 记录数 | 数值记录 | 区域数 | 指标数 | proxy 数 | simulated 合计 | 年份范围 |
|---|---:|---:|---:|---:|---:|---:|---|
| `display_mangrove_area_timeseries.csv` | 172 | 172 | 12 | 13 | 0 | 0 | 1990..2022 |
| `display_remote_sensing_metrics.csv` | 147 | 147 | 10 | 49 | 30 | 0 | 1990..2020 |
| `display_literature_carbon_records.csv` | 73 | 73 | 14 | 31 | 19 | 0 | 2005..various |
| `display_species_allometry_records.csv` | 22 | 22 | 3 | 6 | 0 | 0 | 2005..2022 |
| `display_data_sources.csv` | 9 | 0 | 0 | 0 | 0 | 0 | NA |
| `display_region_dictionary.csv` | 25 | 0 | 25 | 0 | 0 | 0 | NA |

前端 JSON：

- `frontend_mangrove_area_timeseries.json`：顶层键 `dataset`、`is_simulated`、`regions`、`supplement_metrics`。
- `frontend_remote_sensing_metrics.json`：顶层键 `dataset`、`is_simulated`、`metrics`。
- `frontend_literature_carbon_cards.json`：顶层键 `dataset`、`is_simulated`、`cards`。

判断：

- 公开数据包已经达到静态展示页首轮数据量要求。
- `display_*` CSV 适合后续入库映射与审计。
- `frontend_*` JSON 适合页面直接读取。
- `is_simulated` 当前全部为 0，符合公开数据展示边界。

## 风险与下一步检查

| 风险 | 影响 | 最小检查 |
|---|---|---|
| 前端路由目录缺失 | 无法确认正式页面挂载位置 | 检查是否存在未纳入仓库的 `frontend/src/router/index.js` |
| 前端 assets 缺失 | `DeVisualPage.vue` 和首页资源可能构建失败 | 补齐或定位 `frontend/src/assets` |
| Spring Boot 指向旧 `hnblue` 库 | 直接导入 V2.0 数据会污染旧库边界 | 单独配置 V2.0 datasource 或导入脚本 |
| 公开数据含 proxy | 前端需显式标注来源类型 | `is_proxy=1` 统一显示为 proxy，不展示为通量塔实测 |
| 中文在 PowerShell 输出中出现乱码 | 控制台证据显示编码不稳定 | 文件本身按 UTF-8 保存，前端需使用 UTF-8 meta 与 JSON 解析 |
