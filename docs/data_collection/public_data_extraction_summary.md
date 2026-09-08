# 第二轮公开数值抽取汇总

| 文件 | 记录数 | 主要内容 |
|---|---:|---|
| `data/staging/literature/literature_carbon_records.csv` | 35 | 文昌八门湾 SOC、海南 EF_AGB/EF_SOC、全省碳密度、样本量、红树林面积 |
| `data/staging/flux/flux_or_proxy_candidate_records.csv` | 22 | 海南和市县 GHG 清单、文昌 N2O、MODIS GPP/NPP proxy |
| `data/staging/remote_sensing/remote_sensing_product_candidates.csv` | 24 | GMW、Sentinel、Landsat、MODIS、GEDI、DEM、CHIRPS、ERA5 等产品 |
| `data/staging/remote_sensing/remote_sensing_metric_plan.csv` | 12 | 可通过 GEE/GDAL 生成的区域指标 |
| `data/staging/species/species_and_allometry_records.csv` | 32 | 海南/文昌物种、Komiyama 方程、BAAD/Tallo 字段、根冠比 |
| `data/staging/core/region_and_project_records.csv` | 21 | 海南、文昌、八门湾面积、bbox、调查样本量、区域 GHG proxy |

本轮总记录数：146 条。其中明确数值记录 112 条，产品元数据和字段存在记录 34 条。所有记录 `is_simulated=0`。

## 可直接进入 V2.0 的记录

1. `literature_carbon_records.csv` 中 Bamen Bay SOC、Hainan EF_AGB/EF_SOC、Hainan mangrove area。
2. `remote_sensing_product_candidates.csv` 中 24 条产品元数据。
3. `region_and_project_records.csv` 中八门湾面积、bbox、海南红树林面积、调查样本量。
4. `species_and_allometry_records.csv` 中 Komiyama 方程系数和海南清单根冠比。

## 必须作为 proxy 的记录

1. `flux_or_proxy_candidate_records.csv` 全部通量相关记录均为 proxy 或 GHG 清单指标。
2. MODIS GPP/NPP、ERA5 蒸散等遥感/再分析记录不得写成通量塔实测。
