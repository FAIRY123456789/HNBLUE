# HNBLUE V2.0 Phase 3 MySQL core 最小真实导入报告

生成日期：2026-05-28
工作目录：`D:/ProgramData/Postgraduate/HNBLUE`

## 1. 结论

本轮已成功连接本机 MySQL，并完成 HNBLUE V2.0 测试库 core 最小真实导入。

- 连接方式：MySQL CLI，使用用户提供的本地 root 凭据，通过临时进程环境变量传入密码。
- 密码处理：未写入仓库文件、SQL、报告或 Git diff；报告不记录明文密码。
- 写入目标：仅 `hainan_blue_carbon_core.t_data_source` 与 `hainan_blue_carbon_core.t_region`。
- 未写入目标：旧 `hnblue`、`hainan_blue_carbon_satellite`、`hainan_blue_carbon_intl`。
- 导入结果：本批次 source_code 命中 9，region_code 命中 25。

## 2. 执行文件

| 文件 | 用途 | 执行状态 |
|---|---|---|
| `sql/seed/hnblue_v2_phase3_preflight_check.sql` | 只读 preflight，检查 schema、表、字段、行数与旧库 guard | 已执行 |
| `.codex-run/hnblue_v2_schema_init_if_missing.sql` | 从 `sql/schema/hnblue_v2_schema.sql` 生成的临时安全初始化 SQL，去除 DROP，使用 `CREATE TABLE IF NOT EXISTS` | 已执行 |
| `sql/seed/hnblue_v2_phase3_core_minimal_import_preview.sql` | core 最小导入 SQL，仅写 `t_data_source` 与 `t_region` | 已执行 |
| `.codex-run/hnblue_v2_phase3_post_import_checks.sql` | 导入后完整性复核 SQL | 已执行 |

证据文件：

- `docs/v2/HNBLUE_V2_phase3_mysql_preflight_result.txt`
- `docs/v2/HNBLUE_V2_phase3_mysql_v2_schema_existing_tables_before_init.txt`
- `docs/v2/HNBLUE_V2_phase3_mysql_schema_init_result.txt`
- `docs/v2/HNBLUE_V2_phase3_mysql_preflight_result_after_schema_init.txt`
- `docs/v2/HNBLUE_V2_phase3_mysql_before_import_counts.txt`
- `docs/v2/HNBLUE_V2_phase3_mysql_core_import_execution_result.txt`
- `docs/v2/HNBLUE_V2_phase3_mysql_post_import_checks.txt`

## 3. Preflight 结果

初次 preflight 已成功连接 MySQL。结果显示：

- `hainan_blue_carbon_core`：存在。
- `hainan_blue_carbon_satellite`：存在。
- `hainan_blue_carbon_intl`：存在。
- 旧 `hnblue`：存在；本轮仅做只读 guard，不作为写入目标。
- 7 张目标表初始均缺失：`t_data_source`、`t_region`、`t_satellite_mangrove_cover`、`t_satellite_region_metric`、`t_literature_reference`、`t_literature_carbon_record`、`t_allometry_equation`。

V2 schema 中已有非本轮目标表：

- `hainan_blue_carbon_core.t_data_asset_index`：0 行。
- `hainan_blue_carbon_satellite.t_sat_product_meta`：0 行。
- `hainan_blue_carbon_intl.env_remotesensing_gpp`、`gcc_coastal_transects`、`global_baad`、`global_tallo`、`model_allometry_cn`：已有大量数据。

因此未执行原始 `sql/schema/hnblue_v2_schema.sql` 中包含 `DROP TABLE IF EXISTS` 的版本。已生成并执行安全变体：仅保留 `CREATE DATABASE IF NOT EXISTS`、`USE`、`CREATE TABLE IF NOT EXISTS`，不含可执行 `DROP/TRUNCATE/DELETE/UPDATE/ALTER/INSERT`，不含旧 `hnblue` 目标。

schema 补表后再次 preflight：

- 3 个 V2 schema 均存在。
- 7 张目标表均存在。
- core 最小导入所需字段均存在。
- 导入前 `t_data_source=0`、`t_region=0`。
- satellite/intl 目标事实表导入前均为 0。

## 4. Core SQL 静态审计

`sql/seed/hnblue_v2_phase3_core_minimal_import_preview.sql` 审计结果：

