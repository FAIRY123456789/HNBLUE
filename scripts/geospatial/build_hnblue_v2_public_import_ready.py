from __future__ import annotations

import csv
import math
import re
from datetime import date
from pathlib import Path

import pandas as pd


ROOT = Path(__file__).resolve().parents[2]
NORMALIZED_DIR = ROOT / "data" / "results" / "hnblue_v2_public" / "normalized"
IMPORT_DIR = ROOT / "data" / "results" / "hnblue_v2_public" / "import_ready"
SQL_PATH = ROOT / "sql" / "seed" / "hnblue_v2_public_import_preview.sql"
REPORT_PATH = ROOT / "docs" / "v2" / "HNBLUE_V2_phase2_import_mapping_report.md"
REJECT_PATH = ROOT / "docs" / "v2" / "HNBLUE_V2_phase2_rejected_records.md"
SCHEMA_PATH = ROOT / "sql" / "schema" / "hnblue_v2_schema.sql"

DISPLAY_FILES = {
    "sources": "display_data_sources.csv",
    "regions": "display_region_dictionary.csv",
    "area": "display_mangrove_area_timeseries.csv",
    "remote": "display_remote_sensing_metrics.csv",
    "literature": "display_literature_carbon_records.csv",
    "allometry": "display_species_allometry_records.csv",
}

COMMON_REQUIRED = [
    "source_id",
    "region_id",
    "indicator",
    "value",
    "unit",
    "is_proxy",
    "is_simulated",
]


def clean(value: object) -> str:
    if value is None:
        return ""
    if isinstance(value, float) and math.isnan(value):
        return ""
    text = str(value).strip()
    if text.lower() in {"nan", "none"}:
        return ""
    return text


def number(value: object) -> float | None:
    text = clean(value)
    if not text:
        return None
    try:
        return float(text)
    except ValueError:
        return None


def int_or_blank(value: object) -> str:
    val = number(value)
    if val is None:
        return ""
    return str(int(val))


def sql_quote(value: object) -> str:
    text = clean(value)
    if not text:
        return "NULL"
    return "'" + text.replace("\\", "\\\\").replace("'", "''") + "'"


def sql_num(value: object) -> str:
    val = number(value)
    if val is None:
        return "NULL"
    return f"{val:.6f}".rstrip("0").rstrip(".")


def sql_int(value: object) -> str:
    text = int_or_blank(value)
    return text if text else "NULL"


def source_fk(source_code: object) -> str:
    return (
        "(SELECT source_id FROM hainan_blue_carbon_core.t_data_source "
        f"WHERE source_code={sql_quote(source_code)} LIMIT 1)"
    )


def region_fk(region_code: object) -> str:
    return (
        "(SELECT region_id FROM hainan_blue_carbon_core.t_region "
        f"WHERE region_code={sql_quote(region_code)} LIMIT 1)"
    )


def reference_fk(source_code: object) -> str:
    return (
        "(SELECT reference_id FROM hainan_blue_carbon_intl.t_literature_reference "
        f"WHERE source_id={source_fk(source_code)} LIMIT 1)"
    )


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


def province_city_county(region_name: str, region_level: str) -> tuple[str, str, str]:
    if region_level == "PROVINCE":
        return "海南省", "", ""
    if region_name.startswith("海南岛-"):
        return "海南省", "", region_name.replace("海南岛-", "")
    if region_name == "文昌市":
        return "海南省", "文昌市", ""
    if "文昌" in region_name:
        return "海南省", "文昌市", region_name
    if region_name.endswith("市") or region_name.endswith("县"):
        return "海南省", region_name, ""
    return "海南省", "", ""


def stat_method(indicator_code: str, unit: str) -> str:
    code = indicator_code.upper()
    if unit in {"ha", "hm2", "km2"} or code.endswith("_AREA"):
        return "AREA"
    if "COUNT" in code:
        return "COUNT"
    if "MIN" in code:
        return "MIN"
    if "MAX" in code:
        return "MAX"
    if "TOTAL" in code or "POOL" in code or "STORAGE" in code:
        return "SUM"
    return "MEAN"


def carbon_pool(indicator_code: str) -> str:
    code = indicator_code.upper()
    if "SOC" in code or "SOIL" in code:
        return "SOC"
    if "BELOWGROUND" in code or "BGB" in code:
        return "BGB"
    if "AGB" in code:
        return "AGB"
    if "TOTAL" in code or "DENSITY" in code or "ECOSYSTEM" in code:
        return "TOTAL"
    return "OTHER"


def depth_from_method(method: str) -> tuple[str, str]:
    match = re.search(r"(\d+(?:\.\d+)?)\s*-\s*(\d+(?:\.\d+)?)\s*cm", method)
    if not match:
        return "", ""
    return match.group(1), match.group(2)


