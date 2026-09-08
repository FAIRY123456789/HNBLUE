# HNBLUE V2.0 Phase 1 公开数据包移交报告

生成日期：2026-05-27
工作范围：基于 `data/results/hnblue_v2_public/normalized/` 的公开数据结果包，完成前端静态展示接入与入库映射梳理。
当前结论：本阶段产物已经具备前端展示能力；数据库尚未执行写入；下一阶段应进入 V2 Schema 映射与受控导入。

## 1. 数据库写入状态

从仓库改动和脚本行为看未执行数据库写入；如需严格确认，应由用户在 MySQL 中执行表记录数核验。

已核验事实：

- `scripts/frontend/sync_hnblue_v2_public_data.py` 仅执行 `data/results/hnblue_v2_public/normalized/` 到 `frontend/public/data/hnblue_v2_public/normalized/` 的文件复制，并生成 `sync_manifest.json`。
- 同步脚本未导入 `mysql`、`pymysql`、`mysql.connector`、`sqlalchemy`，未调用 `execute()`、`executemany()`，未访问网络服务。
- 本阶段未运行 SQL seed、Flyway、Liquibase、后端接口写入任务或数据库导入命令。
- 仓库中存在 V2 seed SQL 文件与 schema 文件，它们是静态 SQL 资产；当前核验未发现执行证据。
- 现有后端配置仍包含旧库连接 `jdbc:mysql://localhost:3306/hnblue`，本阶段未修改后端、Flask、Redis、AnythingLLM 配置。

数据库状态判定：

| 对象 | 当前状态 | 证据 | 结论 |
|---|---:|---|---|
| 旧 `hnblue` V1.0 库 | 未发现本阶段写入 | 本阶段无后端写入命令；同步脚本只复制静态文件 | 未替换数据库数据 |
| `hainan_blue_carbon_*` V2 Schema | 未发现本阶段写入 | 仅存在 schema/seed SQL 文件；无执行记录 | 待下一阶段导入 |
| 前端静态数据目录 | 已写入文件 | `frontend/public/data/hnblue_v2_public/normalized/` | 可直接展示 |

## 2. 公开数据包状态

源目录：`data/results/hnblue_v2_public/normalized/`
前端副本：`frontend/public/data/hnblue_v2_public/normalized/`

两处文件已经同步。`sync_manifest.json` 记录了同步文件、行数与哈希。

| 数据文件 | 数据内容 | 记录数 | 是否已入库 | 推荐目标 Schema | 推荐目标表 | 是否可替换模拟数据 | 风险说明 |
|---|---:|---:|---|---|---|---|---|
| `display_mangrove_area_timeseries.csv` | 海南、文昌、八门湾等红树林面积时序 | 172 | 否 | `hainan_blue_carbon_satellite` | `t_satellite_mangrove_cover` | 是 | 遥感/公开统计结果，适合替换前端面积模拟时序；入库前需统一区域编码与面积单位 |
| `display_remote_sensing_metrics.csv` | GPP/NPP、气候、土地利用、碳储等区域指标 | 147 | 否 | `hainan_blue_carbon_satellite` | `t_satellite_region_metric` | 是，需显示 proxy | 含 30 条 proxy；MODIS、ERA5、CHIRPS 等不可标注为通量塔或样地实测 |
| `display_literature_carbon_records.csv` | 文献碳储、SOC、AGB、BGB、面积、物种组成等数值证据 | 73 | 否 | `hainan_blue_carbon_intl` | `t_literature_carbon_record` | 是 | 含 19 条 proxy；文献区域均值不可拆成样方、样木或土壤样品明细 |
| `display_species_allometry_records.csv` | 物种、异速方程、生物量相关记录 | 22 | 否 | `hainan_blue_carbon_intl` | `t_allometry_equation` | 部分是 | 适合作为模型参数与文献证据；字段需按方程参数、适用物种、适用部位拆分 |
| `display_data_sources.csv` | 数据来源、质量等级、proxy 汇总 | 9 | 否 | `hainan_blue_carbon_core` | `t_data_source` | 是 | `proxy_count` 为来源层级汇总值，不能按观测记录解释 |
| `display_region_dictionary.csv` | 展示区域字典 | 25 | 否 | `hainan_blue_carbon_core` | `t_region` | 是 | 入库前需确认行政区、生态区、研究区的层级关系 |
| `frontend_mangrove_area_timeseries.json` | 面积时序前端聚合 JSON | 11 个区域组 | 否 | 不建议直接入库 | 前端派生文件 | 是 | 展示派生数据，入库应使用 CSV 原表 |
| `frontend_remote_sensing_metrics.json` | 遥感指标前端 JSON | 147 | 否 | 不建议直接入库 | 前端派生文件 | 是，需显示 proxy | 展示派生数据，入库应使用 CSV 原表 |
| `frontend_literature_carbon_cards.json` | 文献证据卡片 JSON | 73 | 否 | 不建议直接入库 | 前端派生文件 | 是 | 展示派生数据，入库应使用 CSV 原表 |

