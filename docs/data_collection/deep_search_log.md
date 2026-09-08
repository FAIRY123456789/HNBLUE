# 第二轮公开数据深挖检索日志

检索日期：2026-05-25。已确认 `sql/schema/hnblue_v2_schema.sql` 存在；本轮按真实 schema 语义生成 staging CSV。

## 检索关键词覆盖

已检索中文关键词：海南 红树林 碳储量、海南岛 红树林 碳储量、海南 红树林 土壤有机碳、海南 红树林 地上生物量、海南 红树林 地下生物量、文昌 八门湾 红树林 土壤有机碳、清澜港 红树林 碳储量、东寨港 红树林 碳储量、海南 蓝碳 温室气体 排放、海南 红树林 CH4 N2O CO2、海南 红树林 遥感 面积、海南 红树林 分布 数据、海南 红树植物 名录、中国 红树林 异速生长方程。

已检索英文关键词：Hainan mangrove carbon stock、Hainan mangrove soil organic carbon、Bamen Bay mangrove soil organic carbon、Qinglan Harbor mangrove carbon、Dongzhaigang mangrove carbon stock、Hainan blue carbon greenhouse gas、Hainan mangrove methane nitrous oxide、Hainan mangrove Global Mangrove Watch、Hainan mangrove Sentinel-2、Hainan mangrove MODIS GPP、mangrove allometric equation China、Komiyama mangrove allometric equation。

## 新增或复核公开来源

