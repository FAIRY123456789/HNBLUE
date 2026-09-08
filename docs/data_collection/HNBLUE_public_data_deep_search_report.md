# HNBLUE V2.0 第二轮公开数据深挖报告

检索日期：2026-05-25。`sql/schema/hnblue_v2_schema.sql` 已存在，本轮按真实 V2.0 schema 语义生成 staging 数据，不执行数据库导入。

## 1. 新增来源和记录数量

本轮新增或复核公开来源 40 条，见 `deep_search_log.md`。本轮生成可进入 staging 的记录 146 条：文献碳储 35 条，遥感产品 24 条，遥感指标计划 12 条，通量/代理 22 条，物种与异速生长 32 条，区域项目 21 条。

## 2. 区域数据发现

海南省：获得 2010 年和 2020 年红树林面积、EF_AGB、EF_SOC、总碳储密度、GHG 清单、市县尺度净 CO2+CH4 proxy、363 个植被样方和 145 个沉积柱公开统计。

文昌市：获得 EF_AGB 66.2 MgC/ha、EF_soil 337.0 MgC/ha、市级 GHG proxy、文昌 N2O shrimp aquaculture proxy，以及八门湾研究区数据。

八门湾/清澜港：获得研究区坐标范围、保护区总面积 2948 hm2、红树林面积 1223.3 hm2、天然红树林 842.2 hm2、人工红树林 381.1 hm2、SOC storage 104.41/207.14/228.78 Mg/ha。

东寨港：本轮通过关键词检索到方向线索，但未锁定开放数值表。当前先用海南省级清单和 GMW/GEE 产品覆盖，后续继续追中文 PDF、硕博摘要和保护区资料。

## 3. 可直接进入 HNBLUE V2.0 的数据

1. `literature_carbon_records.csv`：文昌八门湾 SOC、海南 EF_AGB/EF_SOC、海南红树林面积、政府公开样本量。
2. `remote_sensing_product_candidates.csv`：GMW、Sentinel、Landsat、MODIS、GEDI、CHIRPS、ERA5 等产品元数据。
3. `region_and_project_records.csv`：八门湾面积、bbox、海南面积、调查样本量。
4. `species_and_allometry_records.csv`：文昌/海南物种记录、Komiyama 方程、根冠比。

## 4. 只能作为 proxy 的数据

通量相关记录全部作为 proxy。包括海南 GHG 清单、市县净 CO2+CH4、文昌水产养殖 N2O、MODIS GPP/NPP、ERA5 蒸散和气候驱动项。这些记录已写入 `is_proxy=1`。

## 5. 只能作为知识库或报告引用的数据

IPCC、Blue Carbon Initiative、Verra、NASA/USGS/Copernicus 平台入口、BAAD/Tallo 字段说明、GEE 产品说明适合进入 AI 碳助手和报告模板。它们不直接作为观测数值写入业务指标表。

## 6. 需要 GEE 或 GDAL 后处理的数据

GMW 需要按海南、文昌、八门湾边界裁剪计算面积。Sentinel-2 和 Landsat 需要云掩膜、潮滩语境过滤和 NDVI/MNDWI 计算。MODIS GPP/NPP 需要红树林掩膜和区域年度统计。GEDI 需要质量过滤和红树林掩膜叠置。

## 7. 仍未找到公开来源的方向

海南红树林通量塔 30 分钟开放数据、清澜港独立碳储量表格、东寨港开放碳储表格、中国本土红树林多物种异速方程全文表仍需下一轮深挖。已检索关键词和页面记录在 `unresolved_but_search_attempted.md`。

## 8. 相比上一轮的实质新增

上一轮以来源目录和映射为主。本轮补齐了八门湾 SOC 数值、海南 EF_AGB/EF_SOC、市县 GHG proxy、八门湾面积和 bbox、海南调查样本量、物种存在记录、异速生长方程系数、24 条遥感产品元数据和 12 条 GEE/GDAL 指标生产计划。

## 9. 下一轮第一个代码任务

编写 `scripts/data_staging/validate_public_records.py`：读取本轮 CSV，校验 `record_id/source_id/data_type/extraction_method/quality_level/is_proxy/is_simulated/value/unit/source_url`，按 `hnblue_v2_schema.sql` 生成 `core.t_data_source`、`core.t_region`、`intl.t_literature_carbon_record`、`intl.t_external_observation`、`intl.t_allometry_equation`、`satellite.t_satellite_product` 的导入 SQL。脚本只生成 SQL，不连接数据库。
