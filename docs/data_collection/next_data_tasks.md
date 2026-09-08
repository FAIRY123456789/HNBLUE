# HNBLUE V2.0 后续数据任务清单

## Phase 0：Schema 复核和导入准备

1. 补齐 `hnblue_v2_schema.sql`，确认 `hainan_blue_carbon_*` Schema、表名、字段、主键、外键和枚举。
2. 将本轮 CSV 字段与正式 schema 做一轮字段对齐，生成可执行导入脚本。
3. 为 `source_category`、`quality_level`、`is_simulated`、`is_proxy` 建立枚举或检查约束。
4. 建立冷数据路径规范，例如 `cold_data/remote_sensing/gmw/v3/`、`cold_data/uav/wenchang/`、`cold_data/literature/pdf/`。

## Phase 1：公开数据精化

1. 下载或在线裁剪 GMW v3.0 海南和文昌范围红树林分布，生成区域面积统计 CSV。
2. 编写 GEE 脚本，基于 Sentinel-2 和 Landsat 计算文昌示范区 NDVI、NDWI、MNDWI 和红树林掩膜候选。
3. 基于 MODIS MOD17A3HGF 计算文昌红树林区域 GPP/NPP 年度代理序列，所有结果标记 `is_proxy=1`。
4. 对 MDPI 文昌八门湾 SOC 论文逐表核对，补录表号、页码、原始单位和标准单位。

## Phase 2：项目组真实数据接入

1. 获取文昌样方调查 Excel，生成 `ground.t_survey_batch`、`ground.t_plot`、`ground.t_tree_measurement`、`ground.t_soil_sample` 的导入模板。
2. 获取通量塔 30 分钟数据样例，建立字段校验脚本，保留 QC flag、缺测标识和仪器状态。
3. 获取无人机正射影像、DSM、CHM、点云和航飞日志，生成资产索引和区域统计表。
4. 获取保护区边界或示范区矢量范围，替换本轮区域对象中的近似中心点。

## Phase 3：平台功能接入

1. 实现 V2.0 数据来源目录 API，只读返回 `t_data_source`、`t_data_version` 和 `t_data_asset_index`。
2. 实现文昌示范区数据包页面，展示来源、质量等级、可下载状态和待接入状态。
3. 实现遥感产品元数据列表和冷数据索引，不直接加载大体量 GeoTIFF。
4. 将 IPCC、Blue Carbon Initiative、Verra 和 HNBLUE 字段口径整理为 AI 碳助手知识库文档。
