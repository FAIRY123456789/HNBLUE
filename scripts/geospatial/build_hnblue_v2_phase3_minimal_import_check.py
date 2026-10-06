from __future__ import annotations

import csv
import math
import re
from pathlib import Path

import pandas as pd


ROOT = Path(__file__).resolve().parents[2]
IMPORT_DIR = ROOT / "data" / "results" / "hnblue_v2_public" / "import_ready"
SCHEMA_PATH = ROOT / "sql" / "schema" / "hnblue_v2_schema.sql"
PREFLIGHT_SQL_PATH = ROOT / "sql" / "seed" / "hnblue_v2_phase3_preflight_check.sql"
CORE_IMPORT_SQL_PATH = ROOT / "sql" / "seed" / "hnblue_v2_phase3_core_minimal_import_preview.sql"
REPORT_PATH = ROOT / "docs" / "v2" / "HNBLUE_V2_phase3_minimal_import_check_report.md"

TARGETS = {
    "hainan_blue_carbon_core": ["t_data_source", "t_region"],
    "hainan_blue_carbon_satellite": ["t_satellite_mangrove_cover", "t_satellite_region_metric"],
    "hainan_blue_carbon_intl": [
        "t_literature_reference",
        "t_literature_carbon_record",
        "t_allometry_equation",
    ],
}

CORE_SOURCE_CSV = IMPORT_DIR / "core_data_sources_import_ready.csv"
CORE_REGION_CSV = IMPORT_DIR / "core_regions_import_ready.csv"


def clean(value: object) -> str:
    if value is None:
        return ""
    if isinstance(value, float) and math.isnan(value):
        return ""
    text = str(value).strip()
    if text.lower() in {"nan", "none"}:
        return ""
    return text


def sql_quote(value: object) -> str:
    text = clean(value)
    if not text:
        return "NULL"
    return "'" + text.replace("\\", "\\\\").replace("'", "''") + "'"


def sql_int(value: object) -> str:
    text = clean(value)
    if not text:
        return "NULL"
    return str(int(float(text)))


def source_category(source_type: str) -> str:
    text = source_type.lower()
    if "government" in text:
        return "GOVERNMENT_DATA"
    if "model" in text:
        return "MODEL_OUTPUT"
    if "public_numeric" in text:
        return "PUBLIC_DATASET"
    if "remote_sensing" in text:
        return "DERIVED_PRODUCT"
    if "peer_reviewed" in text or "literature" in text or "allometry" in text:
        return "LITERATURE"
    return "UNKNOWN"


def parse_schema_columns() -> dict[tuple[str, str], set[str]]:
    schema = SCHEMA_PATH.read_text(encoding="utf-8")
    current_schema = ""
    columns: dict[tuple[str, str], set[str]] = {}
    for line in schema.splitlines():
        use_match = re.search(r"USE `([^`]+)`", line)
        if use_match:
            current_schema = use_match.group(1)
            continue
        table_match = re.search(r"CREATE TABLE `([^`]+)`", line)
        if table_match:
            table = table_match.group(1)
            columns[(current_schema, table)] = set()
            continue
        if not columns:
            continue
        col_match = re.match(r"\s+`([^`]+)`\s+", line)
        if col_match:
            key = next(reversed(columns))
            columns[key].add(col_match.group(1))
    return columns


