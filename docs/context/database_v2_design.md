# HNBLUE V2.0 数据库规范设计说明

本文件用于说明 `hnblue_v2_schema.sql` 的设计依据、表结构边界和后续数据填充顺序。该设计以“当前没有可直接入库的真实数据”为前提，先建立数据治理框架，再通过浏览器检索、人工核验、公开数据下载、文献整理和后续实测数据交付逐步填充。HNBLUE 1.0 已经通过 `hnblue` 库支撑前端模拟可视化页面，因此 V2.0 方案保留旧库，不直接删除旧表和旧数据。

## 1. 总体判断

当前数据库已经出现 V2.0 的雏形。`hnblue` 库承担用户系统和前端图表展示功能，属于旧版演示库。`hainan_blue_carbon_core` 已经出现全局资产索引思路，适合作为 V2.0 的主数据中心。`hainan_blue_carbon_flux`、`hainan_blue_carbon_ground`、`hainan_blue_carbon_satellite` 和 `hainan_blue_carbon_uav` 已经按数据模态拆分，但站点、区域、来源、版本和质量控制等公共主数据仍需统一。`hainan_blue_carbon_intl` 适合承载 BAAD、Tallo、异速生长方程、公开 GPP、文献观测等外部参照数据。`hainan_blue_carbon_local` 目前混合了本地专题、旧版规范表、文献原始数据和栅格转点结果，V2.0 中应调整为文昌和海南本地展示聚合层。

V2.0 数据库的核心目标是把展示型指标表升级为数据治理型结构。所有真实数据、公开数据、文献数据、模拟数据和模型输出都必须能够追溯来源、版本、空间范围、时间范围、处理方法、质量标识和共享范围。大体量遥感、无人机、点云、GeoTIFF、原始通量文件和模型文件不进入 MySQL 二进制字段。MySQL 保存这些文件的元数据、路径、哈希值、空间范围、时间范围和状态。

## 2. V1.0 旧库定位

`hnblue` 库继续作为 V1.0 演示与前端联动库。该库中的 `region_yearly_carbon`、`region_flux_composition`、`region_flux_factors`、`region_multi_carbon_metrics`、`region_species_composition`、`region_summary_info`、`region_yearly_trend`、`region_zone_map`、`region_economy_projection` 和 `visual_indicator_data` 继续支撑 Vue/ECharts 页面。短期内不建议删除这些表，也不建议直接修改字段名。旧库字段以 `region_name` 为核心关联键，适合演示，难以承担正式数据治理。V2.0 使用 `region_id`、`site_id`、`source_id`、`version_id` 和 `qc_flag_id` 作为更稳定的关联结构。

旧页面后续可以通过两种方式接入 V2.0。第一种方式是在后端 Service 层把 V2.0 聚合表转换成旧接口需要的字段。第二种方式是在 `hainan_blue_carbon_local` 中使用视图输出类似 `region_yearly_carbon`、`region_flux_composition`、`region_species_composition` 和 `visual_indicator_data` 的结构。`hnblue_v2_schema.sql` 已经提供了 `v_legacy_region_yearly_carbon`、`v_legacy_region_flux_composition`、`v_legacy_region_species_composition` 和 `v_legacy_visual_indicator_data` 四个适配视图。

## 3. Schema 边界

`hainan_blue_carbon_core` 是 V2.0 的主数据和资产索引中心。它包含区域、项目、站点、数据来源、数据版本、质量标识、指标字典和资产索引。后续任何数据入库都应先登记来源，再登记版本和资产。区域不再只用中文名称关联，统一使用 `region_id` 和 `region_code`。站点、样地、通量塔、无人机样区和遥感网格可通过 `t_site` 统一登记。

`hainan_blue_carbon_ground` 承载地面调查数据，包括调查批次、样方、样木、土壤样品、样方物种组成和样方碳库汇总。样方基础信息与测量明细分开保存。这样可以同时支持真实样方调查、文献样方整理和模拟样方演示。土壤数据单独建表，便于表达不同深度层、实验方法和土壤有机碳密度。

