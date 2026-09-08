# HNBLUE V2.0 Phase 5 剩余公开数据表导入报告

生成日期：2026-05-28
工作目录：`D:/ProgramData/Postgraduate/HNBLUE`

## 1. 执行结论

本轮未导入剩余三张事实表。停止原因不是 MySQL 连接失败，而是目标表结构无法完整承载 import-ready 数据语义，触发质量门：

- `hainan_blue_carbon_satellite.t_satellite_region_metric` 缺少 `is_proxy` 字段；CSV 中存在 30 条 proxy 记录，不能丢失 `is_proxy=1`。
- `hainan_blue_carbon_intl.t_literature_carbon_record` 缺少 `is_proxy`、`unit`、`indicator` 字段；不能把文献区域均值改写为样方、样木或土壤样品记录。
- `hainan_blue_carbon_intl.t_allometry_equation` 缺少 `source_id`、`region_id`、`is_proxy`、`indicator`、`value`、`unit` 字段；不能直接承载 import-ready 记录。
- `hainan_blue_carbon_core.t_indicator_dictionary` 尚无 remote 指标编码，`t_satellite_region_metric` 的 indicator FK 预检为 0。
- `hainan_blue_carbon_intl.t_literature_reference` 尚未建立本批次文献引用，intl 两张表 reference FK 预检为 0。

因此，本轮遵守质量门停止，不执行剩余事实表 INSERT。

## 2. 已保持的完成结果

| 表 | 导入前行数 | 导入后行数 | 新增行数 | 状态 |
|---|---:|---:|---:|---|
| `hainan_blue_carbon_core.t_data_source` | 9 | 9 | 0 | 已完成，保持 |
| `hainan_blue_carbon_core.t_region` | 25 | 25 | 0 | 已完成，保持 |
| `hainan_blue_carbon_satellite.t_satellite_mangrove_cover` | 66 | 66 | 0 | 已完成，保持 |
| `hainan_blue_carbon_satellite.t_satellite_region_metric` | 0 | 0 | 0 | 暂缓 |
| `hainan_blue_carbon_intl.t_literature_carbon_record` | 0 | 0 | 0 | 暂缓 |
| `hainan_blue_carbon_intl.t_allometry_equation` | 0 | 0 | 0 | 暂缓 |

旧 `hnblue` guard：

- 导入前：13 表，约 4804 行。
- 导入后：13 表，约 4804 行。
- 本轮未写旧 `hnblue`。

## 3. CSV 静态检查

| import-ready CSV | 记录数 | source 数 | region 数 | indicator 数 | proxy 数 | simulated 数 | 判定 |
|---|---:|---:|---:|---:|---:|---:|---|
| `satellite_region_metric_import_ready.csv` | 147 | 2 | 10 | 49 | 30 | 0 | 暂缓，目标表缺少 `is_proxy` |
| `intl_literature_carbon_record_import_ready.csv` | 33 | 3 | 7 | 9 | 0 | 0 | 暂缓，目标表缺少 `is_proxy/unit/indicator` 完整承载 |
| `intl_allometry_equation_import_ready.csv` | 2 | 1 | 1 | 1 | 0 | 0 | 暂缓，目标表缺少多项 import-ready 核心字段 |

三个 `frontend_*.json` 未进入数据库，仅作为展示派生文件。

## 4. 外键预检摘要

| 检查项 | 结果 |
|---|---:|
| remote_csv_rows | 147 |
| remote_source_fk_matches | 2 |
| remote_region_fk_matches | 10 |
| remote_indicator_dictionary_matches | 0 |
| literature_csv_rows | 33 |
| literature_source_fk_matches | 3 |
| literature_region_fk_matches | 7 |
| literature_reference_matches | 0 |
| allometry_csv_rows | 2 |
| allometry_source_fk_matches | 1 |
| allometry_reference_matches | 0 |

预检证据：`docs/v2/阶段5_外键预检结果.txt`

CSV 自然键重复静态检查：

- `satellite_region_metric_import_ready.csv`：0。
- `intl_literature_carbon_record_import_ready.csv`：0。
- `intl_allometry_equation_import_ready.csv`：0。

## 5. 导入执行状态

本轮实际执行：

- 只读导入前计数。
- 只读外键与字段缺口预检。
- 只读导入后复核。

本轮未执行：

- 未执行剩余事实表 INSERT。
- 未执行 `DROP`、`TRUNCATE`、`DELETE`、`UPDATE`。
- 未写旧 `hnblue`。
- 未修改前端、后端、Flask、Redis、AnythingLLM。
- 未修改 import-ready CSV。

执行证据：`docs/v2/阶段5_导入执行结果.txt`

## 6. 为什么不能强行导入

`satellite_region_metric_import_ready.csv` 中有 30 条 proxy 记录。目标表 `t_satellite_region_metric` 目前有 `region_id`、`indicator_code`、`value`、`unit`、`source_id`、`is_simulated`，但没有 `is_proxy`。如果强行导入，会丢失 proxy 标记，并可能把 MODIS、ERA5、CHIRPS、InVEST 等区域指标误读为真实通量塔或样地观测。

`intl_literature_carbon_record_import_ready.csv` 是文献区域均值或文献推导记录。目标表可以承载 `source_id`、`region_id`、`carbon_pool`、`value_mg_ha`，但不能完整保留 `indicator`、`unit`、`is_proxy`。强行导入会降低数据血缘与质量标记的可追踪性。

`intl_allometry_equation_import_ready.csv` 的 import-ready 记录带有 `source_id`、`region_id`、`indicator`、`value`、`unit`、`is_proxy`。目标表 `t_allometry_equation` 更接近参数化方程表，不包含这些通用事实记录字段。强行导入会丢失来源和区域语义。

## 7. 现在 V2.0 MySQL 已写入的公开数据

| 表 | 行数 | 可用于替换旧模拟展示 |
|---|---:|---|
| `hainan_blue_carbon_core.t_data_source` | 9 | 是，作为来源字典 |
| `hainan_blue_carbon_core.t_region` | 25 | 是，作为区域字典 |
| `hainan_blue_carbon_satellite.t_satellite_mangrove_cover` | 66 | 是，作为红树林面积时序 |
| `hainan_blue_carbon_satellite.t_satellite_region_metric` | 0 | 暂不能 |
| `hainan_blue_carbon_intl.t_literature_carbon_record` | 0 | 暂不能 |
| `hainan_blue_carbon_intl.t_allometry_equation` | 0 | 暂不能 |

可替换旧模拟展示的数据：

- 数据来源说明。
- 区域字典。
- 红树林面积时序。

仍是 proxy 或需暂缓的数据：

- MODIS/ERA5/CHIRPS/InVEST/土地利用/碳储 proxy 区域指标。
- 文献区域均值和文献推导碳储记录。
- 异速方程参数记录。

## 8. 下一步

下一步不建议直接生成写入脚本。建议先完成 V2 schema 扩展或新增旁路审计表：

1. 为 `t_satellite_region_metric` 增加 `is_proxy`、`source_record_id`、`quality_level`、`method_note` 字段，或建立 `t_public_import_audit` 旁路表。
2. 先导入 `hainan_blue_carbon_core.t_indicator_dictionary` 中缺失的 49 个 remote 指标。
3. 先导入 `hainan_blue_carbon_intl.t_literature_reference` 中缺失的文献引用。
4. 再做 satellite/intl 事实表导入前核验。

后端只读 API 可以先基于已完成的 core 字典和 `t_satellite_mangrove_cover` 生成，不应把暂缓的 proxy 和文献事实表纳入 API 结果。
