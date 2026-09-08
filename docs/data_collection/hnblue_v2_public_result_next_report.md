# HNBLUE V2.0 Public Result Next Report

## 本轮新增真实数值记录

- 新增派生/补充数值记录：120 条。
- 面积变化与八门湾组成补充：78 条。
- 遥感/模型表格变化量补充：30 条。
- 文献碳储量公式派生补充：12 条。
- 所有新增记录均来自已锁定公开来源的已发表数值或其直接差分、比例、公式计算，`is_simulated=0`。

## Normalized 文件校验

### `display_mangrove_area_timeseries.csv`
- records: 145
- indicators: 10
- regions: 12
- year range: 1990 - 2022
- non-null numeric values: 145
- proxy count: 0
- simulated sum: 0

### `display_remote_sensing_metrics.csv`
- records: 147
- indicators: 49
- regions: 10
- year range: 1990 - 2020
- non-null numeric values: 147
- proxy count: 30
- simulated sum: 0

### `display_literature_carbon_records.csv`
- records: 73
- indicators: 31
- regions: 14
- year range: 2005 - 2022
- non-null numeric values: 73
- proxy count: 19
- simulated sum: 0

### `display_species_allometry_records.csv`
- records: 22
- indicators: 6
- regions: 3
- year range: 2005 - 2022
- non-null numeric values: 22
- proxy count: 0
- simulated sum: 0

## 可直接用于 V2.0 前端展示

- `display_mangrove_area_timeseries.csv` 与 `frontend_mangrove_area_timeseries.json`：海南岛 9 个红树林区域 1990-2020 面积时序，海南省 2010/2020 面积，八门湾 2022 面积与天然/人工组成。
- `display_remote_sensing_metrics.csv` 与 `frontend_remote_sensing_metrics.json`：海南岛土地利用面积/比例、红树林面积、InVEST 碳储量与变化量。
- `display_literature_carbon_records.csv` 与 `frontend_literature_carbon_cards.json`：SOC、AGB、BGB、总碳密度、GHG 清单 proxy、样本量等卡片数据。
- `display_species_allometry_records.csv`：八门湾物种出现记录、海南/八门湾物种数、Komiyama 红树林异速方程参数、根冠比。

## 只能作为 proxy 的数据

- GHG 清单排放、N2O 水产养殖清单、InVEST 碳储量变化、GEE 脚本中的 MODIS GPP/NPP、CHIRPS、ERA5、DEM 指标均保持 `is_proxy=1`。
- 未写入任何通量塔观测字段，未将区域均值拆分为样方、样木或土壤样品。

## 适合后续入库

- `display_data_sources.csv` -> `hainan_blue_carbon_core.t_data_source`。
- `display_region_dictionary.csv` -> `hainan_blue_carbon_core.t_region` 的候选字典层。
- 面积与遥感指标 -> `hainan_blue_carbon_satellite` 或统一外部观测事实表。
- 文献碳储量与异速方程 -> `hainan_blue_carbon_intl.t_literature_carbon_record`、`t_allometry_equation`。

## 仍需 GEE 或边界文件进一步计算

- 文昌市和八门湾精确 GMW 1996/2007-2010/2015-2020 面积，需要行政边界或保护区边界 GeoJSON。
- MODIS GPP/NPP、CHIRPS、ERA5、GEDI/NASADEM 区域统计，需要 GEE 认证并运行 `scripts/geospatial/gee_hnblue_public_metrics.js`。
- 当前浏览器对锁定论文域名访问被策略拒绝，本轮未通过网页继续抽表。

## 浏览器阻塞证据

- Browser opening https://www.mdpi.com/1999-4907/14/11/2217 was rejected by browser security policy.
- Browser opening https://www.frontiersin.org/journals/marine-science/articles/10.3389/fmars.2022.932984/full was rejected by browser security policy.