校验统计：

| 文件 | 记录数 | 区域数 | 指标数 | proxy 数 | `is_simulated` |
|---|---:|---:|---:|---:|---:|
| `display_mangrove_area_timeseries.csv` | 172 | 12 | 13 | 0 | 0 |
| `display_remote_sensing_metrics.csv` | 147 | 10 | 49 | 30 | 0 |
| `display_literature_carbon_records.csv` | 73 | 14 | 31 | 19 | 0 |
| `display_species_allometry_records.csv` | 22 | 3 | 6 | 0 | 0 |
| `display_data_sources.csv` | 9 | 0 | 0 | 49 汇总值 | 0 |
| `display_region_dictionary.csv` | 25 | 25 | 0 | 0 | 0 |

## 3. 前端改动清单

| 代码文件 | 改动目的 | 是否影响 V1.0 原页面 | 是否建议保留 | 后续处理建议 |
|---|---|---|---|---|
| `frontend/src/components/PublicDataPage.vue` | 新增 HNBLUE V2.0 公开数据展示页，读取 public normalized JSON/CSV | 低 | 保留 | 后续接入真实 V2 API 后保留为页面组件，数据源从静态 JSON 切换为接口 |
| `frontend/src/router/index.js` | 注册 `/v2-public-data` 路由，并补齐当前前端启动所需路由 | 中 | 保留主路由，审查临时占位路由 | 下一阶段确认 V1.0 原路由表，移除或替换占位页 |
| `frontend/src/App.vue` | 改为 `<router-view />` 入口，保证 V2 页面可通过路由访问 | 中 | 临时保留 | 需与 V1.0 原布局合并，恢复全局导航、过渡和布局约束 |
| `frontend/src/components/HomePage.vue` | 增加 V2.0 公开数据入口 | 低 | 保留 | 保持入口文案与产品导航一致 |
| `frontend/src/components/AboutPage.vue` | 增加 V2.0 公开数据入口 | 低 | 保留 | 同步后续正式路由名称 |
| `frontend/src/components/CarbonSeek.vue` | 增加 V2.0 公开数据入口卡片 | 低 | 保留 | 与碳汇模块正式信息架构合并 |
| `frontend/vue.config.js` | 增加 history fallback，支持刷新静态路由 | 低 | 保留 | 部署到 Nginx 或 Spring 静态资源时同步配置 fallback |
| `frontend/public/data/hnblue_v2_public/normalized/*` | 前端静态数据副本 | 低 | 保留 | 由同步脚本生成，不手工维护 |
| `frontend/src/stores/counter.js` | 补齐现有 `main.js` 依赖的最小 Vuex store | 中 | 临时保留 | 查找 V1.0 原 store 设计后替换 |
| `frontend/src/assets/*` | 补齐旧页面构建所需占位图片 | 中 | 临时保留 | 恢复正式视觉资产，避免影响 V1.0 页面观感 |
| `scripts/frontend/sync_hnblue_v2_public_data.py` | 同步 normalized 数据到前端 public 目录 | 低 | 保留 | 下一阶段纳入数据构建流水线 |
| `docs/v2/HNBLUE_v2_public_data_frontend_integration_report.md` | 前端集成报告 | 无 | 保留 | 作为阶段验收记录 |

## 4. 哪些数据可替换旧模拟展示

可直接替换：

- 红树林面积时序：使用 `display_mangrove_area_timeseries.csv` 或 `frontend_mangrove_area_timeseries.json`。
- 文献碳储证据卡片：使用 `display_literature_carbon_records.csv` 或 `frontend_literature_carbon_cards.json`。
- 数据来源与质量说明：使用 `display_data_sources.csv`、`display_region_dictionary.csv`。