| id | URL | 类型 | 是否抽取数值 | 进入文件 |
|---|---|---|---|---|
| S01 | https://www.mdpi.com/2073-4441/14/20/3278 | 文昌八门湾 SOC 论文 | 是 | literature/species/region |
| S02 | https://www.frontiersin.org/journals/marine-science/articles/10.3389/fmars.2022.932984/full | 海南蓝碳 GHG 清单论文 | 是 | literature/flux/region |
| S03 | https://en.hainan.gov.cn/hainan/zmgyshj/202307/3f612bd40e1941a080bf7abef3b71c69.shtml | 海南省政府蓝碳页面 | 是 | literature/species/region |
| S04 | https://zenodo.org/records/6894273 | GMW v3.0 | 是，产品元数据 | remote_sensing |
| S05 | https://globalmangrovewatch.org/ | GMW 平台 | 是，平台元数据 | knowledge |
| S06 | https://developers.google.com/earth-engine/datasets/catalog/LANDSAT_MANGROVE_FORESTS | GEE 红树林 2000 | 是，产品元数据 | remote_sensing |
| S07 | https://developers.google.com/earth-engine/datasets/catalog/COPERNICUS_S2_SR_HARMONIZED | Sentinel-2 | 是 | remote_sensing |
| S08 | https://developers.google.com/earth-engine/datasets/catalog/LANDSAT_LC08_C02_T1_L2 | Landsat 8 | 是 | remote_sensing |
| S09 | https://developers.google.com/earth-engine/datasets/catalog/LANDSAT_LT05_C02_T1_L2 | Landsat 5 | 是 | remote_sensing |
| S10 | https://developers.google.com/earth-engine/datasets/catalog/LANDSAT_LE07_C02_T1_L2 | Landsat 7 | 是 | remote_sensing |
| S11 | https://developers.google.com/earth-engine/datasets/catalog/MODIS_061_MOD17A3HGF | MODIS GPP/NPP | 是 | remote_sensing/flux |
| S12 | https://developers.google.com/earth-engine/datasets/catalog/MODIS_061_MOD13Q1 | MODIS NDVI/EVI Terra | 是 | remote_sensing |
| S13 | https://developers.google.com/earth-engine/datasets/catalog/MODIS_061_MYD13Q1 | MODIS NDVI/EVI Aqua | 是 | remote_sensing |
| S14 | https://developers.google.com/earth-engine/datasets/catalog/MODIS_061_MCD12Q1 | MODIS Land Cover | 是 | remote_sensing |
| S15 | https://developers.google.com/earth-engine/datasets/catalog/JRC_GSW1_4_GlobalSurfaceWater | JRC 水体 | 是 | remote_sensing |
| S16 | https://developers.google.com/earth-engine/datasets/catalog/USGS_SRTMGL1_003 | SRTM DEM | 是 | remote_sensing |
| S17 | https://developers.google.com/earth-engine/datasets/catalog/COPERNICUS_DEM_GLO30 | Copernicus DEM | 是 | remote_sensing |
| S18 | https://developers.google.com/earth-engine/datasets/catalog/LARSE_GEDI_GEDI02_A_002 | GEDI L2A | 是 | remote_sensing |
| S19 | https://developers.google.com/earth-engine/datasets/catalog/LARSE_GEDI_GEDI04_A_002 | GEDI L4A | 是 | remote_sensing |
| S20 | https://developers.google.com/earth-engine/datasets/catalog/UCSB-CHG_CHIRPS_DAILY | CHIRPS | 是 | remote_sensing |
| S21 | https://developers.google.com/earth-engine/datasets/catalog/ECMWF_ERA5_LAND_DAILY_AGGR | ERA5-Land | 是 | remote_sensing/flux |
| S22 | https://developers.google.com/earth-engine/datasets/catalog/IDAHO_EPSCOR_TERRACLIMATE | TerraClimate | 是 | remote_sensing |
| S23 | https://developers.google.com/earth-engine/datasets/catalog/ESA_WorldCover_v200 | ESA WorldCover | 是 | remote_sensing |
| S24 | https://developers.google.com/earth-engine/datasets/catalog/GOOGLE_DYNAMICWORLD_V1 | Dynamic World | 是 | remote_sensing |
| S25 | https://developers.google.com/earth-engine/datasets/catalog/NASA_NASADEM_HGT_001 | NASADEM | 是 | remote_sensing |
| S26 | https://developers.google.com/earth-engine/datasets/catalog/WORLDCLIM_V1_BIO | WorldClim BIO | 是 | remote_sensing |
| S27 | https://www.ipcc-nggip.iges.or.jp/public/wetlands/ | IPCC Wetlands | 方法 | knowledge |
| S28 | https://www.ipcc-nggip.iges.or.jp/public/2006gl/ | IPCC 2006 | 方法 | knowledge |
| S29 | https://www.thebluecarboninitiative.org/manual | Blue Carbon Manual | 方法 | knowledge |
| S30 | https://verra.org/methodologies/vm0033-methodology-for-tidal-wetland-and-seagrass-restoration-v2-1/ | Verra VM0033 | 方法 | knowledge |
| S31 | https://link.springer.com/article/10.1007/s11284-005-0088-4 | Komiyama allometry | 是 | species |
| S32 | https://github.com/dfalster/baad | BAAD | 是，字段级 | species |
| S33 | https://zenodo.org/ | Tallo 检索入口 | 字段级待版本锁定 | species/knowledge |
| S34 | https://daac.ornl.gov/ | NASA ORNL DAAC | 数据入口 | knowledge |
| S35 | https://lpdaac.usgs.gov/ | NASA LP DAAC | 数据入口 | knowledge |
| S36 | https://browser.dataspace.copernicus.eu/ | Copernicus Browser | 数据入口 | knowledge |
| S37 | https://earthexplorer.usgs.gov/ | USGS EarthExplorer | 数据入口 | knowledge |
| S38 | https://www.usgs.gov/landsat-missions/landsat-collection-2-level-2-science-products | USGS Landsat C2 L2 | 是 | remote_sensing |
| S39 | https://www.jorae.cn/CN/10.5814/j.issn.1674-764x.2022.03.010 | 海南红树林碳储摘要页 | 待全文 | unresolved |
| S40 | https://en.hainan.gov.cn/hainan/zdjsxm/202510/d9dd6ae78ac74071aa8995d56dd633e7.shtml | 海南红树林修复工程页面 | 项目线索 | region |
