# HNBLUE V2.0 公开数据包集成计划

生成时间：2026-05-27

## 目标

将 `data/results/hnblue_v2_public/normalized/` 从离线 CSV/JSON 数据包升级为可支撑 HNBLUE V2.0 前端展示与后续入库映射的数据资产。首轮集成采用静态专题页，直接读取 JSON，不连接数据库。

## 数据包基线

目录：`data/results/hnblue_v2_public/normalized/`

| 文件 | 用途 | 当前状态 |
|---|---|---|
| `display_mangrove_area_timeseries.csv` | 红树林面积时序，审计与入库源表 | 172 条，12 区域，13 指标，`is_simulated` 合计 0 |
| `display_remote_sensing_metrics.csv` | 遥感/土地利用/碳储 proxy 与区域指标，审计与入库源表 | 147 条，10 区域，49 指标，30 条 proxy，`is_simulated` 合计 0 |
| `display_literature_carbon_records.csv` | 文献碳储证据，审计与入库源表 | 73 条，14 区域，31 指标，19 条 proxy，`is_simulated` 合计 0 |
| `display_species_allometry_records.csv` | 物种与异速方程数值，审计与入库源表 | 22 条，3 区域，6 指标，`is_simulated` 合计 0 |
| `display_data_sources.csv` | 来源清单与质量等级说明 | 9 条来源 |
| `display_region_dictionary.csv` | 区域标准字典 | 25 条区域 |
| `frontend_mangrove_area_timeseries.json` | 前端面积时序直接读取 | 顶层含 `dataset`、`is_simulated`、`regions`、`supplement_metrics` |
| `frontend_remote_sensing_metrics.json` | 前端遥感/土地利用/proxy 指标直接读取 | 顶层含 `dataset`、`is_simulated`、`metrics` |
| `frontend_literature_carbon_cards.json` | 前端文献证据卡片直接读取 | 顶层含 `dataset`、`is_simulated`、`cards` |

字段规范证据：

- `display_*` 主数据表包含 `source_name`、`source_url`、`source_type`、`region_name`、`year_or_period`、`indicator_name`、`value`、`unit`、`method`、`quality_level`、`is_proxy`、`is_simulated`、`database_target`、`frontend_group`。
- 当前只读校验显示 `is_simulated` 合计均为 0。
- proxy 数据集中于遥感/土地利用/碳储 proxy 和部分文献推导指标，前端需要显式标注。

## 展示模块映射

| 前端模块 | 直接数据源 | 主键/聚合 | 展示建议 | 入库价值 |
|---|---|---|---|---|
| 海南红树林面积时序 | `frontend_mangrove_area_timeseries.json`、`display_mangrove_area_timeseries.csv` | `region_id`、`region_name`、`year_or_period`、`indicator_code` | 折线图、区域筛选、来源标签、质量等级 | 高，适合入 `satellite.public_area_observation` 或 V2.0 面积时序表 |
| 海南岛遥感/土地利用/碳储 proxy 指标 | `frontend_remote_sensing_metrics.json`、`display_remote_sensing_metrics.csv` | `region_id`、`indicator_code`、`year_or_period` | 指标矩阵、柱状图、proxy 徽标 | 中高，适合入遥感统计表，proxy 字段必须保留 |
| 文献碳储证据卡片 | `frontend_literature_carbon_cards.json`、`display_literature_carbon_records.csv` | `source_id`、`record_id`、`indicator_code` | 卡片列表、指标数值、方法与来源 URL | 高，适合入文献证据表 |
| 数据来源与质量等级说明 | `display_data_sources.csv`、三个 JSON 的 source 字段 | `source_id` | 来源表、质量等级图例、可入库状态说明 | 高，适合入数据源元数据表 |
| 物种与异速方程 | `display_species_allometry_records.csv` | `record_id`、`species` 或 `indicator_code` | 二期单独模块或文献卡片补充 | 中，适合入物种/异速方程证据表 |

## 三种嵌入方案

### 方案 A：地图/可视化页嵌入

路径设想：

- 使用现有 `/visual` 导航入口。
- 在地图侧边栏或弹窗中展示区域面积时序与 proxy 指标。

仓库证据：