可替换但必须标注 proxy：

- 遥感、再分析、土地利用、模型碳储类区域指标：使用 `display_remote_sensing_metrics.csv` 或 `frontend_remote_sensing_metrics.json`。
- MODIS GPP/NPP、ERA5、CHIRPS、GHG/碳储清单类数据只能作为区域 proxy，不能展示为通量塔观测、样地实测或土壤样品记录。

不建议直接作为观测表入库：

- 三个 `frontend_*.json` 文件属于展示派生文件。
- 文献区域均值、模型结果和异速方程记录不应拆分成样方、样木、土壤剖面明细。

## 5. 推荐 V2 入库顺序

第一步导入核心字典：

- `display_region_dictionary.csv` -> `hainan_blue_carbon_core.t_region`
- `display_data_sources.csv` -> `hainan_blue_carbon_core.t_data_source`
- 指标字典可由各 display CSV 的 `indicator_name`、`unit`、`method` 聚合后写入 `hainan_blue_carbon_core.t_indicator_dictionary`
- 数据版本与资产索引写入 `hainan_blue_carbon_core.t_data_version`、`hainan_blue_carbon_core.t_data_asset_index`

第二步导入事实数据：

- `display_mangrove_area_timeseries.csv` -> `hainan_blue_carbon_satellite.t_satellite_mangrove_cover`
- `display_remote_sensing_metrics.csv` -> `hainan_blue_carbon_satellite.t_satellite_region_metric`
- `display_literature_carbon_records.csv` -> `hainan_blue_carbon_intl.t_literature_reference` + `hainan_blue_carbon_intl.t_literature_carbon_record`
- `display_species_allometry_records.csv` -> `hainan_blue_carbon_intl.t_allometry_equation`

第三步生成本地展示兼容层：

- 面向 V1.0 可视化替换的数据，可从 V2 表聚合写入 `hainan_blue_carbon_local.t_local_region_yearly_metric` 或 `hainan_blue_carbon_local.t_local_visualization_metric`。
- 该层仅作为展示兼容层，权威数据仍以 core、satellite、intl schema 为准。

## 6. 本轮核验命令摘要

已执行核验：

- `git diff --name-only`：确认已修改前端入口、导航、路由兼容配置。
- `git status --short`：确认新增数据、脚本、文档、前端 public 资源处于工作区。
- Python 读取 source/public normalized CSV：确认两处记录数、区域数、指标数、proxy 数和 `is_simulated` 一致。
- 搜索数据库写入关键字：确认本阶段新增同步脚本不包含数据库连接与写入逻辑；仓库中的 SQL 文件仅为静态入库资产。
- 检查 `sync_manifest.json`：确认 9 个前端展示数据文件已经从 normalized 源目录复制到 public 目录。

后续严格验库建议：

```sql
SELECT COUNT(*) FROM hainan_blue_carbon_core.t_region;
SELECT COUNT(*) FROM hainan_blue_carbon_core.t_data_source;
SELECT COUNT(*) FROM hainan_blue_carbon_satellite.t_satellite_mangrove_cover;
SELECT COUNT(*) FROM hainan_blue_carbon_satellite.t_satellite_region_metric;
SELECT COUNT(*) FROM hainan_blue_carbon_intl.t_literature_carbon_record;
SELECT COUNT(*) FROM hainan_blue_carbon_intl.t_allometry_equation;
```

若上述表在 MySQL 中已经存在记录，应结合 `created_at`、`data_version_id`、`source_id` 与导入日志判断来源；当前仓库证据不能直接证明外部 MySQL 实例的实际状态。

## 7. 交接结论

Phase 1 已形成可展示、可校验、可映射的公开数据包。当前最合适的下一步是编写受控入库脚本，而非继续扩展来源目录。

推荐下一阶段任务：

1. 固化 `source_id`、`region_id`、`indicator_id` 映射字典。
2. 为 6 个 display CSV 生成 V2 Schema staging 表或导入 SQL。
3. 先导入 `core` 字典，再导入 `satellite` 与 `intl` 事实表。
4. 从 V2 表生成 `local` 展示兼容层，用于逐步替换旧 `hnblue` V1.0 演示数据。
