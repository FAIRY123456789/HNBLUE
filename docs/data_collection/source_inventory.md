# HNBLUE V2.0 公开来源目录

访问日期统一记录为 2026-05-25。质量等级按本轮规则分为 A、B、C、D。A 级可作为核心元数据或正式数据集来源，B 级可作为可核验统计或项目线索，C 级只进入知识背景，D 级只保留待核验。

| source_id | 来源名称 | 类型 | 发布机构 | 时间 | 空间范围 | 格式 | 质量 | 目标表 | 可用字段 | 限制 |
|---|---|---|---|---|---|---|---|---|---|---|
| SRC-GOV-001 | 海南省红树林保护规划相关公开页面 | 政策规划 | 国家林草局/海南相关部门 | 2025 前后，页面时间需复核 | 海南省 | HTML/PDF 线索 | B | core.t_data_source, core.t_region | 规划名称、保护对象、政策依据 | 未取得可机器读取空间边界 |
| SRC-GOV-002 | 海南蓝碳试点与生态产品信息 | 政府报道 | 海南省人民政府 | 2023-07 | 海南省、文昌八门湾等 | HTML | B | core.t_data_source, core.t_data_asset_index | 145 个沉积物柱样、363 个样方、无人机遥感调查线索 | 报道型页面，无原始表格下载 |
| SRC-GOV-003 | 海南红树林保护与修复工程信息 | 政府报道 | 海南省人民政府 | 2025-10 | 海南省 | HTML | B | core.t_data_source | 修复工程、蓝碳项目线索 | 未来日期页面需后续复核 |
| SRC-GMW-001 | Global Mangrove Watch 平台 | 遥感平台 | Global Mangrove Watch | 持续更新 | 全球含海南 | Web map/API | A | satellite.t_satellite_product_metadata | 产品名称、年份、空间范围、下载入口 | 入库只登记元数据 |
| SRC-GMW-002 | Global Mangrove Watch 1996-2020 Version 3.0 | 遥感数据集 | GMW/Zenodo | 2022 | 全球含海南 | GeoTIFF/Shapefile/tiles | A | satellite.t_satellite_product_metadata, core.t_data_asset_index | 年份、分辨率、红树林 extent、DOI、下载 URL | 大体量文件进入冷数据路径 |
| SRC-RS-001 | Sentinel-2 MSI Level-2A Surface Reflectance Harmonized | 遥感产品 | ESA/Copernicus, Google Earth Engine | 2015 至今 | 全球含海南 | GEE ImageCollection | A | satellite.t_satellite_product_metadata | product_id、bands、cloud QA、10m/20m 分辨率 | 需 GEE 脚本计算区域指标 |
| SRC-RS-002 | MODIS MOD17A3HGF GPP/NPP | 遥感生产力产品 | NASA LP DAAC, Google Earth Engine | 2000 至今 | 全球含海南 | GEE ImageCollection | A | flux.t_flux_daily_aggregate 或 intl.t_public_remote_sensing_product | GPP、NPP、年份、质量字段 | 替代通量指标，必须 is_proxy=1 |
| SRC-RS-003 | Landsat Collection 2 Level-2 Science Products | 遥感产品 | USGS/NASA | 1982 至今 | 全球含海南 | GeoTIFF/USGS/GEE | A | satellite.t_satellite_product_metadata | SR bands、QA_PIXEL、时间、轨道 | 需区域裁剪和云掩膜 |
| SRC-RS-004 | NASA ORNL DAAC 红树林生物量/冠层高度产品线索 | 遥感/生态数据平台 | NASA ORNL DAAC | 多版本 | 全球或区域 | NetCDF/GeoTIFF/CSV | A | core.t_data_asset_index, intl.t_public_remote_sensing_product | 产品 DOI、变量、分辨率 | 本轮未锁定单一 DOI，列为下一轮核验 |
| SRC-LIT-001 | Composition and distribution of soil organic carbon in mangroves: a case study from Bamen Bay | 论文 | MDPI Water | 2022 | 文昌八门湾 | HTML/PDF | A | intl.t_literature_carbon_record, ground.t_soil_sample_candidate | SOC、土层、植被类型、采样区域 | 表号页码需二次核对 |
| SRC-LIT-002 | Accounting of blue carbon and greenhouse gas emissions in Hainan Province | 论文 | Frontiers in Marine Science | 2022 | 海南省 | HTML/PDF | A | intl.t_literature_carbon_record, flux.t_flux_or_proxy_candidate | 蓝碳清单、温室气体、方法 | 省域统计，不拆成样木明细 |
| SRC-LIT-003 | Assessment of Mangrove Carbon Stock in Hainan Island | 论文摘要页 | Journal of Resources and Ecology | 2022 | 海南岛 | HTML | B | intl.t_literature_carbon_record | 碳储量评估方法、生态系统类型 | 当前只记录摘要级信息 |
| SRC-METHOD-001 | 2013 Supplement to the 2006 IPCC Guidelines for National Greenhouse Gas Inventories: Wetlands | 方法标准 | IPCC | 2013 | 全球 | PDF | A | knowledge.blue_carbon_sources | 湿地碳库、排放因子、核算框架 | 方法来源，不直接写数值 |
| SRC-METHOD-002 | Coastal Blue Carbon Methods Manual | 方法手册 | Blue Carbon Initiative | 2014 | 全球 | PDF | A | knowledge.blue_carbon_sources | 样地、土壤、植被碳测量方法 | 需按引用格式登记 |
| SRC-METHOD-003 | VM0033 Methodology for Tidal Wetland and Seagrass Restoration | 核证方法学 | Verra | v2.1 页面 | 全球 | PDF/HTML | A | knowledge.blue_carbon_sources | 项目边界、基线、监测、核证 | 用于项目合规模块 |
| SRC-ALLO-001 | BAAD Biomass And Allometry Database | 公开数据库 | BAAD authors/GitHub | 多版本 | 全球 | CSV/RData | A | intl.t_allometric_dataset, model.t_model_reference_dataset | dbh、height、biomass、species、site | 非海南专属，模型参考 |
| SRC-ALLO-002 | Tallo tree allometry and crown architecture database | 公开数据库 | Zenodo/论文作者 | 多版本 | 全球 | CSV | A | intl.t_allometric_dataset | height、crown、dbh、species | 需下一轮锁定版本 DOI |
| SRC-LOCAL-001 | HNBLUE 仓库 docs/context/04_database_and_data_context.md | 仓库上下文 | HNBLUE 项目 | 2026-05-25 仓库状态 | HNBLUE V2.0 | Markdown | A | 字段映射依据 | schema 边界、建议表名、冷热存储规则 | `hnblue_v2_schema.sql` 当前未找到 |

## 当前仓库未找到的材料

| 文件或目录 | 状态 |
|---|---|
| `hnblue_v2_schema.sql` | 当前仓库未找到该文件 |
| `docs/context/database_v2_design.md` | 当前仓库未找到该文件 |
| `docs/v2/` | 当前仓库未找到该目录或无文件 |
