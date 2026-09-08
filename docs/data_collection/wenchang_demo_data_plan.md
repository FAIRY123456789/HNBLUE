# 文昌示范数据准备计划

## 数据域拆分

| 数据域 | 当前可用公开来源 | 质量等级 | 入库路径 | 当前处理策略 |
|---|---|---|---|---|
| 区域对象 | 海南省政府页面、国家林草局/海南规划页面、GMW 空间产品 | A/B | core.t_region, core.t_data_source | 先登记海南省、文昌市、文昌八门湾、清澜港红树林等区域对象。未核验边界不写 WKT，只写空间说明。 |
| 红树林分布 | GMW v3.0、Sentinel-2、Landsat | A | satellite.t_satellite_product_metadata, core.t_data_asset_index | 只登记产品元数据、下载链接、时间范围、冷数据路径建议。 |
| 碳储量和 SOC | MDPI Water 文昌八门湾 SOC 论文、Frontiers 海南省级蓝碳清单论文、JORE 海南碳储量评估论文 | A/B | intl.t_literature_carbon_record, ground.t_soil_sample_candidate | 只录区域统计或论文记录。没有公开样木明细时不生成样木记录。 |
| 通量或替代通量 | Frontiers GHG 清单论文、MODIS GPP/NPP | A | flux.t_flux_or_proxy_candidate | MODIS GPP/NPP 写入 is_proxy=1。论文 GHG 记录保持文献候选，不混写通量塔观测。 |
| 物种与异速生长 | BAAD、Tallo、论文摘要页 | A/B | intl.t_allometric_dataset, model.t_model_reference_dataset | 作为模型参考数据，不写成海南本地实测。 |
| 无人机遥感 | 海南省政府页面披露已有无人机遥感调查线索 | B | uav.t_uav_product_or_method_candidate, core.t_data_asset_index | 本轮没有公开正射影像或点云下载，只登记待接入需求。 |
| AI 知识库 | IPCC Wetlands Supplement、Blue Carbon Initiative Manual、Verra VM0033 | A | knowledge.blue_carbon_knowledge_sources | 只进入知识库候选清单，不直接作为数值数据入库。 |

## 文昌区域对象策略

1. `REG-HI-0001`：海南省，省级区域对象，中心点使用公开常识级省域中心近似值，后续由行政区划边界替换。
2. `REG-WC-0001`：文昌市，县市级区域对象，中心点使用公开常识级城市中心近似值，后续由行政区划边界替换。
3. `REG-WC-BMB-001`：文昌八门湾红树林相关区域。公开来源明确出现八门湾红树林和蓝碳调查线索，但本轮未取得法定边界 WKT，暂不写 bbox。
4. `REG-WC-QLG-001`：清澜港红树林相关区域。公开来源出现清澜港、清澜红树林保护区语境，行政归属和边界需项目组或主管部门材料复核。

## 可直接进入下一轮导入测试的文件

| 文件 | 用途 | 导入前复核 |
|---|---|---|
| `data/staging/core/seed_data_sources.csv` | 数据来源主表种子 | 按实际 `t_data_source` 字段复核 |
| `data/staging/core/seed_regions.csv` | 文昌示范区域对象 | 用正式行政区划和保护地边界替换近似中心点 |
| `data/staging/core/seed_asset_index.csv` | 遥感和文档资产索引 | 文件大小、哈希、冷数据路径待落地 |
| `data/staging/literature/literature_carbon_records.csv` | 文献碳储记录候选 | 表号、页码、数值字段需人工核验后录入 |
| `data/staging/remote_sensing/remote_sensing_product_candidates.csv` | 遥感产品候选 | 可直接进入产品元数据表 |
| `data/staging/flux/flux_or_proxy_candidate_records.csv` | 通量或替代通量候选 | MODIS 类记录保持 `is_proxy=1` |

## 需要线下提供的数据

| 数据 | 必要字段 | 目标表 | 当前状态 |
|---|---|---|---|
| 文昌真实样方调查表 | 样方编号、经纬度、调查日期、物种、胸径、树高、冠幅、土壤深度、干重、碳含量 | ground.* | 公开来源未提供充分信息 |
| 通量塔连续观测 | 站点、时间戳、NEE/NEP/GPP/Reco、CO2/CH4/N2O、QC flag、缺测标识 | flux.* | 当前公开来源未找到海南红树林通量塔开放数据 |
| 无人机正射影像和点云 | 任务编号、航飞日期、分辨率、坐标系、文件路径、处理软件、质量报告 | uav.* | 公开页面仅提供项目线索 |
| 保护区边界 | WKT/GeoJSON、行政编码、保护地级别、批准文号 | core.t_region, core.t_data_asset_index | 需主管部门或公开矢量文件 |
