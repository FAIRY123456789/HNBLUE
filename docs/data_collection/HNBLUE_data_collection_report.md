# HNBLUE V2.0 公开数据搜集与入库准备报告

检索日期：2026-05-25
执行范围：公开网页、数据集页面、论文页面、方法学页面、当前 HNBLUE 仓库上下文。
执行边界：未修改前端、后端、Flask 模型服务、Redis、AnythingLLM 配置；未执行数据库导入；未向 V1.0 `hnblue` 库写入数据。

## 1. 本轮检索范围

本轮覆盖政府与官方来源、权威遥感数据平台、同行评议论文、蓝碳方法标准、公开模型参考数据库和公开处理案例。重点面向海南省、文昌市、文昌八门湾、清澜港、海南红树林、碳储量、土壤有机碳、温室气体或替代通量指标、GMW、Sentinel-2、Landsat、MODIS GPP/NPP、BAAD、Tallo、IPCC、Blue Carbon Initiative 和 Verra。

## 2. 仓库与 V2.0 数据库材料状态

| 材料 | 状态 |
|---|---|
| `docs/context/00_README.md` 至 `08_codex_working_prompt.md` | 已读取 |
| `docs/context/04_database_and_data_context.md` | 已读取，作为 V2.0 Schema 边界依据 |
| `docs/context/database_v2_design.md` | 当前仓库未找到该文件 |
| `docs/v2/` | 当前仓库未找到可读取诊断文档 |
| `hnblue_v2_schema.sql` | 当前仓库未找到该文件 |
| `frontend/`、`backend/`、`flask_model/`、`figures/` | 已检查，只读 |
| 数据库脚本目录 | 当前未发现独立 `database/` 或 `sql/` 旧脚本目录，本轮新建 `sql/seed/` |

V2.0 正式 SQL 缺失，因此本轮按 `docs/context/04_database_and_data_context.md` 提供的 `hainan_blue_carbon_core`、`ground`、`flux`、`satellite`、`uav`、`model`、`intl`、`local` 等 Schema 边界建立准备文件。

## 3. 已确认可用的数据来源

| 类型 | 最高价值来源 | 适用场景 |
|---|---|---|
| 政府来源 | 海南省政府关于蓝碳试点、文昌八门湾调查和红树林修复的页面；国家林草局和海南相关红树林规划页面 | 区域对象、项目背景、待接入数据线索 |
| 遥感产品 | Global Mangrove Watch 1996-2020 v3.0、Sentinel-2 SR Harmonized、Landsat Collection 2 Level-2、MODIS MOD17A3HGF | 红树林分布、NDVI、面积变化、GPP/NPP 替代通量 |
| 文献数据 | MDPI Water 文昌八门湾 SOC 论文、Frontiers 海南蓝碳清单论文、JORE 海南碳储量评估论文 | 文献碳储、SOC、GHG 清单、方法参数 |
| 方法标准 | IPCC 2013 Wetlands Supplement、Blue Carbon Initiative Manual、Verra VM0033 | AI 碳助手、报告模板、核证口径 |
| 模型参考 | BAAD、Tallo | CatBoost 变量、异速生长方程、结构参数参考 |

## 4. 暂未找到或不可直接使用的数据来源

1. 当前公开来源未找到海南红树林通量塔连续开放数据。MODIS GPP/NPP 和文献 GHG 结果只能作为替代通量或文献候选。
2. 当前公开来源未提供文昌示范区真实样木级调查表。不能把区域均值伪造成样木明细。
3. 当前公开来源未提供文昌无人机正射影像、DSM、CHM、点云或航飞日志下载。
4. 当前公开来源未取得清澜港、八门湾和保护区法定边界 WKT。区域对象只能登记名称、空间语境和待复核状态。

## 5. 文昌示范区可优先使用的数据