def write_csv(path: Path, rows: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if not rows:
        path.write_text("", encoding="utf-8")
        return
    columns: list[str] = []
    for row in rows:
        for key in row:
            if key not in columns:
                columns.append(key)
    with path.open("w", newline="", encoding="utf-8-sig") as fh:
        writer = csv.DictWriter(fh, fieldnames=columns)
        writer.writeheader()
        writer.writerows(rows)


def reject(
    rejected: list[dict],
    source_file: str,
    row: dict,
    reason: str,
    recommended_action: str,
) -> None:
    rejected.append(
        {
            "source_file": source_file,
            "record_id": clean(row.get("record_id")),
            "source_id": clean(row.get("source_id")),
            "region_id": clean(row.get("region_id")),
            "indicator_code": clean(row.get("indicator_code")),
            "value": clean(row.get("value")),
            "unit": clean(row.get("unit")),
            "is_proxy": clean(row.get("is_proxy")),
            "is_simulated": clean(row.get("is_simulated")),
            "reason": reason,
            "recommended_action": recommended_action,
        }
    )


def build_import_ready() -> tuple[dict[str, list[dict]], list[dict]]:
    schema_text = SCHEMA_PATH.read_text(encoding="utf-8")
    required_tables = [
        "t_region",
        "t_data_source",
        "t_satellite_mangrove_cover",
        "t_satellite_region_metric",
        "t_literature_reference",
        "t_literature_carbon_record",
        "t_allometry_equation",
    ]
    missing_tables = [table for table in required_tables if f"CREATE TABLE `{table}`" not in schema_text]
    if missing_tables:
        raise RuntimeError(f"Schema missing required tables: {missing_tables}")

    frames = {
        key: pd.read_csv(NORMALIZED_DIR / filename).fillna("")
        for key, filename in DISPLAY_FILES.items()
    }
    rejected: list[dict] = []

    source_rows: list[dict] = []
    for idx, row in frames["sources"].iterrows():
        source_code = clean(row["source_id"])
        proxy_count = int(number(row["proxy_count"]) or 0)
        record_count = int(number(row["record_count"]) or 0)
        source_rows.append(
            {
                "import_record_id": f"CORE-SRC-{idx + 1:05d}",
                "target_schema": "hainan_blue_carbon_core",
                "target_table": "t_data_source",
                "source_id": source_code,
                "region_id": "REG_GLOBAL",
                "indicator": "DATA_SOURCE_RECORD_COUNT",
                "value": record_count,
                "unit": "records",
                "is_proxy": 1 if proxy_count else 0,
                "is_simulated": int(row["is_simulated"]),
                "source_code": source_code,
                "source_category": source_category(clean(row["source_type"])),
                "source_name": clean(row["source_name"]),
                "source_url": clean(row["source_url"]),
                "source_type": clean(row["source_type"]),
                "quality_level": clean(row["quality_level"]),
                "record_count": record_count,
                "proxy_count": proxy_count,
                "direct_count": int(number(row["direct_count"]) or 0),
                "locked_high_value_source": int(number(row["locked_high_value_source"]) or 0),
                "citation_text": clean(row["source_name"]),
                "version_label": "HNBLUE V2.0 public normalized package",
                "remark": clean(row["notes"]),
                "notes": "Natural source_code is preserved as source_id in import_ready CSV; target table uses AUTO_INCREMENT source_id.",
            }
        )

    region_rows: list[dict] = []
    for idx, row in frames["regions"].iterrows():
        region_code = clean(row["region_id"])
        region_name = clean(row["region_name"])
        region_level = clean(row["region_level"])
        province, city, county = province_city_county(region_name, region_level)
        needs_boundary = int(number(row["needs_boundary_file"]) or 0)
        region_rows.append(
            {
                "import_record_id": f"CORE-REG-{idx + 1:05d}",
                "target_schema": "hainan_blue_carbon_core",
                "target_table": "t_region",
                "source_id": "PUBLIC_RESULT_PACKAGE",
                "region_id": region_code,
                "indicator": "REGION_NEEDS_BOUNDARY_FILE",
                "value": needs_boundary,
                "unit": "flag",
                "is_proxy": 0,
                "is_simulated": int(row["is_simulated"]),
                "region_code": region_code,
                "parent_region_code": clean(row["parent_region_id"]),
                "region_name": region_name,
                "region_level": region_level,
                "ecosystem_type": clean(row["ecosystem_type"]) or "MANGROVE",
                "source_scope": clean(row["source_scope"]),
                "needs_boundary_file": needs_boundary,
                "data_scope": "PUBLIC",
                "province": province,
                "city": city,
                "county": county,
                "remark": clean(row["notes"]),
                "notes": "Region geometry is not imported; boundary-dependent rows keep needs_boundary_file flag.",
            }
        )

    cover_rows: list[dict] = []
    for _, row in frames["area"].iterrows():
        record = row.to_dict()
        indicator = clean(row["indicator_code"])
        unit = clean(row["unit"])
        value = number(row["value"])
        if indicator != "MANGROVE_AREA" or unit not in {"ha", "hm2"} or value is None:
            reject(
                rejected,
                DISPLAY_FILES["area"],
                record,
                "Only absolute MANGROVE_AREA in ha/hm2 maps to t_satellite_mangrove_cover.mangrove_area_ha.",
                "Keep index, rank, share, change and restoration metrics in a future generic satellite metric import.",
            )
            continue
        cover_rows.append(
            {
                "import_record_id": f"SAT-COVER-{len(cover_rows) + 1:05d}",
                "target_schema": "hainan_blue_carbon_satellite",
                "target_table": "t_satellite_mangrove_cover",
                "source_id": clean(row["source_id"]),
                "region_id": clean(row["region_id"]),
                "indicator": indicator,
                "value": value,
                "unit": "ha",
                "is_proxy": int(number(row["is_proxy"]) or 0),
                "is_simulated": int(number(row["is_simulated"]) or 0),
                "source_record_id": clean(row["record_id"]),
                "metric_year": int_or_blank(row["year_start"]),
                "mangrove_area_ha": value,
                "cover_ratio": "",
                "classification_method": clean(row["method"]),
                "overall_accuracy": "",
                "kappa": "",
                "quality_level": clean(row["quality_level"]),
                "year_or_period": clean(row["year_or_period"]),
                "method": clean(row["method"]),
                "notes": clean(row["notes"]),
            }
        )

    metric_rows: list[dict] = []
    for _, row in frames["remote"].iterrows():
        value = number(row["value"])
        if value is None:
            reject(
                rejected,
                DISPLAY_FILES["remote"],
                row.to_dict(),
                "Remote sensing record has no numeric value.",
                "Exclude until a numeric regional statistic is available.",
            )
            continue
        indicator = clean(row["indicator_code"])
        unit = clean(row["unit"])
        metric_rows.append(
            {
                "import_record_id": f"SAT-METRIC-{len(metric_rows) + 1:05d}",
                "target_schema": "hainan_blue_carbon_satellite",
                "target_table": "t_satellite_region_metric",
                "source_id": clean(row["source_id"]),
                "region_id": clean(row["region_id"]),
                "indicator": indicator,
                "value": value,
                "unit": unit,
                "is_proxy": int(number(row["is_proxy"]) or 0),
                "is_simulated": int(number(row["is_simulated"]) or 0),
                "source_record_id": clean(row["record_id"]),
                "metric_year": int_or_blank(row["year_start"]),
                "metric_month": "",
                "metric_date": "",
                "indicator_code": indicator,
                "indicator_name": clean(row["indicator_name"]),
                "indicator_label": clean(row["indicator_label"]),
                "indicator_group": clean(row["indicator_group"]),
                "stat_method": stat_method(indicator, unit),
                "quality_level": clean(row["quality_level"]),
                "year_or_period": clean(row["year_or_period"]),
                "method": clean(row["method"]),
                "notes": clean(row["notes"]),
            }
        )

    carbon_rows: list[dict] = []
    accepted_units = {"MgC/ha", "Mg/ha"}
    accepted_indicators = {
        "SOC_STORAGE",
        "EF_SOIL",
        "EF_AGB",
        "BELOWGROUND_CARBON_FROM_AGB",
        "BELOWGROUND_CARBON_FROM_MEAN_AGB",
        "TOTAL_BIOMASS_SOIL_CARBON",
        "CARBON_DENSITY_MIN",
        "CARBON_DENSITY_MAX",
        "CARBON_DENSITY_MEAN",
    }
    for _, row in frames["literature"].iterrows():
        record = row.to_dict()
        indicator = clean(row["indicator_code"])
        unit = clean(row["unit"])
        value = number(row["value"])
        if indicator not in accepted_indicators or unit not in accepted_units or value is None:
            reject(
                rejected,
                DISPLAY_FILES["literature"],
                record,
                "Record is not a carbon-density value supported by t_literature_carbon_record.value_mg_ha.",
                "Keep as source evidence or route to indicator dictionary / satellite metric / future external observation table.",
            )
            continue
        top, bottom = depth_from_method(clean(row["method"]))
        carbon_rows.append(
            {
                "import_record_id": f"INTL-CARBON-{len(carbon_rows) + 1:05d}",
                "target_schema": "hainan_blue_carbon_intl",
                "target_table": "t_literature_carbon_record",
                "source_id": clean(row["source_id"]),
                "region_id": clean(row["region_id"]),
                "indicator": indicator,
                "value": value,
                "unit": unit,
                "is_proxy": int(number(row["is_proxy"]) or 0),
                "is_simulated": int(number(row["is_simulated"]) or 0),
                "source_record_id": clean(row["record_id"]),
                "reference_source_id": clean(row["source_id"]),
                "site_name": clean(row["region_name"]),
                "ecosystem_type": "MANGROVE",
                "carbon_pool": carbon_pool(indicator),
                "value_mg_ha": value,
                "depth_top_cm": top,
                "depth_bottom_cm": bottom,
                "quality_level": clean(row["quality_level"]),
                "year_or_period": clean(row["year_or_period"]),
                "method_note": (
                    f"{clean(row['method'])}; original_indicator={indicator}; "
                    f"original_unit={unit}; is_proxy={int(number(row['is_proxy']) or 0)}; "
                    f"notes={clean(row['notes'])}"
                ),
                "notes": clean(row["notes"]),
            }
        )

    allometry_rows: list[dict] = []
    allometry = frames["allometry"]
    komiyama = allometry[allometry["source_id"] == "SRC_ALLO_KOMIYAMA_2005"]
    if not komiyama.empty:
        allometry_rows.extend(
            [
                {
                    "import_record_id": "INTL-ALLO-00001",
                    "target_schema": "hainan_blue_carbon_intl",
                    "target_table": "t_allometry_equation",
                    "source_id": "SRC_ALLO_KOMIYAMA_2005",
                    "region_id": "REG_GLOBAL",
                    "indicator": "ALLOMETRIC_EQUATION",
                    "value": 0.251,
                    "unit": "equation",
                    "is_proxy": 0,
                    "is_simulated": 0,
                    "source_record_id": "SPAL-00018;SPAL-00019",
                    "reference_source_id": "SRC_ALLO_KOMIYAMA_2005",
                    "country": "",
                    "province": "",
                    "scientific_name": "Mangrove spp.",
                    "equation_form": "Wtop=0.251*rho*D^2.46",
                    "component": "ABOVEGROUND",
                    "coefficient_a": 0.251,
                    "coefficient_b": 2.46,
                    "coefficient_c": "",
                    "coefficient_d": "",
                    "r_squared": "",
                    "sample_size": "",
                    "dbh_min_cm": "",
                    "dbh_max_cm": "",
                    "height_min_m": "",
                    "height_max_m": "",
                    "unit_note": "D cm; rho g/cm3; W kg. Built from normalized coefficient records SPAL-00018/SPAL-00019.",
                    "quality_level": "A",
                    "method": "abstract_value; Equation; Wtop=0.251*rho*D^2.46",
                    "notes": "Generic mangrove aboveground biomass allometric equation.",
                },
                {
                    "import_record_id": "INTL-ALLO-00002",
                    "target_schema": "hainan_blue_carbon_intl",
                    "target_table": "t_allometry_equation",
                    "source_id": "SRC_ALLO_KOMIYAMA_2005",
                    "region_id": "REG_GLOBAL",
                    "indicator": "ALLOMETRIC_EQUATION",
                    "value": 0.199,
                    "unit": "equation",
                    "is_proxy": 0,
                    "is_simulated": 0,
                    "source_record_id": "SPAL-00020;SPAL-00021;LIT-00034",
                    "reference_source_id": "SRC_ALLO_KOMIYAMA_2005",
                    "country": "",
                    "province": "",
                    "scientific_name": "Mangrove spp.",
                    "equation_form": "Wr=0.199*rho^0.899*D^2.22",
                    "component": "BELOWGROUND",
                    "coefficient_a": 0.199,
                    "coefficient_b": 2.22,
                    "coefficient_c": 0.899,
                    "coefficient_d": "",
                    "r_squared": "",
                    "sample_size": "",
                    "dbh_min_cm": "",
                    "dbh_max_cm": "",
                    "height_min_m": "",
                    "height_max_m": "",
                    "unit_note": "D cm; rho g/cm3; W kg. coefficient_c stores rho exponent.",
                    "quality_level": "A",
                    "method": "abstract_value; Equation; Wr=0.199*rho^0.899*D^2.22",
                    "notes": "Generic mangrove root biomass allometric equation.",
                },
            ]
        )
    accepted_spal = {"SPAL-00018", "SPAL-00019", "SPAL-00020", "SPAL-00021"}
    for _, row in allometry.iterrows():
        if clean(row["record_id"]) in accepted_spal:
            continue
        reject(
            rejected,
            DISPLAY_FILES["allometry"],
            row.to_dict(),
            "Record is species presence, species count, root-shoot ratio or single parameter that does not independently form a t_allometry_equation row.",
            "Keep as evidence; route species records to a future species-composition table or keep root-shoot ratio as model parameter.",
        )

    for filename in [
        "frontend_mangrove_area_timeseries.json",
        "frontend_remote_sensing_metrics.json",
        "frontend_literature_carbon_cards.json",
    ]:
        rejected.append(
            {
                "source_file": filename,
                "record_id": "",
                "source_id": "",
                "region_id": "",
                "indicator_code": "",
                "value": "",
                "unit": "",
                "is_proxy": "",
                "is_simulated": "0",
                "reason": "Frontend JSON is a display derivative and is excluded from database import.",
                "recommended_action": "Use normalized display CSV as ingestion source; keep JSON for frontend only.",
            }
        )

    return (
        {
            "core_data_sources_import_ready.csv": source_rows,
            "core_regions_import_ready.csv": region_rows,
            "satellite_mangrove_cover_import_ready.csv": cover_rows,
            "satellite_region_metric_import_ready.csv": metric_rows,
            "intl_literature_carbon_record_import_ready.csv": carbon_rows,
            "intl_allometry_equation_import_ready.csv": allometry_rows,
        },
        rejected,
    )


def build_sql(imports: dict[str, list[dict]]) -> str:
    today = date.today().isoformat()
    lines: list[str] = [
        "-- HNBLUE V2.0 public data controlled import preview",
        f"-- Generated on {today}",
        "-- Preview only: this file is not executed by the build script.",
        "-- 本轮未连接 MySQL，未执行导入。",
        "SET NAMES utf8mb4;",
        "-- Foreign key checks are intentionally left enabled for controlled review.",
        "",
        "-- 1. Core data sources",
    ]

    for row in imports["core_data_sources_import_ready.csv"]:
        lines.append(
            "INSERT IGNORE INTO hainan_blue_carbon_core.t_data_source "
            "(source_code, source_category, source_name, source_url, citation_text, version_label, is_simulated, remark) VALUES "
            f"({sql_quote(row['source_code'])}, {sql_quote(row['source_category'])}, {sql_quote(row['source_name'])}, "
            f"{sql_quote(row['source_url'])}, {sql_quote(row['citation_text'])}, {sql_quote(row['version_label'])}, "
            f"{int(row['is_simulated'])}, {sql_quote(row['remark'])});"
        )

    lines.append("")
    lines.append("-- 2. Core regions")
    for row in imports["core_regions_import_ready.csv"]:
        parent = (
            "NULL"
            if not clean(row["parent_region_code"])
            else region_fk(row["parent_region_code"])
        )
        lines.append(
            "INSERT IGNORE INTO hainan_blue_carbon_core.t_region "
            "(region_code, parent_region_id, region_name, region_level, province, city, county, ecosystem_type, data_scope, remark) VALUES "
            f"({sql_quote(row['region_code'])}, {parent}, {sql_quote(row['region_name'])}, {sql_quote(row['region_level'])}, "
            f"{sql_quote(row['province'])}, {sql_quote(row['city'])}, {sql_quote(row['county'])}, "
            f"{sql_quote(row['ecosystem_type'])}, 'PUBLIC', {sql_quote(row['remark'])});"
        )

    indicators = {
        row["indicator_code"]: row
        for row in imports["satellite_region_metric_import_ready.csv"]
    }
    lines.append("")
    lines.append("-- 3. Indicator dictionary needed by satellite metric FK")
    for code, row in sorted(indicators.items()):
        domain = "CARBON_STOCK" if "CARBON" in code or "BIOMASS" in code else "REMOTE_SENSING"
        lines.append(
            "INSERT IGNORE INTO hainan_blue_carbon_core.t_indicator_dictionary "
            "(indicator_code, indicator_name_cn, indicator_name_en, indicator_domain, default_unit, value_type, method_note) VALUES "
            f"({sql_quote(code)}, {sql_quote(row['indicator_label'] or row['indicator_name'])}, "
            f"{sql_quote(row['indicator_name'])}, {sql_quote(domain)}, {sql_quote(row['unit'])}, "
            f"'NUMERIC', {sql_quote(row['method'])});"
        )

    ref_sources = sorted(
        {
            row["source_id"]
            for row in imports["intl_literature_carbon_record_import_ready.csv"]
        }
        | {
            row["source_id"]
            for row in imports["intl_allometry_equation_import_ready.csv"]
        }
    )
    source_lookup = {
        row["source_code"]: row for row in imports["core_data_sources_import_ready.csv"]
    }
    lines.append("")
    lines.append("-- 4. Literature references used by intl tables")
    for source_code in ref_sources:
        source = source_lookup.get(source_code, {})
        year_match = re.search(r"(19|20)\d{2}", source_code)
        pub_year = year_match.group(0) if year_match else "NULL"
        lines.append(
            "INSERT INTO hainan_blue_carbon_intl.t_literature_reference "
            "(source_id, title, publication_year, url, study_area, ecosystem_type, citation_text) "
            "SELECT "
            f"{source_fk(source_code)}, {sql_quote(source.get('source_name', source_code))}, {pub_year}, "
            f"{sql_quote(source.get('source_url', ''))}, 'Hainan / public literature package', 'MANGROVE', "
            f"{sql_quote(source.get('citation_text', source_code))} "
            "WHERE NOT EXISTS (SELECT 1 FROM hainan_blue_carbon_intl.t_literature_reference "
            f"WHERE source_id={source_fk(source_code)});"
        )

    lines.append("")
    lines.append("-- 5. Satellite mangrove cover")
    for row in imports["satellite_mangrove_cover_import_ready.csv"]:
        lines.append(
            f"-- source_record_id={row['source_record_id']}; is_proxy={row['is_proxy']}; is_simulated={row['is_simulated']}"
        )
        lines.append(
            "INSERT INTO hainan_blue_carbon_satellite.t_satellite_mangrove_cover "
            "(region_id, metric_year, mangrove_area_ha, classification_method, source_id) VALUES "
            f"({region_fk(row['region_id'])}, {sql_int(row['metric_year'])}, {sql_num(row['mangrove_area_ha'])}, "
            f"{sql_quote(row['classification_method'])}, {source_fk(row['source_id'])});"
        )

    lines.append("")
    lines.append("-- 6. Satellite region metrics")
    for row in imports["satellite_region_metric_import_ready.csv"]:
        lines.append(
            f"-- source_record_id={row['source_record_id']}; is_proxy={row['is_proxy']}; is_simulated={row['is_simulated']}"
        )
        lines.append(
            "INSERT INTO hainan_blue_carbon_satellite.t_satellite_region_metric "
            "(region_id, indicator_code, metric_year, value, unit, stat_method, source_id, is_simulated) VALUES "
            f"({region_fk(row['region_id'])}, {sql_quote(row['indicator_code'])}, {sql_int(row['metric_year'])}, "
            f"{sql_num(row['value'])}, {sql_quote(row['unit'])}, {sql_quote(row['stat_method'])}, "
            f"{source_fk(row['source_id'])}, {int(row['is_simulated'])});"
        )

    lines.append("")
    lines.append("-- 7. Literature carbon records; regional means remain literature evidence, not ground samples")
    for row in imports["intl_literature_carbon_record_import_ready.csv"]:
        lines.append(
            f"-- source_record_id={row['source_record_id']}; is_proxy={row['is_proxy']}; is_simulated={row['is_simulated']}"
        )
        lines.append(
            "INSERT INTO hainan_blue_carbon_intl.t_literature_carbon_record "
            "(reference_id, region_id, site_name, ecosystem_type, carbon_pool, value_mg_ha, depth_top_cm, depth_bottom_cm, method_note, source_id) VALUES "
            f"({reference_fk(row['reference_source_id'])}, {region_fk(row['region_id'])}, {sql_quote(row['site_name'])}, "
            f"'MANGROVE', {sql_quote(row['carbon_pool'])}, {sql_num(row['value_mg_ha'])}, "
            f"{sql_num(row['depth_top_cm'])}, {sql_num(row['depth_bottom_cm'])}, {sql_quote(row['method_note'])}, "
            f"{source_fk(row['source_id'])});"
        )

    lines.append("")
    lines.append("-- 8. Allometry equations")
    for row in imports["intl_allometry_equation_import_ready.csv"]:
        lines.append(
            f"-- source_record_id={row['source_record_id']}; is_proxy={row['is_proxy']}; is_simulated={row['is_simulated']}"
        )
        lines.append(
            "INSERT INTO hainan_blue_carbon_intl.t_allometry_equation "
            "(reference_id, country, province, scientific_name, equation_form, component, coefficient_a, coefficient_b, coefficient_c, coefficient_d, r_squared, sample_size, dbh_min_cm, dbh_max_cm, height_min_m, height_max_m, unit_note) VALUES "
            f"({reference_fk(row['reference_source_id'])}, {sql_quote(row['country'])}, {sql_quote(row['province'])}, "
            f"{sql_quote(row['scientific_name'])}, {sql_quote(row['equation_form'])}, {sql_quote(row['component'])}, "
            f"{sql_num(row['coefficient_a'])}, {sql_num(row['coefficient_b'])}, {sql_num(row['coefficient_c'])}, "
            f"{sql_num(row['coefficient_d'])}, {sql_num(row['r_squared'])}, {sql_int(row['sample_size'])}, "
            f"{sql_num(row['dbh_min_cm'])}, {sql_num(row['dbh_max_cm'])}, {sql_num(row['height_min_m'])}, "
            f"{sql_num(row['height_max_m'])}, {sql_quote(row['unit_note'])});"
        )

    lines.append("")
    return "\n".join(lines)


def write_rejected(rejected: list[dict]) -> None:
    lines = [
        "# HNBLUE V2.0 Phase 2 rejected / deferred records",
        "",
        "本文件列出本轮不能直接进入 V2 目标表或明确排除的记录。本轮未连接 MySQL，未执行导入。",
        "",
        f"- 拒收或暂缓记录数：{len(rejected)}",
        "- 三个 `frontend_*.json` 是展示派生文件，不进入数据库。",
        "- 文献区域均值只映射到 `hainan_blue_carbon_intl.t_literature_carbon_record`，不得写入样方、样木或土壤样品表。",
        "",
        "| source_file | record_id | source_id | region_id | indicator_code | value | unit | is_proxy | is_simulated | reason | recommended_action |",
        "|---|---|---|---|---|---:|---|---:|---:|---|---|",
    ]
    for row in rejected:
        lines.append(
            "| "
            + " | ".join(
                clean(row.get(col)).replace("|", "/")
                for col in [
                    "source_file",
                    "record_id",
                    "source_id",
                    "region_id",
                    "indicator_code",
                    "value",
                    "unit",
                    "is_proxy",
                    "is_simulated",
                    "reason",
                    "recommended_action",
                ]
            )
            + " |"
        )
    REJECT_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_report(imports: dict[str, list[dict]], rejected: list[dict]) -> None:
    counts = {name: len(rows) for name, rows in imports.items()}
    proxy_counts = {
        name: sum(int(number(row.get("is_proxy")) or 0) for row in rows)
        for name, rows in imports.items()
    }
    sim_counts = {
        name: sum(int(number(row.get("is_simulated")) or 0) for row in rows)
        for name, rows in imports.items()
    }
    lines = [
        "# HNBLUE V2.0 Phase 2 公开数据受控入库映射报告",
        "",
        "本报告基于 `docs/v2/HNBLUE_V2_phase1_handoff_report.md`、6 个 normalized display CSV 和 `sql/schema/hnblue_v2_schema.sql` 生成。",
        "",
        "**本轮未连接 MySQL，未执行导入。**",
        "",
        "## 1. 生成产物",
        "",
        "| 文件 | 目标 Schema / 表 | 记录数 | proxy 记录数 | is_simulated 合计 | 入库定位 |",
        "|---|---|---:|---:|---:|---|",
    ]
    target_desc = {
        "core_data_sources_import_ready.csv": "hainan_blue_carbon_core.t_data_source",
        "core_regions_import_ready.csv": "hainan_blue_carbon_core.t_region",
        "satellite_mangrove_cover_import_ready.csv": "hainan_blue_carbon_satellite.t_satellite_mangrove_cover",
        "satellite_region_metric_import_ready.csv": "hainan_blue_carbon_satellite.t_satellite_region_metric",
        "intl_literature_carbon_record_import_ready.csv": "hainan_blue_carbon_intl.t_literature_carbon_record",
        "intl_allometry_equation_import_ready.csv": "hainan_blue_carbon_intl.t_allometry_equation",
    }
    for name in imports:
        lines.append(
            f"| `{name}` | `{target_desc[name]}` | {counts[name]} | {proxy_counts[name]} | {sim_counts[name]} | import_ready CSV，等待人工审核后执行 SQL |"
        )

    lines.extend(
        [
            "",
            f"- SQL preview：`sql/seed/hnblue_v2_public_import_preview.sql`",
            f"- 拒收清单：`docs/v2/HNBLUE_V2_phase2_rejected_records.md`，共 {len(rejected)} 条。",
            "- SQL 文件仅为预览，不包含连接串，不由脚本执行。",
            "",
            "## 2. 字段映射",
            "",
            "| 源 CSV | 入库 CSV | 源字段 | 目标表字段 | 映射规则 | 风险控制 |",
            "|---|---|---|---|---|---|",
            "| `display_data_sources.csv` | `core_data_sources_import_ready.csv` | `source_id` | `source_code` | 保留自然来源编码；V2 `source_id` 由 MySQL 自增 | 不把自然编码写入自增主键 |",
            "| `display_data_sources.csv` | `core_data_sources_import_ready.csv` | `source_type` | `source_category` | peer reviewed / public / government 类型映射到 schema enum | 原始 `source_type` 同步保留 |",
            "| `display_data_sources.csv` | `core_data_sources_import_ready.csv` | `record_count`,`proxy_count`,`direct_count` | `remark` / import audit fields | 作为来源层级质量统计 | `proxy_count` 不等于观测表行级 proxy |",
            "| `display_region_dictionary.csv` | `core_regions_import_ready.csv` | `region_id` | `region_code` | 保留自然区域编码；V2 `region_id` 由 MySQL 自增 | 后续 FK 使用 `region_code` 子查询解析 |",
            "| `display_region_dictionary.csv` | `core_regions_import_ready.csv` | `parent_region_id` | `parent_region_id` | 通过父级 `region_code` 子查询解析 | 空父级保持 NULL |",
            "| `display_region_dictionary.csv` | `core_regions_import_ready.csv` | `needs_boundary_file` | import audit field / `remark` | 边界缺失只作为入库风险标记 | 不伪造 geometry_wkt |",
            "| `display_mangrove_area_timeseries.csv` | `satellite_mangrove_cover_import_ready.csv` | `value`,`unit` | `mangrove_area_ha` | 仅接收 `MANGROVE_AREA` 且单位为 ha/hm2 的绝对面积 | index/share/rank/change/revegetation 已暂缓 |",
            "| `display_mangrove_area_timeseries.csv` | `satellite_mangrove_cover_import_ready.csv` | `year_start` | `metric_year` | 年份转整数 | 非年度值不导入 cover 表 |",
            "| `display_remote_sensing_metrics.csv` | `satellite_region_metric_import_ready.csv` | `indicator_code` | `indicator_code` | 同步生成 indicator dictionary 预览 SQL | proxy 继续保留在 import_ready 与 SQL 注释中 |",
            "| `display_remote_sensing_metrics.csv` | `satellite_region_metric_import_ready.csv` | `value`,`unit` | `value`,`unit` | 只接收非空真实数值 | MODIS/ERA5/CHIRPS/模型结果不改写为实测 |",
            "| `display_literature_carbon_records.csv` | `intl_literature_carbon_record_import_ready.csv` | `indicator_code` | `carbon_pool` | SOC/soil -> SOC，AGB -> AGB，belowground -> BGB，total/density -> TOTAL | GHG、面积、气候、样本量、trait 不进入碳储表 |",
            "| `display_literature_carbon_records.csv` | `intl_literature_carbon_record_import_ready.csv` | `value`,`unit` | `value_mg_ha` | 仅接收 MgC/ha 或 Mg/ha 碳密度值 | MgC 总量、MgCO2e/yr 排放量暂缓 |",
            "| `display_species_allometry_records.csv` | `intl_allometry_equation_import_ready.csv` | `EQUATION_A`,`EQUATION_B` | `coefficient_a`,`coefficient_b`,`coefficient_c` | Komiyama 2005 系数组合成 2 条完整方程 | 物种存在、物种数、root-shoot ratio 暂缓 |",
            "",
            "## 3. 关键约束",
            "",
            "- `is_simulated` 全部为 0。",
            "- proxy 数据在 import_ready CSV 中保留 `is_proxy=1`；目标 schema 当前无统一 `is_proxy` 字段，SQL preview 在行级注释保留该标记。",
            "- 文献区域均值只进入 `hainan_blue_carbon_intl.t_literature_carbon_record`，不得写入 ground 样方、样木、土壤样品表。",
            "- 三个 `frontend_*.json` 不进入数据库，只作为前端展示派生文件。",
            "- `sql/seed/hnblue_v2_public_import_preview.sql` 是人工审阅用 SQL，当前脚本不会连接 MySQL，也不会执行导入。",
            "",
            "## 4. 推荐执行顺序",
            "",
            "1. 审阅 `core_data_sources_import_ready.csv` 与 `core_regions_import_ready.csv`。",
            "2. 确认 region/source 自然编码与 V2 主键映射。",
            "3. 执行 indicator dictionary、literature reference 预置 SQL。",
            "4. 导入 satellite 与 intl 事实表。",
            "5. 生成 local/display 兼容层，逐步替换旧模拟展示数据。",
        ]
    )
    REPORT_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")


def validate_outputs(imports: dict[str, list[dict]]) -> dict[str, dict[str, int]]:
    summary: dict[str, dict[str, int]] = {}
    for name, rows in imports.items():
        path = IMPORT_DIR / name
        df = pd.read_csv(path).fillna("")
        missing = [col for col in COMMON_REQUIRED if col not in df.columns]
        if missing:
            raise RuntimeError(f"{name} missing required columns: {missing}")
        if (df["is_simulated"].astype(int) != 0).any():
            raise RuntimeError(f"{name} contains simulated records")
        if not df["is_proxy"].astype(int).isin([0, 1]).all():
            raise RuntimeError(f"{name} contains invalid is_proxy")
        for col in ["source_id", "region_id", "indicator", "unit"]:
            if (df[col].astype(str).str.len() == 0).any():
                raise RuntimeError(f"{name} has blank {col}")
        if (df["value"].astype(str).str.len() == 0).any():
            raise RuntimeError(f"{name} has blank value")
        summary[name] = {
            "rows": len(df),
            "sources": df["source_id"].nunique(),
            "regions": df["region_id"].nunique(),
            "indicators": df["indicator"].nunique(),
            "proxy": int(df["is_proxy"].astype(int).sum()),
            "simulated": int(df["is_simulated"].astype(int).sum()),
        }
    return summary


def main() -> None:
    imports, rejected = build_import_ready()
    IMPORT_DIR.mkdir(parents=True, exist_ok=True)
    for name, rows in imports.items():
        write_csv(IMPORT_DIR / name, rows)
    write_rejected(rejected)
    SQL_PATH.write_text(build_sql(imports), encoding="utf-8")
    write_report(imports, rejected)
    summary = validate_outputs(imports)
    print("HNBLUE V2 public import_ready validation")
    for name, item in summary.items():
        print(
            f"{name}: rows={item['rows']}, sources={item['sources']}, regions={item['regions']}, "
            f"indicators={item['indicators']}, proxy={item['proxy']}, simulated={item['simulated']}"
        )
    print(f"rejected_or_deferred={len(rejected)}")
    print("mysql_connected=0")
    print("sql_executed=0")


if __name__ == "__main__":
    main()
