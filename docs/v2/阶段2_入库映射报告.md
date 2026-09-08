# HNBLUE V2.0 Phase 2 公开数据受控入库映射报告

本报告基于 `docs/v2/HNBLUE_V2_phase1_handoff_report.md`、6 个 normalized display CSV 和 `sql/schema/hnblue_v2_schema.sql` 生成。

**本轮未连接 MySQL，未执行导入。**

## 1. 生成产物

| 文件 | 目标 Schema / 表 | 记录数 | proxy 记录数 | is_simulated 合计 | 入库定位 |
|---|---|---:|---:|---:|---|
| `core_data_sources_import_ready.csv` | `hainan_blue_carbon_core.t_data_source` | 9 | 2 | 0 | import_ready CSV，等待人工审核后执行 SQL |
| `core_regions_import_ready.csv` | `hainan_blue_carbon_core.t_region` | 25 | 0 | 0 | import_ready CSV，等待人工审核后执行 SQL |
| `satellite_mangrove_cover_import_ready.csv` | `hainan_blue_carbon_satellite.t_satellite_mangrove_cover` | 66 | 0 | 0 | import_ready CSV，等待人工审核后执行 SQL |
| `satellite_region_metric_import_ready.csv` | `hainan_blue_carbon_satellite.t_satellite_region_metric` | 147 | 30 | 0 | import_ready CSV，等待人工审核后执行 SQL |
| `intl_literature_carbon_record_import_ready.csv` | `hainan_blue_carbon_intl.t_literature_carbon_record` | 33 | 0 | 0 | import_ready CSV，等待人工审核后执行 SQL |
| `intl_allometry_equation_import_ready.csv` | `hainan_blue_carbon_intl.t_allometry_equation` | 2 | 0 | 0 | import_ready CSV，等待人工审核后执行 SQL |

- SQL preview：`sql/seed/hnblue_v2_public_import_preview.sql`
- 拒收清单：`docs/v2/HNBLUE_V2_phase2_rejected_records.md`，共 167 条。
- SQL 文件仅为预览，不包含连接串，不由脚本执行。

## 2. 字段映射

| 源 CSV | 入库 CSV | 源字段 | 目标表字段 | 映射规则 | 风险控制 |
|---|---|---|---|---|---|
| `display_data_sources.csv` | `core_data_sources_import_ready.csv` | `source_id` | `source_code` | 保留自然来源编码；V2 `source_id` 由 MySQL 自增 | 不把自然编码写入自增主键 |
| `display_data_sources.csv` | `core_data_sources_import_ready.csv` | `source_type` | `source_category` | peer reviewed / public / government 类型映射到 schema enum | 原始 `source_type` 同步保留 |
| `display_data_sources.csv` | `core_data_sources_import_ready.csv` | `record_count`,`proxy_count`,`direct_count` | `remark` / import audit fields | 作为来源层级质量统计 | `proxy_count` 不等于观测表行级 proxy |
| `display_region_dictionary.csv` | `core_regions_import_ready.csv` | `region_id` | `region_code` | 保留自然区域编码；V2 `region_id` 由 MySQL 自增 | 后续 FK 使用 `region_code` 子查询解析 |
| `display_region_dictionary.csv` | `core_regions_import_ready.csv` | `parent_region_id` | `parent_region_id` | 通过父级 `region_code` 子查询解析 | 空父级保持 NULL |
| `display_region_dictionary.csv` | `core_regions_import_ready.csv` | `needs_boundary_file` | import audit field / `remark` | 边界缺失只作为入库风险标记 | 不伪造 geometry_wkt |
| `display_mangrove_area_timeseries.csv` | `satellite_mangrove_cover_import_ready.csv` | `value`,`unit` | `mangrove_area_ha` | 仅接收 `MANGROVE_AREA` 且单位为 ha/hm2 的绝对面积 | index/share/rank/change/revegetation 已暂缓 |
| `display_mangrove_area_timeseries.csv` | `satellite_mangrove_cover_import_ready.csv` | `year_start` | `metric_year` | 年份转整数 | 非年度值不导入 cover 表 |
| `display_remote_sensing_metrics.csv` | `satellite_region_metric_import_ready.csv` | `indicator_code` | `indicator_code` | 同步生成 indicator dictionary 预览 SQL | proxy 继续保留在 import_ready 与 SQL 注释中 |
| `display_remote_sensing_metrics.csv` | `satellite_region_metric_import_ready.csv` | `value`,`unit` | `value`,`unit` | 只接收非空真实数值 | MODIS/ERA5/CHIRPS/模型结果不改写为实测 |
| `display_literature_carbon_records.csv` | `intl_literature_carbon_record_import_ready.csv` | `indicator_code` | `carbon_pool` | SOC/soil -> SOC，AGB -> AGB，belowground -> BGB，total/density -> TOTAL | GHG、面积、气候、样本量、trait 不进入碳储表 |
| `display_literature_carbon_records.csv` | `intl_literature_carbon_record_import_ready.csv` | `value`,`unit` | `value_mg_ha` | 仅接收 MgC/ha 或 Mg/ha 碳密度值 | MgC 总量、MgCO2e/yr 排放量暂缓 |
| `display_species_allometry_records.csv` | `intl_allometry_equation_import_ready.csv` | `EQUATION_A`,`EQUATION_B` | `coefficient_a`,`coefficient_b`,`coefficient_c` | Komiyama 2005 系数组合成 2 条完整方程 | 物种存在、物种数、root-shoot ratio 暂缓 |

## 3. 关键约束

- `is_simulated` 全部为 0。
- proxy 数据在 import_ready CSV 中保留 `is_proxy=1`；目标 schema 当前无统一 `is_proxy` 字段，SQL preview 在行级注释保留该标记。
- 文献区域均值只进入 `hainan_blue_carbon_intl.t_literature_carbon_record`，不得写入 ground 样方、样木、土壤样品表。
- 三个 `frontend_*.json` 不进入数据库，只作为前端展示派生文件。
- `sql/seed/hnblue_v2_public_import_preview.sql` 是人工审阅用 SQL，当前脚本不会连接 MySQL，也不会执行导入。

## 4. 推荐执行顺序

1. 审阅 `core_data_sources_import_ready.csv` 与 `core_regions_import_ready.csv`。
2. 确认 region/source 自然编码与 V2 主键映射。
3. 执行 indicator dictionary、literature reference 预置 SQL。
4. 导入 satellite 与 intl 事实表。
5. 生成 local/display 兼容层，逐步替换旧模拟展示数据。