优先入库元数据包括：海南省、文昌市、文昌八门湾、清澜港红树林区域对象；GMW v3.0 产品；Sentinel-2/Landsat/MODIS 产品元数据；MDPI 文昌八门湾 SOC 论文；海南省政府披露的蓝碳试点和调查线索。数值型文献数据需人工核对表号、页码和单位后再进入正式业务表。

## 6. 遥感产品和冷数据处理建议

GMW、Sentinel-2、Landsat、MODIS 均不应把大体量影像或栅格直接提交到仓库。MySQL 只保存产品名、版本、时间范围、空间范围、URL、格式、分辨率、质量字段、冷数据建议路径和处理状态。后续使用 GEE 或本地 GDAL 对文昌范围裁剪并生成区域统计结果。

## 7. 地面样方和文献碳储数据处理建议

文献记录先进入 `hainan_blue_carbon_intl.t_literature_carbon_record`。若论文只提供区域均值，记录为区域统计数据。若后续取得项目组样方表，再进入 `hainan_blue_carbon_ground` 相关表。所有单位转换需同时保留原始单位、换算后单位和转换规则。

## 8. 通量数据或替代通量建议

MODIS GPP/NPP 进入 `flux_or_proxy_candidate_records.csv`，字段 `is_proxy=1`，说明为“替代通量指标”。Frontiers 省级 GHG 清单论文可作为文献候选，不能和通量塔 30 分钟观测混写。真实通量塔数据需项目组提供站点元数据、时间戳、NEE/NEP/GPP/Reco、CO2/CH4/N2O 和 QC flag。

## 9. 物种、异速生长方程和模型参考建议

BAAD 和 Tallo 可作为模型参考数据源，支撑 CatBoost 输入变量解释和异速生长方程候选。它们不代表海南本地实测结果。海南本地物种组成和优势种应来自文昌样方调查、保护区管理机构资料或同行评议论文。

## 10. 数据质量与许可证风险

政府网页、国际数据平台和开放论文可登记为 A/B 级来源。政府报道型页面通常缺少原始数据下载和再分发许可，只能作为项目线索和背景依据。GitHub、GEE 脚本和二级转载不进入核心数值入库。所有论文只记录题录、DOI、摘要级信息、表格字段线索和可核验短信息。

## 11. 与 HNBLUE V2.0 表结构的对应关系

| 数据类型 | 目标 Schema | 目标表建议 |
|---|---|---|
| 数据来源 | hainan_blue_carbon_core | t_data_source |
| 数据版本 | hainan_blue_carbon_core | t_data_version |
| 区域对象 | hainan_blue_carbon_core | t_region |
| 资产索引 | hainan_blue_carbon_core | t_data_asset_index |
| 遥感产品 | hainan_blue_carbon_satellite | t_satellite_product_metadata |
| 文献碳储 | hainan_blue_carbon_intl | t_literature_carbon_record |
| 样方候选 | hainan_blue_carbon_ground | t_ground_plot_candidate |
| 通量或代理 | hainan_blue_carbon_flux | t_flux_or_proxy_candidate |
| 无人机候选 | hainan_blue_carbon_uav | t_uav_product_or_method_candidate |
| 知识库材料 | hainan_blue_carbon_knowledge 或项目文档 | blue_carbon_knowledge_sources |

## 12. 项目组线下补充清单

1. 文昌示范区边界、保护地边界、行政区划矢量文件。
2. 文昌样方调查表、样木测量表、土壤样品化验表。
3. 通量塔站点元数据和连续观测文件。
4. 无人机正射影像、DSM、CHM、点云、航飞日志和处理报告。
5. 数据共享协议、使用限制和项目内部版本号。

## 13. 下一轮适合 Codex 执行的最小代码任务

第一优先级任务：补齐或接收 `hnblue_v2_schema.sql` 后，生成 `data/staging/*` 到正式 V2.0 表的校验型导入脚本。该任务应只读 CSV，校验必填字段、枚举、日期、URL、质量等级和 `is_proxy/is_simulated` 标记，然后输出 SQL 或 Flyway migration，不连接生产库。
