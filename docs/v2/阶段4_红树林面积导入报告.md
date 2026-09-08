# HNBLUE V2.0 Phase 4 红树林面积时序最小事实表导入报告

生成日期：2026-05-28
工作目录：`D:/ProgramData/Postgraduate/HNBLUE`

## 1. 结论

本轮已完成 Phase 4 最小事实表导入，仅导入红树林面积时序到 `hainan_blue_carbon_satellite.t_satellite_mangrove_cover`。

- 已确认 core 字典：`t_data_source=9`，`t_region=25`。
- 已读取 `data/results/hnblue_v2_public/import_ready/satellite_mangrove_cover_import_ready.csv`。
- CSV 输入记录：66 行。
- CSV proxy 记录：0。
- CSV simulated 记录：0。
- 实际导入目标：仅 `hainan_blue_carbon_satellite.t_satellite_mangrove_cover`。
- 未导入：`t_satellite_region_metric`、`t_literature_carbon_record`、`t_allometry_equation`。
- 未写入旧 `hnblue` 库。

## 2. 执行 SQL 与证据文件

| 文件 | 作用 | 状态 |
|---|---|---|
| `.codex-run/hnblue_v2_phase4_mangrove_cover_fk_precheck.sql` | 临时 FK 预检 SQL，只读 | 已执行 |
| `.codex-run/hnblue_v2_phase4_mangrove_cover_import.sql` | 临时幂等导入 SQL，只写 `t_satellite_mangrove_cover` | 已执行 |
| `.codex-run/hnblue_v2_phase4_mangrove_cover_post_checks.sql` | 临时导入后复核 SQL，只读 | 已执行 |
| `docs/v2/HNBLUE_V2_phase4_before_import_counts.txt` | 导入前行数与旧库 guard | 已生成 |
| `docs/v2/HNBLUE_V2_phase4_mangrove_cover_fk_precheck.txt` | FK 预检结果 | 已生成 |
| `docs/v2/HNBLUE_V2_phase4_mangrove_cover_import_execution.txt` | 导入执行输出 | 已生成 |
| `docs/v2/HNBLUE_V2_phase4_mangrove_cover_post_import_checks.txt` | 导入后复核结果 | 已生成 |

临时 SQL 放在 `.codex-run/`，该目录已被 `.gitignore` 忽略，不包含密码。

## 3. 导入前检查

导入前数据库状态：

| 指标 | 行数 |
|---|---:|
| `hainan_blue_carbon_core.t_data_source` | 9 |
| `hainan_blue_carbon_core.t_region` | 25 |
| `hainan_blue_carbon_satellite.t_satellite_mangrove_cover` | 0 |
| `hainan_blue_carbon_satellite.t_satellite_region_metric` | 0 |
| `hainan_blue_carbon_intl.t_literature_carbon_record` | 0 |
| `hainan_blue_carbon_intl.t_allometry_equation` | 0 |

旧 `hnblue` guard：

- 导入前：13 表，约 4804 行。
- 仅做只读检查，未作为写入目标。

CSV 静态检查：

| 检查项 | 结果 |
|---|---:|
| CSV 记录数 | 66 |
| source 数 | 3 |
| region 数 | 11 |
| 年份范围 | 1990-2022 |
| proxy 记录数 | 0 |
| simulated 记录数 | 0 |
| 面积为空或小于 0 | 0 |
| CSV 自然键重复 | 0 |

FK 预检：

| 指标 | 结果 |
|---|---:|
| expected_csv_rows | 66 |
| matched_source_fk_rows | 66 |
| matched_region_fk_rows | 66 |
| matched_both_fk_rows | 66 |
| existing_duplicate_natural_key_rows | 0 |

## 4. 导入方式

导入 SQL 为幂等写入：

- 仅使用 `INSERT INTO ... SELECT ... WHERE NOT EXISTS`。
- 不含 `DROP`、`TRUNCATE`、`DELETE`、`UPDATE`、`ALTER`、`CREATE`、`REPLACE`。
- 不含旧 `hnblue` 写入。
- 不含 `t_satellite_region_metric`、`t_literature_carbon_record`、`t_allometry_equation` 写入。
- 使用 `START TRANSACTION; source ...; COMMIT;` 执行。

幂等键：

- `region_id`
- `metric_year`
- `source_id`

该键用于避免同一来源、同一区域、同一年份重复导入。

## 5. 导入后结果

| 指标 | 导入前 | 导入后 | 变化 |
|---|---:|---:|---:|
| `hainan_blue_carbon_satellite.t_satellite_mangrove_cover` | 0 | 66 | +66 |
| `hainan_blue_carbon_satellite.t_satellite_region_metric` | 0 | 0 | 0 |
| `hainan_blue_carbon_intl.t_literature_carbon_record` | 0 | 0 | 0 |
| `hainan_blue_carbon_intl.t_allometry_equation` | 0 | 0 | 0 |

本批次导入命中：

- `matched_imported_cover_rows=66`
- `duplicate_natural_key_count=0`
- 失败记录数：0

## 6. 按区域统计

| region_code | region_name | cover_records |
|---|---|---:|
| `REG_HI` | 海南省 | 2 |
| `REG_HI_DONGFANG` | 海南岛-东方 | 7 |
| `REG_HI_DONGZHAIGANG` | 海南岛-东寨港 | 7 |
| `REG_HI_GUANNAN` | 海南岛-冠南 | 7 |
| `REG_HI_HUACHANGWAN` | 海南岛-花场湾 | 7 |
| `REG_HI_HUIWEN` | 海南岛-会文 | 7 |
| `REG_HI_MANIAOGANG` | 海南岛-马袅港 | 7 |
| `REG_HI_PUQIAN` | 海南岛-铺前 | 7 |
| `REG_HI_XINYINGGANG` | 海南岛-新英港 | 7 |
| `REG_HI_YANGPUGANG` | 海南岛-洋浦港 | 7 |
| `REG_WENCHANG_BAMEN_BAY` | 文昌八门湾自然保护区 | 1 |

## 7. 按年份统计

| metric_year | cover_records |
|---:|---:|
| 1990 | 9 |
| 1995 | 9 |
| 2000 | 9 |
| 2005 | 9 |
| 2010 | 10 |
| 2015 | 9 |
| 2020 | 10 |
| 2022 | 1 |

## 8. 完整性复核

| 检查项 | 结果 |
|---|---:|
| broken_region_fk_count | 0 |
| broken_source_fk_count | 0 |
| null_or_negative_area_count | 0 |
| duplicate_natural_key_count | 0 |
| satellite metric 行数 | 0 |
| intl literature carbon 行数 | 0 |
| intl allometry 行数 | 0 |

旧 `hnblue` guard：

- 导入前：13 表，约 4804 行。
- 导入后：13 表，约 4804 行。
- 本轮未写旧库。

## 9. 约束确认

- 未导入 proxy。
- 未导入文献区域均值。
- 未导入 `t_satellite_region_metric`。
- 未导入 `t_literature_carbon_record`。
- 未导入 `t_allometry_equation`。
- 未写旧 `hnblue`。
- 未修改前端、后端、Flask、Redis、AnythingLLM。
- 未执行 `DROP`、`TRUNCATE`、`DELETE`、`UPDATE`。
- 未修改 import_ready CSV。

## 10. 下一步

下一步可以进入 `t_satellite_region_metric` 的事实表导入前核验。该步骤必须先处理 proxy 字段承载问题，因为当前目标表没有独立 `is_proxy` 字段，不能把 proxy 区域指标误标为真实观测。