def build_preflight_sql() -> str:
    schema_values = ", ".join(f"'{schema}'" for schema in TARGETS)
    table_values = []
    for schema, tables in TARGETS.items():
        for table in tables:
            table_values.append(f"SELECT '{schema}' AS target_schema, '{table}' AS target_table")
    table_union = "\nUNION ALL\n".join(table_values)

    required_columns = [
        ("hainan_blue_carbon_core", "t_data_source", "source_code"),
        ("hainan_blue_carbon_core", "t_data_source", "source_category"),
        ("hainan_blue_carbon_core", "t_data_source", "source_name"),
        ("hainan_blue_carbon_core", "t_data_source", "source_url"),
        ("hainan_blue_carbon_core", "t_data_source", "citation_text"),
        ("hainan_blue_carbon_core", "t_data_source", "version_label"),
        ("hainan_blue_carbon_core", "t_data_source", "is_simulated"),
        ("hainan_blue_carbon_core", "t_data_source", "remark"),
        ("hainan_blue_carbon_core", "t_region", "region_code"),
        ("hainan_blue_carbon_core", "t_region", "parent_region_id"),
        ("hainan_blue_carbon_core", "t_region", "region_name"),
        ("hainan_blue_carbon_core", "t_region", "region_level"),
        ("hainan_blue_carbon_core", "t_region", "province"),
        ("hainan_blue_carbon_core", "t_region", "city"),
        ("hainan_blue_carbon_core", "t_region", "county"),
        ("hainan_blue_carbon_core", "t_region", "ecosystem_type"),
        ("hainan_blue_carbon_core", "t_region", "data_scope"),
        ("hainan_blue_carbon_core", "t_region", "remark"),
        ("hainan_blue_carbon_satellite", "t_satellite_mangrove_cover", "region_id"),
        ("hainan_blue_carbon_satellite", "t_satellite_mangrove_cover", "metric_year"),
        ("hainan_blue_carbon_satellite", "t_satellite_mangrove_cover", "mangrove_area_ha"),
        ("hainan_blue_carbon_satellite", "t_satellite_region_metric", "indicator_code"),
        ("hainan_blue_carbon_satellite", "t_satellite_region_metric", "value"),
        ("hainan_blue_carbon_intl", "t_literature_reference", "source_id"),
        ("hainan_blue_carbon_intl", "t_literature_carbon_record", "value_mg_ha"),
        ("hainan_blue_carbon_intl", "t_allometry_equation", "equation_form"),
    ]
    column_union = "\nUNION ALL\n".join(
        f"SELECT '{schema}' AS target_schema, '{table}' AS target_table, '{column}' AS target_column"
        for schema, table, column in required_columns
    )

    return f"""-- HNBLUE V2.0 Phase 3 preflight check SQL
-- Read-only verification. This file contains SELECT statements only.
-- Do not run against the legacy `hnblue` schema as an import target.

SET NAMES utf8mb4;

-- 1. Required schemas.
SELECT
  s.SCHEMA_NAME AS schema_name,
  CASE WHEN s.SCHEMA_NAME IS NULL THEN 'MISSING' ELSE 'EXISTS' END AS status
FROM (
  SELECT 'hainan_blue_carbon_core' AS schema_name
  UNION ALL SELECT 'hainan_blue_carbon_satellite'
  UNION ALL SELECT 'hainan_blue_carbon_intl'
) expected
LEFT JOIN information_schema.SCHEMATA s
  ON s.SCHEMA_NAME = expected.schema_name;

-- 2. Guard: legacy hnblue may exist but is not a target in this phase.
SELECT
  'legacy_hnblue_guard' AS check_name,
  CASE WHEN COUNT(*) = 0 THEN 'legacy schema absent'
       ELSE 'legacy schema exists; do not write it in Phase 3'
  END AS status
FROM information_schema.SCHEMATA
WHERE SCHEMA_NAME = 'hnblue';

-- 3. Required target tables.
WITH expected_tables AS (
{table_union}
)
SELECT
  e.target_schema,
  e.target_table,
  CASE WHEN t.TABLE_NAME IS NULL THEN 'MISSING' ELSE 'EXISTS' END AS status
FROM expected_tables e
LEFT JOIN information_schema.TABLES t
  ON t.TABLE_SCHEMA = e.target_schema
 AND t.TABLE_NAME = e.target_table
ORDER BY e.target_schema, e.target_table;

-- 4. Required columns used by Phase 2/3 import SQL.
WITH expected_columns AS (
{column_union}
)
SELECT
  e.target_schema,
  e.target_table,
  e.target_column,
  CASE WHEN c.COLUMN_NAME IS NULL THEN 'MISSING' ELSE 'EXISTS' END AS status,
  c.DATA_TYPE,
  c.COLUMN_TYPE,
  c.IS_NULLABLE
FROM expected_columns e
LEFT JOIN information_schema.COLUMNS c
  ON c.TABLE_SCHEMA = e.target_schema
 AND c.TABLE_NAME = e.target_table
 AND c.COLUMN_NAME = e.target_column
ORDER BY e.target_schema, e.target_table, e.target_column;

-- 5. Current row counts before any minimal import.
SELECT 'hainan_blue_carbon_core.t_data_source' AS table_name, COUNT(*) AS row_count
FROM hainan_blue_carbon_core.t_data_source
UNION ALL
SELECT 'hainan_blue_carbon_core.t_region', COUNT(*)
FROM hainan_blue_carbon_core.t_region
UNION ALL
SELECT 'hainan_blue_carbon_satellite.t_satellite_mangrove_cover', COUNT(*)
FROM hainan_blue_carbon_satellite.t_satellite_mangrove_cover
UNION ALL
SELECT 'hainan_blue_carbon_satellite.t_satellite_region_metric', COUNT(*)
FROM hainan_blue_carbon_satellite.t_satellite_region_metric
UNION ALL
SELECT 'hainan_blue_carbon_intl.t_literature_carbon_record', COUNT(*)
FROM hainan_blue_carbon_intl.t_literature_carbon_record
UNION ALL
SELECT 'hainan_blue_carbon_intl.t_allometry_equation', COUNT(*)
FROM hainan_blue_carbon_intl.t_allometry_equation;

-- 6. Existing core rows matching this public package.
SELECT source_code, source_id
FROM hainan_blue_carbon_core.t_data_source
WHERE source_code IN (
  SELECT source_id FROM hainan_blue_carbon_core.t_data_source WHERE 1 = 0
);
-- Fill the empty subquery above with source_code values from
-- data/results/hnblue_v2_public/import_ready/core_data_sources_import_ready.csv
-- when running manually in a SQL client.
"""