`hainan_blue_carbon_flux` 承载通量塔数据，包括通量站点扩展信息、原始文件索引、30 分钟观测表、日尺度聚合表和质量事件表。30 分钟表适合对接涡度相关系统的常规统计结果。10Hz 高频文件、原始 CSV 和质控报告作为冷数据文件保存路径。日尺度聚合表用于前端趋势展示和报告材料生成。

`hainan_blue_carbon_satellite` 承载卫星遥感产品和遥感派生指标，包括产品元数据、区域统计指标、红树林覆盖面积和网格尺度碳储/冠层高度结果。GeoTIFF 文件本体通过资产索引记录路径。区域统计指标用于接入 NDVI、冠层高度、红树林面积、AGC 等轻量结果。

`hainan_blue_carbon_uav` 承载无人机任务和产品，包括航飞任务、产品元数据、样区统计、物种识别和碳储量反演结果。该结构把航飞任务与正射影像、DSM、DEM、CHM、点云、多光谱和高光谱产品分开，便于记录飞行日期、传感器、重叠度、定位方式和处理软件。

`hainan_blue_carbon_model` 承载 CatBoost、SHAP 和后续模型服务，包括模型注册、模型运行、预测结果和解释图件。它将本科论文阶段已经形成的碳储估算、SHAP解释、敏感性分析、响应曲线和虚拟样地实验纳入可追溯表结构。模型文件、图件和输出报告仍通过资产索引保存路径。

`hainan_blue_carbon_intl` 承载外部公开数据和文献数据，包括公开数据集登记、文献引用、外部观测记录、异速生长方程和文献碳储记录。该库服务于模型训练、参考区间、文献对比、物种参数查找和 AI 碳助手知识库补充。外部数据的许可证、引用格式、下载日期和网页地址必须记录。

`hainan_blue_carbon_local` 承载文昌示范区和海南本地展示聚合结果。它不承担原始数据存储职责。它主要保存文昌示范区简介、本地区域年度指标、本地可视化指标、模拟数据说明和旧页面适配视图。这个库是 V2.0 规范数据向 V1.0 页面和阶段性汇报材料输出的缓冲层。

## 4. 热数据与冷数据规则

热数据进入 MySQL。热数据包括区域、站点、样地、样木测量、土壤样品、通量 30 分钟统计、通量日尺度统计、遥感产品元数据、遥感区域统计、无人机任务元数据、无人机区域指标、模型运行记录、模型预测结果、指标字典、来源登记、版本记录和质量标识。这类数据体量可控，查询频率较高，适合通过 Spring Boot API 向前端输出。

冷数据进入文件目录、NAS、对象存储或 URL。冷数据包括 GeoTIFF、无人机正射影像、DSM、DEM、CHM、LAS/LAZ 点云、原始通量文件、10Hz 高频数据、原始 CSV、Excel、航飞照片、模型 pkl/cbm 文件、SHAP 图件、响应曲线图、PDF 报告和压缩包。MySQL 只保存 `storage_path`、`file_name`、`file_format`、`file_size_bytes`、`file_hash`、`spatial_wkt`、`start_time`、`end_time`、`source_id`、`version_id` 和 `data_status`。

当前开发阶段可以使用相对路径作为冷数据路径。例如 `data/ground/` 存地面样方 CSV，`data/flux/raw/` 存通量原始文件，`data/satellite/geotiff/` 存卫星产品，`data/uav/products/` 存无人机成果，`data/model/` 存模型和解释图件，`data/demo/` 存模拟数据。后续部署到服务器时，只需要更新资产索引中的路径，不需要改变业务表结构。

## 5. 浏览器检索数据的入库顺序

后续通过浏览器检索数据时，应先记录来源，再保存具体数据。第一步在 `core.t_data_source` 中记录数据名称、发布机构、网页地址、访问日期、下载日期、许可证、引用格式和是否模拟数据。第二步在 `core.t_data_version` 中记录版本编码、发布时间、入库时间、处理级别和处理方法。第三步如果存在文件，则在 `core.t_data_asset_index` 中登记文件路径、格式、大小、哈希、空间范围、时间范围和共享范围。第四步把清洗后的结构化结果写入对应业务 Schema。第五步把适合展示的区域年度指标写入 `local.t_local_region_yearly_metric` 或 `local.t_local_visualization_metric`。

