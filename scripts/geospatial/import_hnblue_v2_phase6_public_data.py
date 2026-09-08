from __future__ import annotations

import argparse
import csv
import os
from collections import Counter
from dataclasses import dataclass
from datetime import date
from decimal import Decimal
from pathlib import Path
from typing import Any

import pymysql


ROOT = Path(__file__).resolve().parents[2]
IMPORT_DIR = ROOT / "data" / "results" / "hnblue_v2_public" / "import_ready"
DOC_DIR = ROOT / "docs" / "v2"

SAT_CSV = IMPORT_DIR / "satellite_region_metric_import_ready.csv"
LIT_CSV = IMPORT_DIR / "intl_literature_carbon_record_import_ready.csv"
ALLO_CSV = IMPORT_DIR / "intl_allometry_equation_import_ready.csv"

TABLES = {
    "sources": ("hainan_blue_carbon_core", "t_data_source"),
    "regions": ("hainan_blue_carbon_core", "t_region"),
    "cover": ("hainan_blue_carbon_satellite", "t_satellite_mangrove_cover"),
    "sat_metric": ("hainan_blue_carbon_satellite", "t_satellite_region_metric"),
    "lit_record": ("hainan_blue_carbon_intl", "t_literature_carbon_record"),
    "allometry": ("hainan_blue_carbon_intl", "t_allometry_equation"),
}

REQUIRED_CORE_COUNTS = {
    "hainan_blue_carbon_core.t_data_source": 9,
    "hainan_blue_carbon_core.t_region": 25,
    "hainan_blue_carbon_satellite.t_satellite_mangrove_cover": 66,
}

ADD_COLUMNS = {
    "hainan_blue_carbon_satellite.t_satellite_region_metric": {
        "source_record_id": "varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公开数据原始记录编码'",
        "is_proxy": "tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否代理/推导公开指标'",
        "indicator_name": "varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公开数据指标名称'",
        "quality_level": "varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公开数据质量等级'",
        "method_note": "text COLLATE utf8mb4_unicode_ci COMMENT '公开数据方法说明'",
        "notes": "text COLLATE utf8mb4_unicode_ci COMMENT '公开数据备注'",
    },
    "hainan_blue_carbon_intl.t_literature_carbon_record": {
        "source_record_id": "varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公开数据原始记录编码'",
        "is_proxy": "tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否代理/推导公开指标'",
        "is_simulated": "tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否模拟数据'",
        "indicator_code": "varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公开数据指标编码'",
        "indicator_name": "varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公开数据指标名称'",
        "value": "decimal(18,6) DEFAULT NULL COMMENT '公开数据原始数值'",
        "unit": "varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公开数据原始单位'",
        "quality_level": "varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公开数据质量等级'",
        "notes": "text COLLATE utf8mb4_unicode_ci COMMENT '公开数据备注'",
    },
    "hainan_blue_carbon_intl.t_allometry_equation": {
        "source_id": "bigint DEFAULT NULL COMMENT '公开数据来源ID'",
        "region_id": "bigint DEFAULT NULL COMMENT '公开数据区域ID'",
        "source_record_id": "varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公开数据原始记录编码'",
        "is_proxy": "tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否代理/推导公开指标'",
        "is_simulated": "tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否模拟数据'",
        "indicator_code": "varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公开数据指标编码'",
        "indicator_name": "varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公开数据指标名称'",
        "value": "decimal(18,6) DEFAULT NULL COMMENT '公开数据原始数值'",
        "unit": "varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公开数据原始单位'",
        "quality_level": "varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公开数据质量等级'",
        "method_note": "text COLLATE utf8mb4_unicode_ci COMMENT '公开数据方法说明'",
        "notes": "text COLLATE utf8mb4_unicode_ci COMMENT '公开数据备注'",
    },
}

FORBIDDEN_SQL = ("DROP ", "TRUNCATE ", "DELETE ", "RENAME ", "UPDATE ")