- `HomePage.vue`、`AboutPage.vue`、`CarbonSeek.vue` 均有 `/visual` 导航。
- `DeVisualPage.vue` 导入 `@/assets/json/hainan.json`，说明存在海南地图展示设计意图。

阻塞：

- 当前仓库未提供充分信息：`VisualPage.vue` 缺失。
- 当前仓库未提供充分信息：`frontend/src/router` 缺失。
- 当前仓库未提供充分信息：`frontend/src/assets/json/hainan.json` 缺失。

适用阶段：

- 地图组件和省域 GeoJSON 恢复后再执行。
- 适合做空间可视化入口，不适合作为首轮最小上线原型。

### 方案 B：区域详情/指标分析页嵌入

路径设想：

- 在 `DeVisualPage.vue` 中新增 V2.0 公开数据区块。
- 按 `props.name` 匹配 normalized `region_name` 或 `display_region_dictionary.csv` 的标准区域。

仓库证据：

- `DeVisualPage.vue` 当前已请求 `http://localhost:8088/api/devisual/${props.name}`。
- `DevisualController.java` 已聚合 `regionInfo`、`carbonTrends`、`fluxComposition`、`multiCarbonMetrics`、`yearlyTrends`、`fluxSamples`、`speciesPie`、`economyProjections`。
- JPA 表中已有 `region_summary_info`、`region_yearly_carbon`、`region_yearly_trend`、`region_species_composition` 等区域展示结构。

优势：

- 与 V2.0 后续入库映射一致。
- 能把公开数据与旧区域详情数据并列比较。

风险：

- 当前路由表缺失，访问链路无法确认。
- 旧接口依赖 MySQL `hnblue` 库，不能作为 V2.0 写入目标。
- normalized 中的区域粒度包含海南省、城市、保护地、研究区，需先完成区域字典映射。

适用阶段：

- V2.0 数据表与区域字典稳定后执行。
- 推荐作为二期融合目标。

### 方案 C：V2.0 公开数据专题页

路径设想：

- 新增 `/v2-public-data` 页面。
- 页面直接读取 normalized JSON。
- 四个模块在同一页面展示，不连接数据库。

仓库证据：

- normalized 已提供三个前端 JSON，结构直接面向展示。
- `frontend/package.json` 已具备 ECharts、Element Plus、axios/fetch 环境。
- `docs/demo/hnblue_v2_public_data_demo.html` 已存在，证明静态展示形态可独立于后端运行。

优势：

- 满足当前“页面直接读取 normalized JSON/CSV，不连接数据库”的边界。
- 改造范围小。
- 易于验收截图。
- 可为后续入库提供数据审阅界面。

风险：

- 需要恢复或补充 Vue Router 文件。
- 需要将 JSON 发布到前端可访问静态路径。

结论：

- 方案 C 为首轮推荐方案。
- 方案 B 为入库后一体化展示方案。
- 方案 A 为地图资产恢复后的空间入口方案。

## 推荐实施路径

### 第 0 步：保持源数据边界

源目录继续使用：

```text
data/results/hnblue_v2_public/normalized/
```

前端运行时目录建议：

```text
frontend/public/data/hnblue_v2_public/normalized/
```

同步方式：

- 用脚本从源目录复制三个 `frontend_*.json` 与必要的 `display_data_sources.csv`。
- 生成 hash 或 manifest，记录源文件时间与记录数。
- 页面只读，不写回数据包。

### 第 1 步：新增专题页

建议文件：

```text
frontend/src/components/PublicDataPage.vue
```

页面模块：

1. `MangroveAreaPanel`：读取 `frontend_mangrove_area_timeseries.json`，按区域渲染面积时序。
2. `RemoteSensingMetricPanel`：读取 `frontend_remote_sensing_metrics.json`，渲染遥感/土地利用/碳储 proxy 指标。
3. `LiteratureCarbonCardGrid`：读取 `frontend_literature_carbon_cards.json`，渲染文献证据卡片。
4. `DataSourceQualityPanel`：读取 `display_data_sources.csv` 或从 JSON 汇总来源，展示质量等级与 proxy 规则。

设计约束：