- `INSERT` 总数：34。
- `t_data_source` INSERT：9。
- `t_region` INSERT：25。
- 不含可执行 `UPDATE/DELETE/DROP/ALTER/TRUNCATE/CREATE/REPLACE`。
- 不含 `hainan_blue_carbon_satellite` 写入。
- 不含 `hainan_blue_carbon_intl` 写入。
- 不含旧 `hnblue` 写入。

首次执行时 MySQL 拦截了 `t_region` 自引用子查询：`You can't specify target table 't_region' for update in FROM clause`。事务未提交，复核显示 `t_data_source=0`、`t_region=0`，没有部分写入。随后修正父区域解析为派生表子查询并重新生成 SQL。

## 5. 导入结果

导入方式：

- 使用 MySQL CLI。
- 使用 `START TRANSACTION; source ...; COMMIT;` 包裹 core 最小导入 SQL。
- SQL 文件内使用 `INSERT IGNORE`，具备重复执行幂等性。

| 指标 | 导入前 | 导入后 | 变化 |
|---|---:|---:|---:|
| `hainan_blue_carbon_core.t_data_source` | 0 | 9 | +9 |
| `hainan_blue_carbon_core.t_region` | 0 | 25 | +25 |
| `hainan_blue_carbon_satellite.t_satellite_mangrove_cover` | 0 | 0 | 0 |
| `hainan_blue_carbon_satellite.t_satellite_region_metric` | 0 | 0 | 0 |
| `hainan_blue_carbon_intl.t_literature_carbon_record` | 0 | 0 | 0 |
| `hainan_blue_carbon_intl.t_allometry_equation` | 0 | 0 | 0 |

批次命中结果：

- `matched_public_sources=9`
- `matched_public_regions=25`

新增行数判断：

- 本轮导入前 core 两张表均为 0 行。
- 本轮实际新增 `t_data_source` 9 行、`t_region` 25 行。
- 不存在“已存在行被 INSERT IGNORE 跳过”的情况。

## 6. 完整性复核

复核结果：

- 本批次 source_code 命中数：9。
- 本批次 region_code 命中数：25。
- `broken_parent_region_count=0`。
- `possible_mojibake_region_rows=0`。
- 本批次 source `is_simulated` 非 0 数量：0。
- `source_category` 为 NULL 或 UNKNOWN 数量：0。
- `version_label` 为空数量：0。
- satellite 与 intl 事实表未发生本轮导入。
- 旧 `hnblue` 库只读 guard：导入前 13 表、约 4804 行；导入后 13 表、约 4804 行。

## 7. 暂缓项

以下数据继续暂缓：

- `hainan_blue_carbon_satellite.t_satellite_mangrove_cover`
- `hainan_blue_carbon_satellite.t_satellite_region_metric`
- `hainan_blue_carbon_intl.t_literature_reference`
- `hainan_blue_carbon_intl.t_literature_carbon_record`
- `hainan_blue_carbon_intl.t_allometry_equation`

未导入 proxy 事实数据。未将文献区域均值写入样方、样木或土壤样品表。

## 8. 回滚或清理

若需要撤销本轮 core 字典导入，应先确认 satellite/intl/local 等事实表没有引用本批次 `source_id` 或 `region_id`。当前本轮未导入事实表，清理顺序如下：

1. 删除 `hainan_blue_carbon_core.t_region` 中本批次 `region_code`。
2. 删除 `hainan_blue_carbon_core.t_data_source` 中本批次 `source_code`。
3. 再次执行 `sql/seed/hnblue_v2_phase3_preflight_check.sql` 与导入后复核 SQL。

清理 SQL 已在 `sql/seed/hnblue_v2_phase3_core_minimal_import_preview.sql` 底部以注释形式保留，本轮未执行任何 DELETE。

## 9. 约束确认

- 成功连接 MySQL。
- schema 初始化已执行，采用安全 IF NOT EXISTS 变体，未执行原始 DROP 版本。
- 只写入 `hainan_blue_carbon_core.t_data_source` 与 `hainan_blue_carbon_core.t_region`。
- 未写旧 `hnblue` 库。
- 未导入 satellite 事实表。
- 未导入 intl 事实表。
- 未导入 proxy 事实数据。
- 未修改前端、后端、Flask、Redis、AnythingLLM。
- 未修改 import_ready CSV。
- 未在报告、SQL、脚本或 Git diff 中写入明文密码。