def build_core_import_sql() -> str:
    sources = pd.read_csv(CORE_SOURCE_CSV).fillna("")
    regions = pd.read_csv(CORE_REGION_CSV).fillna("")
    lines = [
        "-- HNBLUE V2.0 Phase 3 core minimal import preview",
        "-- Contains only hainan_blue_carbon_core.t_data_source and hainan_blue_carbon_core.t_region.",
        "-- 本轮脚本未连接 MySQL，未执行导入。执行前必须先运行 hnblue_v2_phase3_preflight_check.sql。",
        "-- satellite and intl fact tables are intentionally deferred.",
        "SET NAMES utf8mb4;",
        "",
        "-- Optional manual transaction wrapper:",
        "-- START TRANSACTION;",
        "",
        "-- 1. Minimal source dictionary import",
    ]
    for _, row in sources.iterrows():
        category = clean(row.get("source_category")) or source_category(clean(row.get("source_type")))
        lines.append(
            "INSERT IGNORE INTO hainan_blue_carbon_core.t_data_source "
            "(source_code, source_category, source_name, source_url, citation_text, version_label, is_simulated, remark) VALUES "
            f"({sql_quote(row['source_code'])}, {sql_quote(category)}, {sql_quote(row['source_name'])}, "
            f"{sql_quote(row['source_url'])}, {sql_quote(row['citation_text'])}, {sql_quote(row['version_label'])}, "
            f"{sql_int(row['is_simulated'])}, {sql_quote(row['remark'])});"
        )

    lines.extend(
        [
            "",
            "-- 2. Minimal region dictionary import",
            "-- parent_region_id is resolved by parent region_code. Rows with blank parent_region_code use NULL.",
        ]
    )
    for _, row in regions.iterrows():
        parent_code = clean(row.get("parent_region_code"))
        parent_expr = (
            "NULL"
            if not parent_code
            else (
                "(SELECT region_id FROM (SELECT region_id FROM hainan_blue_carbon_core.t_region "
                f"WHERE region_code={sql_quote(parent_code)} LIMIT 1) AS parent_region_lookup)"
            )
        )
        lines.append(
            "INSERT IGNORE INTO hainan_blue_carbon_core.t_region "
            "(region_code, parent_region_id, region_name, region_level, province, city, county, ecosystem_type, data_scope, remark) VALUES "
            f"({sql_quote(row['region_code'])}, {parent_expr}, {sql_quote(row['region_name'])}, "
            f"{sql_quote(row['region_level'])}, {sql_quote(row['province'])}, {sql_quote(row['city'])}, "
            f"{sql_quote(row['county'])}, {sql_quote(row['ecosystem_type'])}, {sql_quote(row['data_scope'])}, "
            f"{sql_quote(row['remark'])});"
        )

    source_codes = ", ".join(sql_quote(v) for v in sources["source_code"].tolist())
    region_codes = ", ".join(sql_quote(v) for v in regions["region_code"].tolist())
    lines.extend(
        [
            "",
            "-- 3. Post-import count checks",
            "SELECT COUNT(*) AS matched_public_sources",
            "FROM hainan_blue_carbon_core.t_data_source",
            f"WHERE source_code IN ({source_codes});",
            "",
            "SELECT COUNT(*) AS matched_public_regions",
            "FROM hainan_blue_carbon_core.t_region",
            f"WHERE region_code IN ({region_codes});",
            "",
            "-- 4. Manual rollback / cleanup SQL. Review FK dependencies before executing.",
            "-- DELETE FROM hainan_blue_carbon_core.t_region",
            f"-- WHERE region_code IN ({region_codes});",
            "-- DELETE FROM hainan_blue_carbon_core.t_data_source",
            f"-- WHERE source_code IN ({source_codes});",
            "",
            "-- Choose exactly one after manual execution inside a transaction:",
            "-- COMMIT;",
            "-- ROLLBACK;",
        ]
    )
    return "\n".join(lines) + "\n"


