# HNBLUE V2.0 Phase 3 测试库最小导入前核验报告

本报告面向 Phase 2 SQL preview 的测试库导入前核验。当前分支未获得明确 MySQL 连接权限，因此本轮只生成核验 SQL、最小 core 导入 SQL 草案和操作说明。

**本轮未连接 MySQL，未执行导入，写入数据库行数为 0。**

## 1. 生成文件

| 文件 | 作用 | 是否执行 |
|---|---|---|
| `sql/seed/hnblue_v2_phase3_preflight_check.sql` | 只读检查目标 schema、表、字段和导入前行数 | 未执行 |
| `sql/seed/hnblue_v2_phase3_core_minimal_import_preview.sql` | 仅包含 `t_data_source` 与 `t_region` 的最小导入 SQL 草案 | 未执行 |
| `docs/v2/HNBLUE_V2_phase3_minimal_import_check_report.md` | 本报告 | 已生成 |

## 2. 当前 MySQL 目标表状态

未连接 MySQL，无法直接确认当前实例中 `hainan_blue_carbon_core`、`hainan_blue_carbon_satellite`、`hainan_blue_carbon_intl` 是否已经创建。`sql/schema/hnblue_v2_schema.sql` 静态定义中包含以下目标表：

- `hainan_blue_carbon_core.t_data_source`
- `hainan_blue_carbon_core.t_region`
- `hainan_blue_carbon_satellite.t_satellite_mangrove_cover`
- `hainan_blue_carbon_satellite.t_satellite_region_metric`
- `hainan_blue_carbon_intl.t_literature_reference`
- `hainan_blue_carbon_intl.t_literature_carbon_record`
- `hainan_blue_carbon_intl.t_allometry_equation`

运行 `sql/seed/hnblue_v2_phase3_preflight_check.sql` 可在测试库中得到真实存在状态和导入前行数。

## 3. 字段匹配静态审计

| import_ready CSV | 目标表 | 记录数 | 目标字段 | schema 静态匹配 | Phase 3 动作 |
|---|---|---:|---|---|---|
| `core_data_sources_import_ready.csv` | `hainan_blue_carbon_core.t_data_source` | 9 | `source_code, source_category, source_name, source_url, citation_text, version_label, is_simulated, remark` | 匹配 | 可执行 |
| `core_regions_import_ready.csv` | `hainan_blue_carbon_core.t_region` | 25 | `region_code, parent_region_id, region_name, region_level, province, city, county, ecosystem_type, data_scope, remark` | 匹配 | 可执行 |
| `satellite_mangrove_cover_import_ready.csv` | `hainan_blue_carbon_satellite.t_satellite_mangrove_cover` | 66 | `region_id, metric_year, mangrove_area_ha, classification_method, source_id` | 匹配 | 暂缓 |
| `satellite_region_metric_import_ready.csv` | `hainan_blue_carbon_satellite.t_satellite_region_metric` | 147 | `region_id, indicator_code, metric_year, value, unit, stat_method, source_id, is_simulated` | 匹配 | 暂缓 |
| `intl_literature_carbon_record_import_ready.csv` | `hainan_blue_carbon_intl.t_literature_carbon_record` | 33 | `reference_id, region_id, site_name, ecosystem_type, carbon_pool, value_mg_ha, depth_top_cm, depth_bottom_cm, method_note, source_id` | 匹配 | 暂缓 |
| `intl_allometry_equation_import_ready.csv` | `hainan_blue_carbon_intl.t_allometry_equation` | 2 | `reference_id, scientific_name, equation_form, component, coefficient_a, coefficient_b, coefficient_c, unit_note` | 匹配 | 暂缓 |

## 4. 可执行与暂缓 SQL

可执行前置条件：测试库中已经创建 V2 schema；预检 SQL 返回 core 目标表和所需字段全部 `EXISTS`；确认不会连接或写入旧 `hnblue` 库。

本轮准备的最小可执行 SQL：

- `hainan_blue_carbon_core.t_data_source`：9 行，来源字典。
- `hainan_blue_carbon_core.t_region`：25 行，区域字典。

本轮明确暂缓 SQL：

- `hainan_blue_carbon_satellite.t_satellite_mangrove_cover`：暂缓事实表导入。
- `hainan_blue_carbon_satellite.t_satellite_region_metric`：暂缓事实表导入；其中 proxy 记录不得作为真实观测。
- `hainan_blue_carbon_intl.t_literature_reference`：暂缓，需先确认 source FK。
- `hainan_blue_carbon_intl.t_literature_carbon_record`：暂缓；文献区域均值不得写入样方、样木或土壤样品表。
- `hainan_blue_carbon_intl.t_allometry_equation`：暂缓，需确认 reference FK。

## 5. 外键顺序

建议顺序：

1. `hainan_blue_carbon_core.t_data_source`
2. `hainan_blue_carbon_core.t_region`，先插入父级为空的区域，再插入带 `parent_region_code` 的子区域
3. 暂停，人工核验 `source_code` 与 `region_code` 到自增主键的映射
4. 后续批次再处理 satellite 与 intl 事实表

## 6. 回滚或清理

`hnblue_v2_phase3_core_minimal_import_preview.sql` 底部提供按 `source_code` 与 `region_code` 清理的注释 SQL。若在事务中手工执行，可在确认前使用 `ROLLBACK`。若已经提交，先确认无事实表外键依赖，再按以下顺序清理：

1. 删除 `hainan_blue_carbon_core.t_region` 中本批次 `region_code`。
2. 删除 `hainan_blue_carbon_core.t_data_source` 中本批次 `source_code`。
3. 再次运行 preflight row count 检查。

## 7. 约束确认

- 未写旧 `hnblue` 库。
- 未导入 proxy 事实数据。
- 未导入 satellite 或 intl 事实表。
- 未导入文献区域均值到样方、样木或土壤样品表。
- 未修改前端、后端、Flask、Redis、AnythingLLM。

## 8. 本机 MySQL 真实导入补充记录

2026-05-28 已基于用户提供的本地 MySQL root 凭据完成 Phase 3 core 最小真实导入。上一轮“未连接 MySQL、写入 0 行”是当时权限状态下的阶段性记录；该状态已被本轮真实导入验证更新。

新增报告：`docs/v2/HNBLUE_V2_phase3_mysql_core_minimal_import_report.md`

本轮真实导入结果：

- 成功连接本机 MySQL。
- schema 初始化已执行，使用去除 DROP 的 `CREATE TABLE IF NOT EXISTS` 安全变体。
- 仅写入 `hainan_blue_carbon_core.t_data_source` 与 `hainan_blue_carbon_core.t_region`。
- `t_data_source` 从 0 行到 9 行，实际新增 9 行。
- `t_region` 从 0 行到 25 行，实际新增 25 行。
- 本批次 source_code 命中数为 9。
- 本批次 region_code 命中数为 25。
- satellite 与 intl 事实表继续暂缓，目标事实表行数保持 0。
- 旧 `hnblue` 库仅做只读 guard，未作为写入目标。
- 未在报告、SQL、脚本或 Git diff 中写入明文密码。
