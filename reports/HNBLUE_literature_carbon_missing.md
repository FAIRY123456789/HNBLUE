# HNBLUE V2.0 文献碳储缺失与阻塞报告

## 当前状态

已生成第四类 staging 文件，未写入 MySQL，未修改 DDL、后端、前端或旧 hnblue 数据库。

## 仍需全文或表格核验

1. `MENG2022_HAINAN_MANGROVE_CARBON_STOCKS`：题名、DOI、摘要值已核验；研究年份、样地设计、表号和完整方法仍需 PDF 复核。
2. `QIU2019_QINGLAN_UAVLIDAR_WV2_AGB`：已核验题名、DOI、研究区面积和模型精度；更完整 AGB 表格和异速方程需全文表格抽取，异速方程转第五类。
3. `GAO2019_CHINA_MANGROVE_SOC_FOUR_RESERVES`：已录入摘要 SOC 含量；SOC 分层、容重、盐度、pH、SOC 密度和储量需全文表格。
4. `DONGZHAIGANG_MFR2018_CARBON_STORAGE_FACTORS`：可检索摘要中包含总碳储和碳库分配，但本轮按候选任务处理，待全文表格确认后再写入 seed_records。

## 数据集下载与解析阻塞

1. `BAI2021_HAINAN_MANGROVE_DIVERSITY_CARBON_DRYAD`：Dryad DOI 和文件名已确认，未下载 `Data_Bai_etal_2021.rar`，未解析 README 和 Excel 字段。
2. `GLOBAL_MANGROVE_SOC_30M_2020`：Zenodo DOI 已确认；未下载 COG 栅格。本地需要海南省界、市县边界或红树林边界后才能裁剪统计。
3. `COASTAL_CARBON_LIBRARY_ATLAS`：USGS/GCB 来源已确认；未下载数据库表，尚不清楚是否包含海南或中国南部红树林样点。

## 中文候选文献

以下来源未写入 seed_records：

1. `LIN2015_QINGLAN_SONNERATIA_CARBON_DENSITY`：清澜港杯萼海桑生态系统碳密度表待提取。
2. `HU2015_DONGZHAIGANG_CARBON_STORAGE`：东寨港红树林湿地碳储与碳汇功能数值待提取。
3. `QINGLAN_SOC_PH_RELATION`：清澜港红树林 SOC 分层数据和 pH 关系待提取。
4. `DONGZHAIGANG_MFR2018_CARBON_STORAGE_FACTORS`：需全文表格核验后再结构化。

## pending_verification 项

1. Lin 2015、Hu 2015、Qinglan SOC-pH 候选来源缺少稳定 DOI、全文 URL 和表格证据。
2. Meng 2022 的完整方法细节和表号仍需全文核验。
3. Bai 2021 Dryad 压缩包未下载，字段定义、单位和样方级记录未核验。

## 空间计算阻塞

全球 30 m mangrove SOC 栅格需要海南边界、市县边界或红树林边界，并需使用 `rasterio/geopandas` 裁剪。当前仓库未发现可用边界文件，因此未写入任何由栅格裁剪得到的区域 SOC 数值。

## 质量边界

本轮没有将候选文献题名写成事实数值，没有将生物量转换为碳量，没有把模型情景预测写成观测事实，没有把海岸带湿地总碳储解释为红树林总碳储。