def static_field_audit() -> list[dict]:
    schema_columns = parse_schema_columns()
    checks = [
        (
            "core_data_sources_import_ready.csv",
            "hainan_blue_carbon_core",
            "t_data_source",
            ["source_code", "source_category", "source_name", "source_url", "citation_text", "version_label", "is_simulated", "remark"],
            "可执行",
        ),
        (
            "core_regions_import_ready.csv",
            "hainan_blue_carbon_core",
            "t_region",
            ["region_code", "parent_region_id", "region_name", "region_level", "province", "city", "county", "ecosystem_type", "data_scope", "remark"],
            "可执行",
        ),
        (
            "satellite_mangrove_cover_import_ready.csv",
            "hainan_blue_carbon_satellite",
            "t_satellite_mangrove_cover",
            ["region_id", "metric_year", "mangrove_area_ha", "classification_method", "source_id"],
            "暂缓",
        ),
        (
            "satellite_region_metric_import_ready.csv",
            "hainan_blue_carbon_satellite",
            "t_satellite_region_metric",
            ["region_id", "indicator_code", "metric_year", "value", "unit", "stat_method", "source_id", "is_simulated"],
            "暂缓",
        ),
        (
            "intl_literature_carbon_record_import_ready.csv",
            "hainan_blue_carbon_intl",
            "t_literature_carbon_record",
            ["reference_id", "region_id", "site_name", "ecosystem_type", "carbon_pool", "value_mg_ha", "depth_top_cm", "depth_bottom_cm", "method_note", "source_id"],
            "暂缓",
        ),
        (
            "intl_allometry_equation_import_ready.csv",
            "hainan_blue_carbon_intl",
            "t_allometry_equation",
            ["reference_id", "scientific_name", "equation_form", "component", "coefficient_a", "coefficient_b", "coefficient_c", "unit_note"],
            "暂缓",
        ),
    ]
    rows = []
    for csv_name, schema, table, target_columns, status in checks:
        actual = schema_columns.get((schema, table), set())
        missing = [column for column in target_columns if column not in actual]
        csv_path = IMPORT_DIR / csv_name
        csv_rows = len(pd.read_csv(csv_path)) if csv_path.exists() else 0
        rows.append(
            {
                "csv": csv_name,
                "target": f"{schema}.{table}",
                "rows": csv_rows,
                "columns": ", ".join(target_columns),
                "schema_static_status": "匹配" if not missing else f"缺字段: {', '.join(missing)}",
                "phase3_action": status,
            }
        )
    return rows