@dataclass
class ImportResult:
    table: str
    csv_rows: int
    before_rows: int
    after_rows: int
    inserted_rows: int
    skipped_existing: int
    failed_rows: int
    csv_proxy_rows: int
    db_proxy_rows: int
    csv_simulated_rows: int
    db_nonzero_simulated_rows: int
    source_matches: int
    region_matches: int


def rows(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))


def clean(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text if text else None


def dec(value: Any) -> Decimal | None:
    text = clean(value)
    return Decimal(text) if text is not None else None


def integer(value: Any) -> int | None:
    text = clean(value)
    return int(text) if text is not None else None


def tinyint(value: Any) -> int:
    text = clean(value)
    return int(text) if text is not None else 0


def table_name(schema: str, table: str) -> str:
    return f"`{schema}`.`{table}`"


def execute(cur: Any, sql: str, args: tuple[Any, ...] | None = None) -> int:
    upper_sql = " ".join(sql.upper().split())
    if any(token in upper_sql for token in FORBIDDEN_SQL):
        raise RuntimeError(f"forbidden SQL blocked: {sql}")
    return cur.execute(sql, args)


def fetch_one(cur: Any, sql: str, args: tuple[Any, ...] | None = None) -> Any:
    cur.execute(sql, args)
    row = cur.fetchone()
    if row is None:
        return None
    if isinstance(row, dict):
        return next(iter(row.values()))
    return row[0]


def count_table(cur: Any, schema: str, table: str) -> int:
    return int(fetch_one(cur, f"SELECT COUNT(*) FROM {table_name(schema, table)}"))


def columns(cur: Any, schema: str, table: str) -> set[str]:
    cur.execute(
        """
        SELECT COLUMN_NAME
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s
        """,
        (schema, table),
    )
    return {row["COLUMN_NAME"] for row in cur.fetchall()}


def add_missing_columns(cur: Any) -> list[str]:
    executed: list[str] = []
    for full_name, cols in ADD_COLUMNS.items():
        schema, table = full_name.split(".", 1)
        existing = columns(cur, schema, table)
        for col, ddl in cols.items():
            if col in existing:
                continue
            sql = f"ALTER TABLE {table_name(schema, table)} ADD COLUMN `{col}` {ddl}"
            execute(cur, sql)
            executed.append(sql)
    return executed


def source_lookup(cur: Any) -> dict[str, int]:
    cur.execute("SELECT source_code, source_id FROM hainan_blue_carbon_core.t_data_source")
    return {row["source_code"]: int(row["source_id"]) for row in cur.fetchall()}


def region_lookup(cur: Any) -> dict[str, int]:
    cur.execute("SELECT region_code, region_id FROM hainan_blue_carbon_core.t_region")
    return {row["region_code"]: int(row["region_id"]) for row in cur.fetchall()}


def reference_lookup(cur: Any) -> dict[int, int]:
    cur.execute(
        """
        SELECT source_id, MIN(reference_id) AS reference_id
        FROM hainan_blue_carbon_intl.t_literature_reference
        WHERE source_id IS NOT NULL
        GROUP BY source_id
        """
    )
    return {int(row["source_id"]): int(row["reference_id"]) for row in cur.fetchall()}


def infer_domain(indicator_code: str) -> str:
    code = indicator_code.upper()
    if any(token in code for token in ("FLUX", "CO2", "CH4", "N2O", "GHG")):
        return "CARBON_FLUX"
    if any(token in code for token in ("CARBON", "SOC", "AGB", "BGB", "BIOMASS", "DENSITY", "EF_")):
        return "CARBON_STOCK"
    if any(token in code for token in ("REMOTE", "LAND", "NDVI", "AREA", "PROPORTION")):
        return "REMOTE_SENSING"
    if "ALLOMETRIC" in code:
        return "MODEL_OUTPUT"
    return "OTHER"


def indicator_payload(all_rows: list[dict[str, str]]) -> dict[str, dict[str, str | None]]:
    payload: dict[str, dict[str, str | None]] = {}
    for row in all_rows:
        code = clean(row.get("indicator_code")) or clean(row.get("indicator"))
        if not code:
            continue
        name_en = clean(row.get("indicator_name")) or code.lower()
        name_cn = clean(row.get("indicator_label")) or name_en.replace("_", " ")
        method = clean(row.get("method")) or clean(row.get("method_note")) or clean(row.get("notes"))
        payload.setdefault(
            code,
            {
                "indicator_name_cn": name_cn,
                "indicator_name_en": name_en,
                "indicator_domain": infer_domain(code),
                "default_unit": clean(row.get("unit")),
                "method_note": method,
            },
        )
    return payload


def ensure_indicators(cur: Any, payload: dict[str, dict[str, str | None]]) -> int:
    inserted = 0
    sql = """
        INSERT IGNORE INTO hainan_blue_carbon_core.t_indicator_dictionary
        (indicator_code, indicator_name_cn, indicator_name_en, indicator_domain, default_unit, value_type, method_note)
        VALUES (%s, %s, %s, %s, %s, 'NUMERIC', %s)
    """
    for code, row in sorted(payload.items()):
        inserted += execute(
            cur,
            sql,
            (
                code,
                row["indicator_name_cn"],
                row["indicator_name_en"],
                row["indicator_domain"],
                row["default_unit"],
                row["method_note"],
            ),
        )
    return inserted


def ensure_references(cur: Any, source_codes: set[str], sources: dict[str, int]) -> int:
    inserted = 0
    sql = """
        INSERT INTO hainan_blue_carbon_intl.t_literature_reference
        (source_id, title, publication_year, journal_or_source, url, study_area, ecosystem_type, citation_text)
        SELECT source_id, source_name, NULL, source_category, source_url, 'HNBLUE V2.0 public import-ready package', 'MANGROVE', citation_text
        FROM hainan_blue_carbon_core.t_data_source
        WHERE source_code=%s
          AND NOT EXISTS (
              SELECT 1
              FROM hainan_blue_carbon_intl.t_literature_reference
              WHERE source_id=%s
          )
    """
    for source_code in sorted(source_codes):
        source_id = sources.get(source_code)
        if source_id is None:
            continue
        inserted += execute(cur, sql, (source_code, source_id))
    return inserted


def existing_source_records(cur: Any, schema: str, table: str) -> set[str]:
    if "source_record_id" not in columns(cur, schema, table):
        return set()
    cur.execute(f"SELECT source_record_id FROM {table_name(schema, table)} WHERE source_record_id IS NOT NULL")
    return {row["source_record_id"] for row in cur.fetchall()}


def check_no_simulated(csv_name: str, data: list[dict[str, str]]) -> None:
    count = sum(1 for row in data if tinyint(row.get("is_simulated")) != 0)
    if count:
        raise RuntimeError(f"{csv_name} contains {count} simulated rows")


def method_text(*parts: str | None) -> str:
    values = [p for p in parts if clean(p)]
    return " | ".join(values)


def import_sat_metric(cur: Any, data: list[dict[str, str]], sources: dict[str, int], regions: dict[str, int]) -> ImportResult:
    schema, table = TABLES["sat_metric"]
    before = count_table(cur, schema, table)
    existing = existing_source_records(cur, schema, table)
    inserted = skipped = failed = 0
    source_match: set[str] = set()
    region_match: set[str] = set()
    sql = """
        INSERT INTO hainan_blue_carbon_satellite.t_satellite_region_metric
        (region_id, indicator_code, metric_year, metric_month, metric_date, value, unit, stat_method, source_id,
         is_simulated, source_record_id, is_proxy, indicator_name, quality_level, method_note, notes)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    for row in data:
        source_code = clean(row["source_id"])
        region_code = clean(row["region_id"])
        source_id = sources.get(source_code or "")
        region_id = regions.get(region_code or "")
        if source_id:
            source_match.add(source_code or "")
        if region_id:
            region_match.add(region_code or "")
        source_record_id = clean(row["source_record_id"])
        if not source_id or not region_id or not source_record_id:
            failed += 1
            continue
        if source_record_id in existing:
            skipped += 1
            continue
        execute(
            cur,
            sql,
            (
                region_id,
                clean(row.get("indicator_code")) or clean(row.get("indicator")),
                integer(row.get("metric_year")),
                integer(row.get("metric_month")),
                clean(row.get("metric_date")),
                dec(row.get("value")),
                clean(row.get("unit")),
                clean(row.get("stat_method")) or "MEAN",
                source_id,
                tinyint(row.get("is_simulated")),
                source_record_id,
                tinyint(row.get("is_proxy")),
                clean(row.get("indicator_name")),
                clean(row.get("quality_level")),
                method_text(clean(row.get("method")), clean(row.get("notes"))),
                clean(row.get("notes")),
            ),
        )
        existing.add(source_record_id)
        inserted += 1
    after = count_table(cur, schema, table)
    return result(cur, f"{schema}.{table}", data, before, after, inserted, skipped, failed, len(source_match), len(region_match))


def carbon_pool(value: str | None) -> str:
    text = (clean(value) or "OTHER").upper()
    return text if text in {"AGB", "BGB", "SOC", "DEADWOOD", "LITTER", "TOTAL", "OTHER"} else "OTHER"


def import_literature(
    cur: Any,
    data: list[dict[str, str]],
    sources: dict[str, int],
    regions: dict[str, int],
    refs: dict[int, int],
) -> ImportResult:
    schema, table = TABLES["lit_record"]
    before = count_table(cur, schema, table)
    existing = existing_source_records(cur, schema, table)
    inserted = skipped = failed = 0
    source_match: set[str] = set()
    region_match: set[str] = set()
    sql = """
        INSERT INTO hainan_blue_carbon_intl.t_literature_carbon_record
        (reference_id, region_id, site_name, ecosystem_type, carbon_pool, value_mg_ha, depth_top_cm, depth_bottom_cm,
         method_note, source_id, source_record_id, is_proxy, is_simulated, indicator_code, indicator_name, value, unit,
         quality_level, notes)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    for row in data:
        source_code = clean(row["source_id"])
        region_code = clean(row["region_id"])
        source_id = sources.get(source_code or "")
        region_id = regions.get(region_code or "")
        if source_id:
            source_match.add(source_code or "")
        if region_id:
            region_match.add(region_code or "")
        source_record_id = clean(row["source_record_id"])
        if not source_id or not region_id or not source_record_id:
            failed += 1
            continue
        if source_record_id in existing:
            skipped += 1
            continue
        note = method_text(
            clean(row.get("method_note")),
            "文献区域统计/文献推导，非样方、样木或土壤样品明细",
            clean(row.get("notes")),
        )
        execute(
            cur,
            sql,
            (
                refs.get(source_id),
                region_id,
                clean(row.get("site_name")),
                clean(row.get("ecosystem_type")) or "MANGROVE",
                carbon_pool(row.get("carbon_pool")),
                dec(row.get("value_mg_ha")),
                dec(row.get("depth_top_cm")),
                dec(row.get("depth_bottom_cm")),
                note,
                source_id,
                source_record_id,
                tinyint(row.get("is_proxy")),
                tinyint(row.get("is_simulated")),
                clean(row.get("indicator")) or clean(row.get("indicator_code")),
                clean(row.get("indicator")),
                dec(row.get("value")),
                clean(row.get("unit")),
                clean(row.get("quality_level")),
                clean(row.get("notes")),
            ),
        )
        existing.add(source_record_id)
        inserted += 1
    after = count_table(cur, schema, table)
    return result(cur, f"{schema}.{table}", data, before, after, inserted, skipped, failed, len(source_match), len(region_match))


def component(value: str | None) -> str:
    text = (clean(value) or "OTHER").upper()
    return text if text in {"TOTAL", "ABOVEGROUND", "BELOWGROUND", "STEM", "BRANCH", "LEAF", "ROOT", "OTHER"} else "OTHER"


def import_allometry(
    cur: Any,
    data: list[dict[str, str]],
    sources: dict[str, int],
    regions: dict[str, int],
    refs: dict[int, int],
) -> ImportResult:
    schema, table = TABLES["allometry"]
    before = count_table(cur, schema, table)
    existing = existing_source_records(cur, schema, table)
    inserted = skipped = failed = 0
    source_match: set[str] = set()
    region_match: set[str] = set()
    sql = """
        INSERT INTO hainan_blue_carbon_intl.t_allometry_equation
        (reference_id, country, province, scientific_name, equation_form, component, coefficient_a, coefficient_b,
         coefficient_c, coefficient_d, r_squared, sample_size, dbh_min_cm, dbh_max_cm, height_min_m, height_max_m,
         unit_note, source_id, region_id, source_record_id, is_proxy, is_simulated, indicator_code, indicator_name,
         value, unit, quality_level, method_note, notes)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    for row in data:
        source_code = clean(row["source_id"])
        region_code = clean(row["region_id"])
        source_id = sources.get(source_code or "")
        region_id = regions.get(region_code or "")
        if source_id:
            source_match.add(source_code or "")
        if region_id:
            region_match.add(region_code or "")
        source_record_id = clean(row["source_record_id"])
        if not source_id or not region_id or not source_record_id:
            failed += 1
            continue
        if source_record_id in existing:
            skipped += 1
            continue
        execute(
            cur,
            sql,
            (
                refs.get(source_id),
                clean(row.get("country")),
                clean(row.get("province")),
                clean(row.get("scientific_name")),
                clean(row.get("equation_form")),
                component(row.get("component")),
                dec(row.get("coefficient_a")),
                dec(row.get("coefficient_b")),
                dec(row.get("coefficient_c")),
                dec(row.get("coefficient_d")),
                dec(row.get("r_squared")),
                integer(row.get("sample_size")),
                dec(row.get("dbh_min_cm")),
                dec(row.get("dbh_max_cm")),
                dec(row.get("height_min_m")),
                dec(row.get("height_max_m")),
                clean(row.get("unit_note")),
                source_id,
                region_id,
                source_record_id,
                tinyint(row.get("is_proxy")),
                tinyint(row.get("is_simulated")),
                clean(row.get("indicator")) or clean(row.get("indicator_code")),
                clean(row.get("indicator")),
                dec(row.get("value")),
                clean(row.get("unit")),
                clean(row.get("quality_level")),
                clean(row.get("method")),
                clean(row.get("notes")),
            ),
        )
        existing.add(source_record_id)
        inserted += 1
    after = count_table(cur, schema, table)
    return result(cur, f"{schema}.{table}", data, before, after, inserted, skipped, failed, len(source_match), len(region_match))


def result(
    cur: Any,
    table: str,
    data: list[dict[str, str]],
    before: int,
    after: int,
    inserted: int,
    skipped: int,
    failed: int,
    source_matches: int,
    region_matches: int,
) -> ImportResult:
    schema, table_only = table.split(".", 1)
    proxy = int(fetch_one(cur, f"SELECT COUNT(*) FROM {table_name(schema, table_only)} WHERE is_proxy=1"))
    nonzero_sim = int(fetch_one(cur, f"SELECT COUNT(*) FROM {table_name(schema, table_only)} WHERE is_simulated<>0"))
    return ImportResult(
        table=table,
        csv_rows=len(data),
        before_rows=before,
        after_rows=after,
        inserted_rows=inserted,
        skipped_existing=skipped,
        failed_rows=failed,
        csv_proxy_rows=sum(1 for row in data if tinyint(row.get("is_proxy")) == 1),
        db_proxy_rows=proxy,
        csv_simulated_rows=sum(1 for row in data if tinyint(row.get("is_simulated")) != 0),
        db_nonzero_simulated_rows=nonzero_sim,
        source_matches=source_matches,
        region_matches=region_matches,
    )


def guard(cur: Any) -> dict[str, int]:
    cur.execute(
        """
        SELECT COUNT(*) AS table_count, COALESCE(SUM(TABLE_ROWS), 0) AS approx_rows
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA='hnblue'
        """
    )
    row = cur.fetchone()
    return {"table_count": int(row["table_count"]), "approx_rows": int(row["approx_rows"])}


def static_counts(data: list[dict[str, str]]) -> dict[str, int]:
    return {
        "rows": len(data),
        "sources": len({clean(row.get("source_id")) for row in data if clean(row.get("source_id"))}),
        "regions": len({clean(row.get("region_id")) for row in data if clean(row.get("region_id"))}),
        "indicators": len({clean(row.get("indicator_code")) or clean(row.get("indicator")) for row in data if clean(row.get("indicator_code")) or clean(row.get("indicator"))}),
        "proxy": sum(1 for row in data if tinyint(row.get("is_proxy")) == 1),
        "simulated": sum(1 for row in data if tinyint(row.get("is_simulated")) != 0),
    }


def write_reports(
    alterations: list[str],
    indicator_inserted: int,
    reference_inserted: int,
    before_counts: dict[str, int],
    after_counts: dict[str, int],
    old_before: dict[str, int],
    old_after: dict[str, int],
    results: list[ImportResult],
    datasets: dict[str, list[dict[str, str]]],
) -> None:
    DOC_DIR.mkdir(parents=True, exist_ok=True)
    today = date.today().isoformat()
    structure_path = DOC_DIR / "阶段6_表结构扩展记录.txt"
    counts_path = DOC_DIR / "阶段6_导入前后计数.txt"
    proxy_path = DOC_DIR / "阶段6_proxy记录复核.txt"
    report_path = DOC_DIR / "阶段6_剩余公开数据宽松导入报告.md"

    structure_lines = [
        "HNBLUE V2.0 Phase 6 表结构扩展记录",
        f"生成日期: {today}",
        "执行策略: 仅 ALTER TABLE ADD COLUMN；未执行 DROP/TRUNCATE/DELETE/RENAME/UPDATE。",
        "",
    ]
    if alterations:
        structure_lines.extend(alterations)
    else:
        structure_lines.append("本轮未新增字段，目标表已具备承载字段。")
    structure_path.write_text("\n".join(structure_lines) + "\n", encoding="utf-8")

    count_lines = [
        "HNBLUE V2.0 Phase 6 导入前后计数",
        f"生成日期: {today}",
        "",
        "核心完成表:",
    ]
    for key, expected in REQUIRED_CORE_COUNTS.items():
        count_lines.append(f"- {key}: before={before_counts[key]}, after={after_counts[key]}, expected={expected}")
    count_lines.extend(["", "事实表导入:"])
    for item in results:
        count_lines.append(
            f"- {item.table}: before={item.before_rows}, after={item.after_rows}, inserted={item.inserted_rows}, "
            f"skipped_existing={item.skipped_existing}, failed={item.failed_rows}, csv_rows={item.csv_rows}, "
            f"csv_proxy={item.csv_proxy_rows}, db_proxy={item.db_proxy_rows}, "
            f"csv_simulated={item.csv_simulated_rows}, db_nonzero_simulated={item.db_nonzero_simulated_rows}, "
            f"source_matches={item.source_matches}, region_matches={item.region_matches}"
        )
    count_lines.extend(
        [
            "",
            f"旧 hnblue guard before: tables={old_before['table_count']}, approx_rows={old_before['approx_rows']}",
            f"旧 hnblue guard after: tables={old_after['table_count']}, approx_rows={old_after['approx_rows']}",
        ]
    )
    counts_path.write_text("\n".join(count_lines) + "\n", encoding="utf-8")

    proxy_lines = [
        "HNBLUE V2.0 Phase 6 proxy记录复核",
        f"生成日期: {today}",
        "原则: proxy 数据保留 is_proxy=1；不改写为通量塔、样方、样木或土壤样品实测。",
        "",
    ]
    for name, data in datasets.items():
        counter = Counter(row.get("indicator_code") or row.get("indicator") for row in data if tinyint(row.get("is_proxy")) == 1)
        proxy_lines.append(f"{name}: proxy_rows={sum(counter.values())}")
        for indicator, count in sorted(counter.items()):
            proxy_lines.append(f"- {indicator}: {count}")
        proxy_lines.append("")
    proxy_path.write_text("\n".join(proxy_lines), encoding="utf-8")

    summary_rows = "\n".join(
        f"| `{item.table}` | {item.before_rows} | {item.after_rows} | {item.inserted_rows} | {item.failed_rows} | {item.csv_proxy_rows} | {item.db_proxy_rows} | {item.db_nonzero_simulated_rows} |"
        for item in results
    )
    report = f"""# HNBLUE V2.0 阶段6 剩余公开数据宽松导入报告

生成日期: {today}

## 结论

已按“宽松但可追溯”策略导入三类 import-ready 公开数据。proxy 记录保留 `is_proxy=1`，所有导入记录保持 `is_simulated=0`。本轮只写入 V2.0 目标库，旧 `hnblue` 仅执行只读 guard。

## 保持完成表

| 表 | 导入前 | 导入后 | 目标 |
|---|---:|---:|---:|
| `hainan_blue_carbon_core.t_data_source` | {before_counts['hainan_blue_carbon_core.t_data_source']} | {after_counts['hainan_blue_carbon_core.t_data_source']} | 9 |
| `hainan_blue_carbon_core.t_region` | {before_counts['hainan_blue_carbon_core.t_region']} | {after_counts['hainan_blue_carbon_core.t_region']} | 25 |
| `hainan_blue_carbon_satellite.t_satellite_mangrove_cover` | {before_counts['hainan_blue_carbon_satellite.t_satellite_mangrove_cover']} | {after_counts['hainan_blue_carbon_satellite.t_satellite_mangrove_cover']} | 66 |

## 剩余公开数据导入结果

| 表 | 导入前 | 导入后 | 新增 | 失败 | CSV proxy | DB proxy | DB simulated!=0 |
|---|---:|---:|---:|---:|---:|---:|---:|
{summary_rows}

## 字典与引用补全

- 指标字典新增: {indicator_inserted}
- 文献引用新增: {reference_inserted}
- 表结构扩展: {len(alterations)} 条 `ADD COLUMN`

## 旧库 guard

- 导入前: `hnblue` tables={old_before['table_count']}, approx_rows={old_before['approx_rows']}
- 导入后: `hnblue` tables={old_after['table_count']}, approx_rows={old_after['approx_rows']}

## 约束执行

- 未执行 `DROP`、`TRUNCATE`、`DELETE`、`RENAME`、`UPDATE`。
- 未修改前端、后端、Flask、Redis、AnythingLLM。
- 文献区域均值保留“文献区域统计/文献推导”说明。
- allometry 记录保留 source、region、indicator、value、unit、quality 与方法备注。

配套证据:
- `docs/v2/阶段6_表结构扩展记录.txt`
- `docs/v2/阶段6_导入前后计数.txt`
- `docs/v2/阶段6_proxy记录复核.txt`
"""
    report_path.write_text(report, encoding="utf-8")


def connect(args: argparse.Namespace) -> pymysql.Connection:
    return pymysql.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        charset="utf8mb4",
        autocommit=False,
        cursorclass=pymysql.cursors.DictCursor,
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Import HNBLUE V2.0 phase6 public import-ready CSVs.")
    parser.add_argument("--host", default=os.getenv("HNBLUE_MYSQL_HOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.getenv("HNBLUE_MYSQL_PORT", "3306")))
    parser.add_argument("--user", default=os.getenv("HNBLUE_MYSQL_USER", "root"))
    parser.add_argument("--password", default=os.getenv("HNBLUE_MYSQL_PASSWORD", ""))
    args = parser.parse_args()

    sat = rows(SAT_CSV)
    lit = rows(LIT_CSV)
    allo = rows(ALLO_CSV)
    for name, data in ((SAT_CSV.name, sat), (LIT_CSV.name, lit), (ALLO_CSV.name, allo)):
        check_no_simulated(name, data)

    with connect(args) as conn:
        try:
            with conn.cursor() as cur:
                old_before = guard(cur)
                before_counts = {
                    "hainan_blue_carbon_core.t_data_source": count_table(cur, *TABLES["sources"]),
                    "hainan_blue_carbon_core.t_region": count_table(cur, *TABLES["regions"]),
                    "hainan_blue_carbon_satellite.t_satellite_mangrove_cover": count_table(cur, *TABLES["cover"]),
                }
                for full_name, expected in REQUIRED_CORE_COUNTS.items():
                    if before_counts[full_name] != expected:
                        raise RuntimeError(f"{full_name} expected {expected}, actual {before_counts[full_name]}")

                alterations = add_missing_columns(cur)
                sources = source_lookup(cur)
                regions = region_lookup(cur)

                indicator_inserted = ensure_indicators(cur, indicator_payload(sat + lit + allo))
                ref_sources = {clean(row.get("reference_source_id")) or clean(row["source_id"]) for row in lit + allo}
                reference_inserted = ensure_references(cur, {v for v in ref_sources if v}, sources)
                refs = reference_lookup(cur)

                results = [
                    import_sat_metric(cur, sat, sources, regions),
                    import_literature(cur, lit, sources, regions, refs),
                    import_allometry(cur, allo, sources, regions, refs),
                ]
                after_counts = {
                    "hainan_blue_carbon_core.t_data_source": count_table(cur, *TABLES["sources"]),
                    "hainan_blue_carbon_core.t_region": count_table(cur, *TABLES["regions"]),
                    "hainan_blue_carbon_satellite.t_satellite_mangrove_cover": count_table(cur, *TABLES["cover"]),
                }
                old_after = guard(cur)

                for item in results:
                    if item.csv_simulated_rows != 0 or item.db_nonzero_simulated_rows != 0:
                        raise RuntimeError(f"{item.table} simulated validation failed")
                    if item.failed_rows != 0:
                        raise RuntimeError(f"{item.table} failed_rows={item.failed_rows}")
                    if item.db_proxy_rows < item.csv_proxy_rows:
                        raise RuntimeError(f"{item.table} proxy rows lost")
                if old_before != old_after:
                    raise RuntimeError(f"hnblue guard changed: before={old_before}, after={old_after}")

                write_reports(
                    alterations,
                    indicator_inserted,
                    reference_inserted,
                    before_counts,
                    after_counts,
                    old_before,
                    old_after,
                    results,
                    {
                        SAT_CSV.name: sat,
                        LIT_CSV.name: lit,
                        ALLO_CSV.name: allo,
                    },
                )
            conn.commit()
        except Exception:
            conn.rollback()
            raise

    for name, data in ((SAT_CSV.name, sat), (LIT_CSV.name, lit), (ALLO_CSV.name, allo)):
        counts = static_counts(data)
        print(f"{name}: {counts}")
    print("phase6 import complete")


if __name__ == "__main__":
    main()
