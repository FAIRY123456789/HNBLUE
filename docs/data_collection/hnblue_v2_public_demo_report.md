# HNBLUE V2.0 Public Data Demo Report

## 本轮完成

- 新增静态展示页：`docs/demo/hnblue_v2_public_data_demo.html`。
- 页面直接读取 `data/results/hnblue_v2_public/normalized/` 下的 JSON/CSV，不连接数据库。
- 新增真实数值记录 27 条，写入 `display_mangrove_area_timeseries.csv`，同步写入 `frontend_mangrove_area_timeseries.json` 的 `supplement_metrics`。
- 新增指标来自 MDPI Forests 2023 Hainan mangrove remote sensing 已发表面积时序的直接派生：2020 跟踪区域面积占比、2020 面积排名、2020 面积指数(1990=100)。
- 所有新增记录 `is_simulated=0`，`is_proxy=0`。

## 展示模块

- 海南红树林面积时序：读取 `frontend_mangrove_area_timeseries.json`，展示主要区域 1990-2020 面积曲线与 2020 区域占比。
- 遥感/土地利用/碳储 proxy 指标：读取 `frontend_remote_sensing_metrics.json`，展示土地利用、InVEST 碳储、变化量指标；proxy 保持标识。
- 文献碳储证据卡片：读取 `frontend_literature_carbon_cards.json`，展示 SOC、AGB、BGB、总碳密度、GHG 清单 proxy。
- 数据来源与质量等级：读取 `display_data_sources.csv`，展示 source_id、质量等级、记录数与 proxy 数。

## 校验结果

```text
display_mangrove_area_timeseries.csv: records=172, regions=12, indicators=13, proxy_count=0, simulated_sum=0
display_remote_sensing_metrics.csv: records=147, regions=10, indicators=49, proxy_count=30, simulated_sum=0
display_literature_carbon_records.csv: records=73, regions=14, indicators=31, proxy_count=19, simulated_sum=0
display_species_allometry_records.csv: records=22, regions=3, indicators=6, proxy_count=0, simulated_sum=0
display_data_sources.csv: records=9, regions=0, indicators=0, proxy_count=0, simulated_sum=0
frontend_mangrove_area_timeseries.json: records=93, regions=11, indicators=4, proxy_count=0, simulated_sum=0
frontend_remote_sensing_metrics.json: records=147, regions=10, indicators=49, proxy_count=30, simulated_sum=0
frontend_literature_carbon_cards.json: records=73, regions=14, indicators=31, proxy_count=19, simulated_sum=0
```

## 可直接用于 HNBLUE V2.0

- 面积时序、面积占比、面积排名、面积指数可直接用于前端趋势页、区域对比页和展示总览。
- 土地利用面积/比例、碳储模型结果、文献碳储记录可直接用于公开数据证据页。
- `display_data_sources.csv` 与 `display_region_dictionary.csv` 可作为 V2.0 入库映射前置字典。

## 需要后续入库映射

- 面积和遥感指标建议映射到 satellite 或外部观测事实表。
- 文献碳储记录建议映射到 `hainan_blue_carbon_intl.t_literature_carbon_record`。
- 物种与异速方程记录建议映射到 species/allometry 相关表。
- proxy 指标入库时必须保留 `is_proxy=1`，不得写作通量塔观测。

## 后续计算依赖

- GMW 文昌/八门湾精确面积仍需 GeoJSON 边界与 GEE 认证。
- MODIS GPP/NPP、CHIRPS、ERA5、GEDI/NASADEM 区域统计仍需运行 `scripts/geospatial/gee_hnblue_public_metrics.js`。

## 页面打开路径

- 本地 HTTP 路径：`http://127.0.0.1:8098/docs/demo/hnblue_v2_public_data_demo.html`。
- 静态文件路径：`docs/demo/hnblue_v2_public_data_demo.html`。
- 浏览器插件尝试打开本地 HTTP 路径时被安全策略拒绝；本轮已通过 JSON/CSV 解析校验确认展示数据可读取。