真实数据、公开数据、文献数据、模型输出和模拟数据必须分开标记。模拟数据统一设置 `is_simulated = 1`，并使用 `source_category = 'SIMULATED'`。公开数据使用 `PUBLIC_DATASET`。文献提取数据使用 `LITERATURE`。模型结果使用 `MODEL_OUTPUT` 或 `DERIVED_PRODUCT`。实测数据使用 `IN_SITU`。这样可以在前端、报告和论文中明确区分不同可信度的数据。

## 6. V2.0 表结构文件的执行建议

`hnblue_v2_schema.sql` 是参考结构和开发环境初始化脚本。它包含 `DROP TABLE IF EXISTS`，适合在新建开发数据库中执行。若要在已有数据库上执行，应先备份当前数据库。对已有线上或演示环境，不建议直接运行整份脚本。更稳妥的方式是建立一个新的 MySQL 实例或新建空库运行，然后再把旧库中仍有价值的结构和模拟数据按需迁移。

若必须在当前 MySQL 环境中执行，应先导出当前所有 Schema 的结构和数据。然后在 Workbench 中新建连接或新建测试实例。待脚本执行成功后，再通过后端代码和 SQL View 检查旧页面需要的接口格式。V1.0 页面继续访问 `hnblue` 库，V2.0 数据采集和入库工作先进入 `hainan_blue_carbon_*` 规范库。

## 7. 数据填充优先级

第一优先级是补齐主数据。应先填 `t_region`、`t_site`、`t_indicator_dictionary`、`t_quality_flag`、`t_data_source` 和 `t_data_version`。没有这些表，后续任何数据都会缺少稳定归属。

第二优先级是填文昌示范区最小数据包。可先围绕文昌建立区域档案、模拟或公开来源说明、年度碳储量、通量展示指标、优势物种展示指标和基础遥感元数据。该步骤用于保持页面可展示，并为后续真实数据替换预留位置。

第三优先级是填公开数据和文献数据。公开遥感数据、红树林分布数据、文献碳储量记录、异速生长方程和 BAAD/Tallo 等参照数据可以先进入 `intl`、`satellite` 和 `model` 相关表。每条记录必须保留来源和引用格式。

第四优先级是等待真实地面、通量和无人机数据交付。真实数据进入系统后，先进入资产索引和对应业务表，再进入本地展示聚合表。展示层只保存结果，不替代原始记录。

## 8. 与前端和后端的关系

HNBLUE 1.0 只与 `hnblue` 库联动。V2.0 初期不强制改动前端。后端可以新增 V2.0 API，逐步从 `hainan_blue_carbon_local` 的聚合表或视图中读取数据。当 V2.0 数据稳定后，再逐步替换旧接口。这个策略可以保护已经做出的模拟可视化页面，同时让数据库走向正式的数据治理结构。

前端页面涉及的年度趋势、通量组成、物种组成、多指标对比、经济价值估算和区域简介，都可以由 `local.t_local_region_yearly_metric` 与 `local.t_local_visualization_metric` 承接。模型页面涉及的结构参数输入、估算输出、SHAP解释、敏感性分析和虚拟样地实验，可以由 `model.t_model_registry`、`model.t_model_run`、`model.t_model_prediction` 和 `model.t_model_explanation_asset` 承接。

## 9. 文件清单

本次生成的 SQL 文件为 `hnblue_v2_schema.sql`。它用于建立 V2.0 规范表。配套说明文件为 `docs/context/database_v2_design.md`。综合上下文文件为 `docs/context/04_database_and_data_context.md`。建议使用新的 `04_database_and_data_context.md` 替换旧版 04 文件，并保留 `database_v2_design.md` 作为更详细的数据库实施说明。