def write_report() -> None:
    sources = pd.read_csv(CORE_SOURCE_CSV).fillna("")
    regions = pd.read_csv(CORE_REGION_CSV).fillna("")
    audit_rows = static_field_audit()
    lines = [
        "# HNBLUE V2.0 Phase 3 测试库最小导入前核验报告",
        "",
        "本报告面向 Phase 2 SQL preview 的测试库导入前核验。当前分支未获得明确 MySQL 连接权限，因此本轮只生成核验 SQL、最小 core 导入 SQL 草案和操作说明。",
        "",
        "**本轮未连接 MySQL，未执行导入，写入数据库行数为 0。**",
        "",
        "## 1. 生成文件",
        "",
        "| 文件 | 作用 | 是否执行 |",
        "|---|---|---|",
        "| `sql/seed/hnblue_v2_phase3_preflight_check.sql` | 只读检查目标 schema、表、字段和导入前行数 | 未执行 |",
        "| `sql/seed/hnblue_v2_phase3_core_minimal_import_preview.sql` | 仅包含 `t_data_source` 与 `t_region` 的最小导入 SQL 草案 | 未执行 |",
        "| `docs/v2/HNBLUE_V2_phase3_minimal_import_check_report.md` | 本报告 | 已生成 |",
        "",
        "## 2. 当前 MySQL 目标表状态",
        "",
        "未连接 MySQL，无法直接确认当前实例中 `hainan_blue_carbon_core`、`hainan_blue_carbon_satellite`、`hainan_blue_carbon_intl` 是否已经创建。`sql/schema/hnblue_v2_schema.sql` 静态定义中包含以下目标表：",
        "",
        "- `hainan_blue_carbon_core.t_data_source`",
        "- `hainan_blue_carbon_core.t_region`",
        "- `hainan_blue_carbon_satellite.t_satellite_mangrove_cover`",
        "- `hainan_blue_carbon_satellite.t_satellite_region_metric`",
        "- `hainan_blue_carbon_intl.t_literature_reference`",
        "- `hainan_blue_carbon_intl.t_literature_carbon_record`",
        "- `hainan_blue_carbon_intl.t_allometry_equation`",
        "",
        "运行 `sql/seed/hnblue_v2_phase3_preflight_check.sql` 可在测试库中得到真实存在状态和导入前行数。",
        "",
        "## 3. 字段匹配静态审计",
        "",
        "| import_ready CSV | 目标表 | 记录数 | 目标字段 | schema 静态匹配 | Phase 3 动作 |",
        "|---|---|---:|---|---|---|",
    ]
    for row in audit_rows:
        lines.append(
            f"| `{row['csv']}` | `{row['target']}` | {row['rows']} | `{row['columns']}` | {row['schema_static_status']} | {row['phase3_action']} |"
        )

    lines.extend(
        [
            "",
            "## 4. 可执行与暂缓 SQL",
            "",
            "可执行前置条件：测试库中已经创建 V2 schema；预检 SQL 返回 core 目标表和所需字段全部 `EXISTS`；确认不会连接或写入旧 `hnblue` 库。",
            "",
            "本轮准备的最小可执行 SQL：",
            "",
            f"- `hainan_blue_carbon_core.t_data_source`：{len(sources)} 行，来源字典。",
            f"- `hainan_blue_carbon_core.t_region`：{len(regions)} 行，区域字典。",
            "",
            "本轮明确暂缓 SQL：",
            "",
            "- `hainan_blue_carbon_satellite.t_satellite_mangrove_cover`：暂缓事实表导入。",
            "- `hainan_blue_carbon_satellite.t_satellite_region_metric`：暂缓事实表导入；其中 proxy 记录不得作为真实观测。",
            "- `hainan_blue_carbon_intl.t_literature_reference`：暂缓，需先确认 source FK。",
            "- `hainan_blue_carbon_intl.t_literature_carbon_record`：暂缓；文献区域均值不得写入样方、样木或土壤样品表。",
            "- `hainan_blue_carbon_intl.t_allometry_equation`：暂缓，需确认 reference FK。",
            "",
            "## 5. 外键顺序",
            "",
            "建议顺序：",
            "",
            "1. `hainan_blue_carbon_core.t_data_source`",
            "2. `hainan_blue_carbon_core.t_region`，先插入父级为空的区域，再插入带 `parent_region_code` 的子区域",
            "3. 暂停，人工核验 `source_code` 与 `region_code` 到自增主键的映射",
            "4. 后续批次再处理 satellite 与 intl 事实表",
            "",
            "## 6. 回滚或清理",
            "",
            "`hnblue_v2_phase3_core_minimal_import_preview.sql` 底部提供按 `source_code` 与 `region_code` 清理的注释 SQL。若在事务中手工执行，可在确认前使用 `ROLLBACK`。若已经提交，先确认无事实表外键依赖，再按以下顺序清理：",
            "",
            "1. 删除 `hainan_blue_carbon_core.t_region` 中本批次 `region_code`。",
            "2. 删除 `hainan_blue_carbon_core.t_data_source` 中本批次 `source_code`。",
            "3. 再次运行 preflight row count 检查。",
            "",
            "## 7. 约束确认",
            "",
            "- 未写旧 `hnblue` 库。",
            "- 未导入 proxy 事实数据。",
            "- 未导入 satellite 或 intl 事实表。",
            "- 未导入文献区域均值到样方、样木或土壤样品表。",
            "- 未修改前端、后端、Flask、Redis 或其他外部知识服务。",
        ]
    )
    REPORT_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> None:
    PREFLIGHT_SQL_PATH.write_text(build_preflight_sql(), encoding="utf-8")
    CORE_IMPORT_SQL_PATH.write_text(build_core_import_sql(), encoding="utf-8")
    write_report()
    print("phase3_generated=1")
    print(f"preflight_sql={PREFLIGHT_SQL_PATH.as_posix()}")
    print(f"core_import_preview_sql={CORE_IMPORT_SQL_PATH.as_posix()}")
    print(f"report={REPORT_PATH.as_posix()}")
    print("mysql_connected=0")
    print("sql_executed=0")
    print("db_rows_written=0")


if __name__ == "__main__":
    main()
