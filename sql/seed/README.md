# HNBLUE V2.0 seed SQL 说明

本目录只生成入库准备 SQL，不执行数据库导入。

`hnblue_v2_schema.sql` 当前仓库未找到。以下 SQL 依据 `docs/context/04_database_and_data_context.md` 中的建议表名生成，正式执行前必须按实际 V2.0 schema 复核：

1. Schema 名称是否为 `hainan_blue_carbon_core`、`hainan_blue_carbon_intl`、`hainan_blue_carbon_flux`、`hainan_blue_carbon_satellite`。
2. 表名是否为 `t_data_source`、`t_region`、`t_data_version`、`t_data_asset_index`、`t_literature_carbon_record`。
3. 字段名、字段类型、主键、唯一键、外键和枚举值是否与 CSV 一致。
4. `source_category`、`is_simulated`、`quality_level`、`is_proxy` 约束是否存在。

推荐下一轮执行顺序：

1. 复核 schema。
2. 加载 `data/staging/core/seed_data_sources.csv`。
3. 加载 `data/staging/core/seed_regions.csv`。
4. 加载 `data/staging/core/seed_data_versions.csv` 和 `seed_asset_index.csv`。
5. 加载文献、遥感、通量代理和无人机候选表。