- `is_simulated=0` 才进入展示。
- `is_proxy=1` 必须显示 proxy 标签。
- MODIS、ERA5、CHIRPS、GHG 清单等区域统计不得标注为通量塔实测。
- 区域均值不得展示为样方、样木或土壤样品明细。

### 第 2 步：注册路由与导航

建议路由：

```text
/v2-public-data
```

建议导航入口：

- `HomePage.vue` 顶部导航新增“V2.0公开数据”。
- `AboutPage.vue` 顶部导航同步新增。
- `CarbonSeek.vue` 功能卡片新增“公开数据包”。

前提：

- 先恢复或创建 `frontend/src/router/index.js`。
- 确认现有 `/homepage`、`/about`、`/visual`、`/carbonseek` 路由配置，避免破坏旧页面。

### 第 3 步：验收

页面验收：

```powershell
cd frontend
npm run serve
```

浏览器：

```text
http://localhost:8080/v2-public-data
```

数据验收：

- 读取三个 JSON 成功。
- 面积模块展示多区域、多年份时序。
- 遥感模块显示 proxy 数量。
- 文献模块展示来源 URL、方法、质量等级。
- 页面不请求 `localhost:8088`、`localhost:8880`。

## 后续入库映射

normalized 字段到后端/数据库的建议映射：

| normalized 字段 | 目标含义 | 可映射对象 |
|---|---|---|
| `record_id` | 公开数据记录主键 | V2.0 事实表主键或外部记录 ID |
| `source_id`、`source_name`、`source_url`、`source_type` | 数据源元数据 | V2.0 数据源表 |
| `region_id`、`region_name`、`region_level` | 区域维表 | V2.0 区域字典表，兼容 `region_summary_info.regionName` |
| `year_or_period`、`year_start`、`year_end` | 时间维度 | 时序事实表 |
| `indicator_code`、`indicator_name`、`indicator_group` | 指标维度 | 指标字典表 |
| `value`、`unit` | 数值事实 | 面积/遥感/文献事实表 |
| `method`、`quality_level`、`notes` | 证据与质量控制 | 质量审计字段 |
| `is_proxy` | proxy 标记 | 必须保留，驱动前端标签与分析过滤 |
| `is_simulated` | 模拟标记 | 必须为 0 才可进入公开展示 |
| `database_target` | 目标库表建议 | 入库脚本映射参考 |
| `frontend_group`、`display_priority` | 前端分组与排序 | 专题页排序与模块过滤 |

与现有 Spring Boot 表的关系：

- `display_mangrove_area_timeseries.csv` 可映射到面积时序事实表，后续可派生给 `region_summary_info.mangroveArea` 或 `region_yearly_carbon` 相关展示。
- `display_remote_sensing_metrics.csv` 可映射到遥感统计事实表，proxy 标记保留。
- `display_literature_carbon_records.csv` 可映射到文献证据事实表，不直接写入样方/样木表。
- `display_species_allometry_records.csv` 可映射到物种与异速方程证据表，再按物种关联 `region_species_composition`。

入库前必须完成：

- 确认 V2.0 schema 的目标表字段。
- 固化区域字典，处理海南省、文昌、八门湾、保护地、研究点的层级关系。
- 建立单位标准化规则。
- 保留原始 `source_url` 与 `method`，支持审计回溯。

## 最小工作包

首轮开发工作包：

1. 静态数据同步脚本。
2. `PublicDataPage.vue`。
3. `/v2-public-data` 路由。
4. 首页、关于页、CarbonSeek 入口。
5. 前端数据读取校验脚本。

二期开发工作包：

1. V2.0 数据库导入脚本。
2. Spring Boot V2.0 只读 API。
3. `DeVisualPage.vue` 区域详情融合。
4. 地图页空间联动。

## 推荐结论

下一轮最应该做“前端静态专题页接入”，同时补齐 `frontend/src/router` 与静态数据发布路径。证据如下：

- 数据包已有展示级 JSON，且 `is_simulated` 全部为 0。
- 当前前端已有 ECharts 与 Element Plus。
- 当前后端仍指向旧 `hnblue` 库，数据库导入不是首轮最小路径。
- 当前地图页和路由文件缺失，区域详情/地图融合需要先恢复前端结构。
- 静态专题页能立即支撑 HNBLUE V2.0 公开数据展示和入库前审阅。
