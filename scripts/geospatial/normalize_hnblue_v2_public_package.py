from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Iterable

import pandas as pd


ROOT = Path(__file__).resolve().parents[2]
BASE_DIR = ROOT / "data" / "results" / "hnblue_v2_public"
NORM_DIR = BASE_DIR / "normalized"
REPORT_PATH = ROOT / "docs" / "data_collection" / "hnblue_v2_public_result_next_report.md"

BASE_COLUMNS = [
    "source_name",
    "source_url",
    "source_type",
    "region_name",
    "year_or_period",
    "indicator_name",
    "value",
    "unit",
    "method",
    "quality_level",
    "is_proxy",
    "is_simulated",
    "notes",
]

DISPLAY_COLUMNS = [
    "record_id",
    "source_id",
    "source_name",
    "source_url",
    "source_type",
    "region_id",
    "region_name",
    "region_level",
    "year_or_period",
    "year_start",
    "year_end",
    "indicator_code",
    "indicator_name",
    "indicator_label",
    "indicator_group",
    "value",
    "unit",
    "method",
    "quality_level",
    "is_proxy",
    "is_simulated",
    "database_target",
    "frontend_group",
    "display_priority",
    "notes",
]

SOURCE_IDS = {
    "Using Medium-Resolution Remote Sensing Satellite Images to Evaluate Recent Changes and Future Development Trends of Mangrove Forests on Hainan Island, China": "SRC_LIT_FORESTS_HI_MANGROVE_RS_2023",
    "Incorporating coastal blue carbon into subnational greenhouse gas inventories": "SRC_LIT_HI_GHG_2022",
    "Hainan blue carbon inventory": "SRC_LIT_HI_GHG_2022",
    "Spatial Variation of Soil Organic Carbon from Bamen Bay Mangrove in Southern China": "SRC_LIT_BAMEN_SOC_2022",
    "Bamen Bay SOC paper": "SRC_LIT_BAMEN_SOC_2022",
    "The impact of land use change on carbon storage and multi-scenario prediction in Hainan Island using InVEST and CA-Markov models": "SRC_LIT_FRONTIERS_HI_LULC_INVEST_2024",
    "Common allometric equations for estimating the tree weight of mangroves": "SRC_ALLO_KOMIYAMA_2005",
    "Hainan blue carbon public information": "SRC_GOV_HI_BLUE_CARBON_2023",
    "海南发挥环境资源优势与自贸港政策优势，积极推动蓝碳事业发展": "SRC_GOV_HI_BLUE_CARBON_2023",
    "海南蓝碳政府公开信息": "SRC_GOV_HI_BLUE_CARBON_2023",
}

INDICATOR_LABELS = {
    "mangrove_area": "红树林面积",
    "remote_sensing_mangrove_area": "遥感解译红树林面积",
    "protected_area_total_area": "保护区总面积",
    "natural_mangrove_area": "天然红树林面积",
    "planted_mangrove_area": "人工红树林面积",
    "natural_mangrove_area_share": "天然红树林占比",
    "planted_mangrove_area_share": "人工红树林占比",
    "mangrove_area_change": "红树林面积变化量",
    "mangrove_area_change_rate": "红树林面积变化率",
    "mangrove_area_increase_rate": "红树林面积年均增长率",
    "revegetated_tidal_flat_or_pond": "滩涂/养殖塘恢复红树林面积",
    "SOC_storage": "土壤有机碳储量",
    "mean_SOC_content": "平均土壤有机碳含量",
    "EF_AGB": "地上生物量碳排放因子",
    "EF_soil": "土壤碳排放因子",
    "belowground_carbon_from_agb": "根系碳储量估算",
    "total_biomass_soil_carbon": "生物量与土壤总碳",
    "root_shoot_ratio": "根冠比",
    "total_CO2_CH4": "CO2+CH4 清单净排放",
    "net_CO2_emission_mangrove": "红树林净 CO2 清单排放",
    "N2O_shrimp_aquaculture": "对虾养殖 N2O 清单排放",
    "mangrove_species_count": "红树植物物种数",
    "presence": "物种出现记录",
    "dominant_species_presence": "优势种出现记录",
    "equation_a": "异速方程系数 a",
    "equation_b": "异速方程指数 b",
    "wood_density_exponent": "木材密度指数",
    "sample_size_agb": "AGB 样本量",
    "sample_size_soc": "SOC 样本量",
}

REGION_ALIASES = {
    "Hainan Province": ("REG_HI", "海南省", "PROVINCE"),
    "海南省": ("REG_HI", "海南省", "PROVINCE"),
    "Hainan Island": ("REG_HI_ISLAND", "海南岛", "PROVINCE"),
    "文昌市": ("REG_WENCHANG", "文昌市", "CITY"),
    "Wenchang": ("REG_WENCHANG", "文昌市", "CITY"),
    "Wenchang / Bamen Bay Nature Reserve": ("REG_WENCHANG_BAMEN_BAY", "文昌八门湾自然保护区", "PROTECTED_AREA"),
    "全球": ("REG_GLOBAL", "全球", "OTHER"),
    "Global": ("REG_GLOBAL", "全球", "OTHER"),
}

REGION_TRANSLATIONS = {
    "Dongzhaigang": "东寨港",
    "Dongfang": "东方",
    "Huiwen": "会文",
    "Guannan": "冠南",
    "Xinyinggang": "新英港",
    "Yangpugang": "洋浦港",
    "Huachangwan": "花场湾",
    "Puqian": "铺前",
    "Maniaogang": "马袅港",
    "Haikou": "海口市",
    "Danzhou": "儋州市",
    "Sanya": "三亚市",
    "Qionghai": "琼海市",
}


def slug(value: str) -> str:
    text = re.sub(r"[^0-9A-Za-z\u4e00-\u9fff]+", "_", str(value)).strip("_")
    if not text:
        return "UNKNOWN"
    return text.upper()


def source_id(source_name: str) -> str:
    if source_name in SOURCE_IDS:
        return SOURCE_IDS[source_name]
    for key, value in SOURCE_IDS.items():
        if key and key in source_name:
            return value
    return f"SRC_{slug(source_name)[:48]}"


def parse_years(value: object) -> tuple[int | None, int | None]:
    years = [int(x) for x in re.findall(r"\d{4}", str(value))]
    if not years:
        return None, None
    return min(years), max(years)


def indicator_group(indicator: str) -> str:
    if "area" in indicator or "latitude" in indicator or "longitude" in indicator:
        return "area_boundary"
    if "carbon" in indicator.lower() or indicator in {"SOC_storage", "EF_AGB", "EF_soil"}:
        return "carbon_stock"
    if "CO2" in indicator or "CH4" in indicator or "N2O" in indicator:
        return "inventory_flux_proxy"
    if "species" in indicator or "presence" in indicator or "equation" in indicator or "ratio" in indicator:
        return "species_allometry"
    if any(k in indicator for k in ["cropland", "forestland", "grassland", "water_land", "built_land", "unused_land"]):
        return "land_use"
    return "environment_remote_sensing"


def region_info(region_name: str) -> tuple[str, str, str]:
    if region_name in REGION_ALIASES:
        return REGION_ALIASES[region_name]
    if region_name.startswith("Hainan Island - "):
        site = region_name.split(" - ", 1)[1]
        return f"REG_HI_{slug(site)}", f"海南岛-{REGION_TRANSLATIONS.get(site, site)}", "SITE_AREA"
    if region_name in REGION_TRANSLATIONS:
        return f"REG_HI_{slug(region_name)}", REGION_TRANSLATIONS[region_name], "CITY"
    return f"REG_{slug(region_name)[:56]}", region_name, "OTHER"


def clean_base(df: pd.DataFrame) -> pd.DataFrame:
    out = df.copy()
    for column in BASE_COLUMNS:
        if column not in out:
            out[column] = ""
    out = out[BASE_COLUMNS]
    out["value"] = pd.to_numeric(out["value"], errors="coerce")
    out = out[out["value"].notna()]
    out["is_proxy"] = pd.to_numeric(out["is_proxy"], errors="coerce").fillna(0).astype(int)
    out["is_simulated"] = pd.to_numeric(out["is_simulated"], errors="coerce").fillna(0).astype(int)
    out = out[out["is_simulated"] == 0]
    for column in ["source_name", "source_url", "source_type", "region_name", "indicator_name", "unit", "method", "quality_level", "notes"]:
        out[column] = out[column].fillna("").astype(str).str.strip()
    out["year_or_period"] = out["year_or_period"].fillna("").astype(str).str.strip()
    out = out[(out["source_name"] != "") & (out["source_url"] != "") & (out["region_name"] != "")]
    return out.reset_index(drop=True)


def to_display(
    df: pd.DataFrame,
    prefix: str,
    database_target: str,
    frontend_group: str,
    start_index: int = 1,
) -> pd.DataFrame:
    rows = []
    for index, row in enumerate(df.itertuples(index=False), start=start_index):
        year_start, year_end = parse_years(row.year_or_period)
        region_id, region_name, region_level = region_info(row.region_name)
        indicator = str(row.indicator_name)
        rows.append(
            {
                "record_id": f"{prefix}-{index:05d}",
                "source_id": source_id(str(row.source_name)),
                "source_name": row.source_name,
                "source_url": row.source_url,
                "source_type": row.source_type,
                "region_id": region_id,
                "region_name": region_name,
                "region_level": region_level,
                "year_or_period": row.year_or_period,
                "year_start": year_start,
                "year_end": year_end,
                "indicator_code": slug(indicator),
                "indicator_name": indicator,
                "indicator_label": INDICATOR_LABELS.get(indicator, indicator.replace("_", " ")),
                "indicator_group": indicator_group(indicator),
                "value": float(row.value),
                "unit": row.unit,
                "method": row.method,
                "quality_level": row.quality_level,
                "is_proxy": int(row.is_proxy),
                "is_simulated": int(row.is_simulated),
                "database_target": database_target,
                "frontend_group": frontend_group,
                "display_priority": 10 if int(row.is_proxy) == 0 else 30,
                "notes": row.notes,
            }
        )
    return pd.DataFrame(rows, columns=DISPLAY_COLUMNS)


def new_base_record(
    source_name: str,
    source_url: str,
    source_type: str,
    region_name: str,
    year_or_period: str,
    indicator_name: str,
    value: float,
    unit: str,
    method: str,
    quality_level: str = "A",
    is_proxy: int = 0,
    notes: str = "",
) -> dict[str, object]:
    return {
        "source_name": source_name,
        "source_url": source_url,
        "source_type": source_type,
        "region_name": region_name,
        "year_or_period": year_or_period,
        "indicator_name": indicator_name,
        "value": float(value),
        "unit": unit,
        "method": method,
        "quality_level": quality_level,
        "is_proxy": int(is_proxy),
        "is_simulated": 0,
        "notes": notes,
    }


def supplement_area(area: pd.DataFrame) -> pd.DataFrame:
    rows = []
    site_rows = area[
        (area["source_type"] == "peer_reviewed_remote_sensing_table")
        & (area["indicator_name"] == "mangrove_area")
    ].copy()
    site_rows["year_start"] = site_rows["year_or_period"].astype(str).astype(int)
    for region_name, group in site_rows.sort_values("year_start").groupby("region_name"):
        records = group.to_dict("records")
        for previous, current in zip(records, records[1:]):
            delta = current["value"] - previous["value"]
            period = f"{previous['year_or_period']}-{current['year_or_period']}"
            rows.append(
                new_base_record(
                    source_name=current["source_name"],
                    source_url=current["source_url"],
                    source_type="derived_from_peer_reviewed_remote_sensing_table",
                    region_name=region_name,
                    year_or_period=period,
                    indicator_name="mangrove_area_change",
                    value=delta,
                    unit="ha",
                    method="Difference of consecutive MDPI Forests 2023 Table 3 area values.",
                    notes="Derived change metric for frontend trend display.",
                )
            )
        first, last = records[0], records[-1]
        total_delta = last["value"] - first["value"]
        rows.append(
            new_base_record(
                source_name=last["source_name"],
                source_url=last["source_url"],
                source_type="derived_from_peer_reviewed_remote_sensing_table",
                region_name=region_name,
                year_or_period=f"{first['year_or_period']}-{last['year_or_period']}",
                indicator_name="mangrove_area_change",
                value=total_delta,
                unit="ha",
                method="Difference of first and last MDPI Forests 2023 Table 3 area values.",
                notes="Long-period change metric for frontend summary.",
            )
        )
        rows.append(
            new_base_record(
                source_name=last["source_name"],
                source_url=last["source_url"],
                source_type="derived_from_peer_reviewed_remote_sensing_table",
                region_name=region_name,
                year_or_period=f"{first['year_or_period']}-{last['year_or_period']}",
                indicator_name="mangrove_area_change_rate",
                value=100.0 * total_delta / first["value"],
                unit="percent",
                method="(last_area - first_area) / first_area from MDPI Forests 2023 Table 3.",
                notes="Long-period percentage change metric for frontend summary.",
            )
        )

    bamen = pd.read_csv(ROOT / "data" / "staging" / "core" / "region_and_project_records.csv", encoding="utf-8")
    bamen["value"] = pd.to_numeric(bamen["value"], errors="coerce")
    for indicator in ["natural_mangrove_area", "planted_mangrove_area", "mangrove_area_increase_rate", "revegetated_tidal_flat_or_pond"]:
        subset = bamen[bamen["indicator_name"] == indicator]
        for row in subset.itertuples(index=False):
            rows.append(
                new_base_record(
                    source_name=str(row.source_name),
                    source_url=str(row.source_url),
                    source_type="peer_reviewed_or_public_numeric_table",
                    region_name=str(row.region_name),
                    year_or_period=str(row.year_or_period),
                    indicator_name=indicator,
                    value=float(row.value),
                    unit=str(row.unit),
                    method=f"{row.extraction_method}; {row.table_or_figure}; {row.conversion_rule}",
                    quality_level=str(row.quality_level),
                    is_proxy=int(row.is_proxy),
                    notes=str(row.notes),
                )
            )

    bamen_total = bamen[bamen["indicator_name"] == "mangrove_area"]
    bamen_natural = bamen[bamen["indicator_name"] == "natural_mangrove_area"]
    bamen_planted = bamen[bamen["indicator_name"] == "planted_mangrove_area"]
    if not bamen_total.empty and not bamen_natural.empty and not bamen_planted.empty:
        total = float(bamen_total.iloc[0]["value"])
        for indicator, frame in [
            ("natural_mangrove_area_share", bamen_natural),
            ("planted_mangrove_area_share", bamen_planted),
        ]:
            value = 100.0 * float(frame.iloc[0]["value"]) / total
            rows.append(
                new_base_record(
                    source_name=str(frame.iloc[0]["source_name"]),
                    source_url=str(frame.iloc[0]["source_url"]),
                    source_type="derived_from_peer_reviewed_study_area_text",
                    region_name="Wenchang / Bamen Bay Nature Reserve",
                    year_or_period="2022",
                    indicator_name=indicator,
                    value=value,
                    unit="percent",
                    method="Component area divided by published 1223.3 hm2 Bamen Bay mangrove area.",
                    notes="Derived composition metric for display; not a plot measurement.",
                )
            )

    return pd.DataFrame(rows, columns=BASE_COLUMNS)


def supplement_remote(remote: pd.DataFrame) -> pd.DataFrame:
    rows = []
    for prefix in [
        "cropland",
        "forestland",
        "grassland",
        "water_land",
        "built_land",
        "unused_land",
    ]:
        area_indicator = f"{prefix}_area"
        proportion_indicator = f"{prefix}_proportion"
        area_rows = remote[(remote["indicator_name"] == area_indicator) & (remote["region_name"] == "Hainan Island")]
        prop_rows = remote[(remote["indicator_name"] == proportion_indicator) & (remote["region_name"] == "Hainan Island")]
        if {2000, 2020}.issubset(set(area_rows["year_or_period"].astype(int))):
            first = area_rows[area_rows["year_or_period"].astype(int) == 2000].iloc[0]
            last = area_rows[area_rows["year_or_period"].astype(int) == 2020].iloc[0]
            delta = float(last["value"] - first["value"])
            rows.append(
                new_base_record(
                    source_name=last["source_name"],
                    source_url=last["source_url"],
                    source_type="derived_from_peer_reviewed_remote_sensing_model_table",
                    region_name="Hainan Island",
                    year_or_period="2000-2020",
                    indicator_name=f"{prefix}_area_change",
                    value=delta,
                    unit="km2",
                    method="Difference of Frontiers 2024 Table 4 land-use area values.",
                    notes="Land-use transition summary for frontend comparison.",
                )
            )
            rows.append(
                new_base_record(
                    source_name=last["source_name"],
                    source_url=last["source_url"],
                    source_type="derived_from_peer_reviewed_remote_sensing_model_table",
                    region_name="Hainan Island",
                    year_or_period="2000-2020",
                    indicator_name=f"{prefix}_area_change_rate",
                    value=100.0 * delta / float(first["value"]),
                    unit="percent",
                    method="(2020 area - 2000 area) / 2000 area from Frontiers 2024 Table 4.",
                    notes="Land-use percentage change for frontend comparison.",
                )
            )
        if {2000, 2020}.issubset(set(prop_rows["year_or_period"].astype(int))):
            first = prop_rows[prop_rows["year_or_period"].astype(int) == 2000].iloc[0]
            last = prop_rows[prop_rows["year_or_period"].astype(int) == 2020].iloc[0]
            rows.append(
                new_base_record(
                    source_name=last["source_name"],
                    source_url=last["source_url"],
                    source_type="derived_from_peer_reviewed_remote_sensing_model_table",
                    region_name="Hainan Island",
                    year_or_period="2000-2020",
                    indicator_name=f"{prefix}_proportion_change",
                    value=float(last["value"] - first["value"]),
                    unit="percentage_point",
                    method="Difference of Frontiers 2024 Table 4 land-use proportion values.",
                    notes="Land-use share change for frontend comparison.",
                )
            )

    for indicator in [
        "aboveground_biomass_carbon_pool",
        "belowground_biomass_carbon_pool",
        "soil_carbon_pool",
        "dead_organic_carbon_pool",
        "total_carbon_storage",
        "carbon_density",
    ]:
        subset = remote[(remote["indicator_name"] == indicator) & (remote["region_name"] == "Hainan Island")]
        if {2000, 2020}.issubset(set(subset["year_or_period"].astype(int))):
            first = subset[subset["year_or_period"].astype(int) == 2000].iloc[0]
            last = subset[subset["year_or_period"].astype(int) == 2020].iloc[0]
            delta = float(last["value"] - first["value"])
            rows.append(
                new_base_record(
                    source_name=last["source_name"],
                    source_url=last["source_url"],
                    source_type="derived_from_peer_reviewed_remote_sensing_model_table",
                    region_name="Hainan Island",
                    year_or_period="2000-2020",
                    indicator_name=f"{indicator}_change",
                    value=delta,
                    unit=last["unit"],
                    method="Difference of Frontiers 2024 Table 5 InVEST carbon values.",
                    is_proxy=1,
                    notes="Model-derived carbon change proxy; not field plot data.",
                )
            )
            rows.append(
                new_base_record(
                    source_name=last["source_name"],
                    source_url=last["source_url"],
                    source_type="derived_from_peer_reviewed_remote_sensing_model_table",
                    region_name="Hainan Island",
                    year_or_period="2000-2020",
                    indicator_name=f"{indicator}_change_rate",
                    value=100.0 * delta / float(first["value"]),
                    unit="percent",
                    method="(2020 value - 2000 value) / 2000 value from Frontiers 2024 Table 5.",
                    is_proxy=1,
                    notes="Model-derived carbon change proxy; not field plot data.",
                )
            )

    return pd.DataFrame(rows, columns=BASE_COLUMNS)


def supplement_literature(lit: pd.DataFrame) -> pd.DataFrame:
    rows = []
    factor_rows = lit[lit["indicator_name"] == "root_shoot_ratio"]
    root_shoot = 0.49 if factor_rows.empty else float(factor_rows.iloc[0]["value"])
    agb = lit[(lit["indicator_name"] == "EF_AGB") & (lit["unit"] == "MgC/ha")]
    soc = lit[(lit["indicator_name"] == "EF_soil") & (lit["unit"] == "MgC/ha")]
    for region_name in sorted(set(agb["region_name"]).intersection(set(soc["region_name"]))):
        agb_row = agb[agb["region_name"] == region_name].iloc[0]
        soc_row = soc[soc["region_name"] == region_name].iloc[0]
        bgb_value = float(agb_row["value"]) * root_shoot
        total_value = float(agb_row["value"]) + bgb_value + float(soc_row["value"])
        rows.append(
            new_base_record(
                source_name=agb_row["source_name"],
                source_url=agb_row["source_url"],
                source_type="derived_from_peer_reviewed_inventory_table",
                region_name=region_name,
                year_or_period=str(agb_row["year_or_period"]),
                indicator_name="belowground_carbon_from_agb",
                value=bgb_value,
                unit="MgC/ha",
                method=f"EF_AGB * root_shoot_ratio = {float(agb_row['value'])} * {root_shoot}.",
                notes="Derived from published EF_AGB and published root-shoot ratio; regional mean only.",
            )
        )
        rows.append(
            new_base_record(
                source_name=agb_row["source_name"],
                source_url=agb_row["source_url"],
                source_type="derived_from_peer_reviewed_inventory_table",
                region_name=region_name,
                year_or_period=str(agb_row["year_or_period"]),
                indicator_name="total_biomass_soil_carbon",
                value=total_value,
                unit="MgC/ha",
                method="EF_AGB + EF_AGB * root_shoot_ratio + EF_soil from Frontiers 2022 Table 3.",
                notes="Derived regional carbon density; not a sample-level measurement.",
            )
        )
    return pd.DataFrame(rows, columns=BASE_COLUMNS)


def read_inputs() -> tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    area = clean_base(pd.read_csv(BASE_DIR / "region_mangrove_area_timeseries.csv", encoding="utf-8"))
    remote = clean_base(pd.read_csv(BASE_DIR / "region_remote_sensing_metrics.csv", encoding="utf-8"))
    literature = clean_base(pd.read_csv(BASE_DIR / "literature_carbon_numeric_records.csv", encoding="utf-8"))
    return area, remote, literature


def split_species_allometry(literature: pd.DataFrame) -> pd.DataFrame:
    mask = literature["indicator_name"].apply(lambda value: indicator_group(str(value)) == "species_allometry")
    return literature[mask].copy()


def build_region_dictionary(frames: Iterable[pd.DataFrame]) -> pd.DataFrame:
    rows = {}
    for df in frames:
        if df.empty:
            continue
        for row in df.itertuples(index=False):
            rows[row.region_id] = {
                "region_id": row.region_id,
                "region_name": row.region_name,
                "region_level": row.region_level,
                "parent_region_id": "REG_HI" if row.region_id.startswith("REG_HI_") and row.region_id != "REG_HI" else "",
                "ecosystem_type": "MANGROVE",
                "source_scope": "public_result_package",
                "needs_boundary_file": 1 if row.region_level in {"CITY", "PROTECTED_AREA", "SITE_AREA"} else 0,
                "is_simulated": 0,
                "notes": "Display dictionary generated from normalized public results.",
            }
    return pd.DataFrame(sorted(rows.values(), key=lambda item: item["region_id"]))


def build_data_sources(frames: Iterable[pd.DataFrame]) -> pd.DataFrame:
    combined = pd.concat([df for df in frames if not df.empty], ignore_index=True)
    rows = []
    for source_id_value, group in combined.groupby("source_id"):
        first = group.iloc[0]
        rows.append(
            {
                "source_id": source_id_value,
                "source_name": first["source_name"],
                "source_url": first["source_url"],
                "source_type": first["source_type"],
                "quality_level": sorted(group["quality_level"].dropna().astype(str).unique())[0],
                "is_simulated": int(group["is_simulated"].sum() > 0),
                "record_count": int(len(group)),
                "proxy_count": int(group["is_proxy"].sum()),
                "direct_count": int((group["is_proxy"] == 0).sum()),
                "locked_high_value_source": int(source_id_value in {
                    "SRC_LIT_FORESTS_HI_MANGROVE_RS_2023",
                    "SRC_LIT_HI_GHG_2022",
                    "SRC_LIT_BAMEN_SOC_2022",
                    "SRC_LIT_FRONTIERS_HI_LULC_INVEST_2024",
                }),
                "database_target": ";".join(sorted(group["database_target"].dropna().astype(str).unique())),
                "notes": "Normalized source summary for frontend filter and V2.0 ingest mapping.",
            }
        )
    return pd.DataFrame(rows).sort_values(["locked_high_value_source", "source_id"], ascending=[False, True])


def write_json(area: pd.DataFrame, remote: pd.DataFrame, literature: pd.DataFrame) -> None:
    area_rows = area[area["indicator_name"].isin(["mangrove_area", "remote_sensing_mangrove_area"])].copy()
    area_payload = {
        "dataset": "HNBLUE V2.0 public mangrove area timeseries",
        "is_simulated": 0,
        "regions": [],
    }
    for region_id, group in area_rows.sort_values(["region_name", "year_start"]).groupby("region_id"):
        first = group.iloc[0]
        area_payload["regions"].append(
            {
                "region_id": region_id,
                "region_name": first["region_name"],
                "unit": "ha",
                "points": [
                    {
                        "year": int(row.year_start),
                        "value": float(row.value),
                        "source_id": row.source_id,
                        "is_proxy": int(row.is_proxy),
                    }
                    for row in group.itertuples(index=False)
                    if pd.notna(row.year_start)
                ],
            }
        )

    remote_payload = {
        "dataset": "HNBLUE V2.0 public remote sensing metrics",
        "is_simulated": 0,
        "metrics": [
            {
                "record_id": row.record_id,
                "region_id": row.region_id,
                "region_name": row.region_name,
                "year_or_period": row.year_or_period,
                "indicator_code": row.indicator_code,
                "indicator_label": row.indicator_label,
                "value": float(row.value),
                "unit": row.unit,
                "is_proxy": int(row.is_proxy),
                "source_id": row.source_id,
            }
            for row in remote.sort_values(["display_priority", "indicator_group", "region_name", "year_start"]).itertuples(index=False)
        ],
    }

    lit_cards = literature.sort_values(["display_priority", "indicator_group", "region_name", "indicator_name"]).head(80)
    literature_payload = {
        "dataset": "HNBLUE V2.0 public literature carbon cards",
        "is_simulated": 0,
        "cards": [
            {
                "record_id": row.record_id,
                "title": f"{row.region_name} · {row.indicator_label}",
                "subtitle": row.source_name,
                "year_or_period": row.year_or_period,
                "value": float(row.value),
                "unit": row.unit,
                "quality_level": row.quality_level,
                "is_proxy": int(row.is_proxy),
                "source_url": row.source_url,
                "notes": row.notes,
            }
            for row in lit_cards.itertuples(index=False)
        ],
    }

    (NORM_DIR / "frontend_mangrove_area_timeseries.json").write_text(
        json.dumps(area_payload, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    (NORM_DIR / "frontend_remote_sensing_metrics.json").write_text(
        json.dumps(remote_payload, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    (NORM_DIR / "frontend_literature_carbon_cards.json").write_text(
        json.dumps(literature_payload, ensure_ascii=False, indent=2), encoding="utf-8"
    )


def stats(df: pd.DataFrame) -> dict[str, object]:
    numeric = pd.to_numeric(df["value"], errors="coerce") if "value" in df else pd.Series(dtype=float)
    years = pd.to_numeric(df["year_start"], errors="coerce") if "year_start" in df else pd.Series(dtype=float)
    return {
        "records": int(len(df)),
        "indicators": int(df["indicator_name"].nunique()) if "indicator_name" in df else 0,
        "regions": int(df["region_id"].nunique()) if "region_id" in df else 0,
        "year_min": None if years.dropna().empty else int(years.min()),
        "year_max": None if years.dropna().empty else int(years.max()),
        "non_null_numeric_values": int(numeric.notna().sum()),
        "simulated_sum": int(df["is_simulated"].sum()) if "is_simulated" in df else 0,
        "proxy_count": int(df["is_proxy"].sum()) if "is_proxy" in df else 0,
    }


def write_report(file_stats: dict[str, dict[str, object]], added_counts: dict[str, int], browser_blockers: list[str]) -> None:
    total_added = sum(added_counts.values())
    lines = [
        "# HNBLUE V2.0 Public Result Next Report",
        "",
        "## 本轮新增真实数值记录",
        "",
        f"- 新增派生/补充数值记录：{total_added} 条。",
        f"- 面积变化与八门湾组成补充：{added_counts.get('area', 0)} 条。",
        f"- 遥感/模型表格变化量补充：{added_counts.get('remote', 0)} 条。",
        f"- 文献碳储量公式派生补充：{added_counts.get('literature', 0)} 条。",
        "- 所有新增记录均来自已锁定公开来源的已发表数值或其直接差分、比例、公式计算，`is_simulated=0`。",
        "",
        "## Normalized 文件校验",
        "",
    ]
    for file_name, item in file_stats.items():
        lines.extend(
            [
                f"### `{file_name}`",
                f"- records: {item['records']}",
                f"- indicators: {item['indicators']}",
                f"- regions: {item['regions']}",
                f"- year range: {item['year_min']} - {item['year_max']}",
                f"- non-null numeric values: {item['non_null_numeric_values']}",
                f"- proxy count: {item['proxy_count']}",
                f"- simulated sum: {item['simulated_sum']}",
                "",
            ]
        )
    lines.extend(
        [
            "## 可直接用于 V2.0 前端展示",
            "",
            "- `display_mangrove_area_timeseries.csv` 与 `frontend_mangrove_area_timeseries.json`：海南岛 9 个红树林区域 1990-2020 面积时序，海南省 2010/2020 面积，八门湾 2022 面积与天然/人工组成。",
            "- `display_remote_sensing_metrics.csv` 与 `frontend_remote_sensing_metrics.json`：海南岛土地利用面积/比例、红树林面积、InVEST 碳储量与变化量。",
            "- `display_literature_carbon_records.csv` 与 `frontend_literature_carbon_cards.json`：SOC、AGB、BGB、总碳密度、GHG 清单 proxy、样本量等卡片数据。",
            "- `display_species_allometry_records.csv`：八门湾物种出现记录、海南/八门湾物种数、Komiyama 红树林异速方程参数、根冠比。",
            "",
            "## 只能作为 proxy 的数据",
            "",
            "- GHG 清单排放、N2O 水产养殖清单、InVEST 碳储量变化、GEE 脚本中的 MODIS GPP/NPP、CHIRPS、ERA5、DEM 指标均保持 `is_proxy=1`。",
            "- 未写入任何通量塔观测字段，未将区域均值拆分为样方、样木或土壤样品。",
            "",
            "## 适合后续入库",
            "",
            "- `display_data_sources.csv` -> `hainan_blue_carbon_core.t_data_source`。",
            "- `display_region_dictionary.csv` -> `hainan_blue_carbon_core.t_region` 的候选字典层。",
            "- 面积与遥感指标 -> `hainan_blue_carbon_satellite` 或统一外部观测事实表。",
            "- 文献碳储量与异速方程 -> `hainan_blue_carbon_intl.t_literature_carbon_record`、`t_allometry_equation`。",
            "",
            "## 仍需 GEE 或边界文件进一步计算",
            "",
            "- 文昌市和八门湾精确 GMW 1996/2007-2010/2015-2020 面积，需要行政边界或保护区边界 GeoJSON。",
            "- MODIS GPP/NPP、CHIRPS、ERA5、GEDI/NASADEM 区域统计，需要 GEE 认证并运行 `scripts/geospatial/gee_hnblue_public_metrics.js`。",
            "- 当前浏览器对锁定论文域名访问被策略拒绝，本轮未通过网页继续抽表。",
        ]
    )
    if browser_blockers:
        lines.extend(["", "## 浏览器阻塞证据", ""])
        lines.extend([f"- {item}" for item in browser_blockers])
    REPORT_PATH.write_text("\n".join(lines), encoding="utf-8")


def main() -> None:
    NORM_DIR.mkdir(parents=True, exist_ok=True)
    REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)

    area, remote, literature = read_inputs()
    area_extra = clean_base(supplement_area(area))
    remote_extra = clean_base(supplement_remote(remote))
    literature_extra = clean_base(supplement_literature(literature))

    area_all = pd.concat([area, area_extra], ignore_index=True).drop_duplicates(BASE_COLUMNS)
    remote_all = pd.concat([remote, remote_extra], ignore_index=True).drop_duplicates(BASE_COLUMNS)
    literature_all = pd.concat([literature, literature_extra], ignore_index=True).drop_duplicates(BASE_COLUMNS)

    species_base = split_species_allometry(literature_all)
    literature_carbon_base = literature_all.drop(species_base.index).reset_index(drop=True)
    species_base = species_base.reset_index(drop=True)

    display_area = to_display(area_all, "AREA", "satellite.public_area_observation", "mangrove_area")
    display_remote = to_display(remote_all, "RS", "satellite.public_remote_sensing_metric", "remote_sensing")
    display_literature = to_display(literature_carbon_base, "LIT", "intl.literature_carbon_record", "literature_carbon")
    display_species = to_display(species_base, "SPAL", "intl.species_allometry_record", "species_allometry")

    display_regions = build_region_dictionary([display_area, display_remote, display_literature, display_species])
    display_sources = build_data_sources([display_area, display_remote, display_literature, display_species])

    outputs = {
        "display_mangrove_area_timeseries.csv": display_area,
        "display_remote_sensing_metrics.csv": display_remote,
        "display_literature_carbon_records.csv": display_literature,
        "display_species_allometry_records.csv": display_species,
        "display_data_sources.csv": display_sources,
        "display_region_dictionary.csv": display_regions,
    }
    for name, df in outputs.items():
        df.to_csv(NORM_DIR / name, index=False, encoding="utf-8")

    write_json(display_area, display_remote, display_literature)

    file_stats = {name: stats(df) for name, df in outputs.items() if "value" in df.columns}
    added_counts = {
        "area": int(len(area_extra)),
        "remote": int(len(remote_extra)),
        "literature": int(len(literature_extra)),
    }
    write_report(
        file_stats,
        added_counts,
        [
            "Browser opening https://www.mdpi.com/1999-4907/14/11/2217 was rejected by browser security policy.",
            "Browser opening https://www.frontiersin.org/journals/marine-science/articles/10.3389/fmars.2022.932984/full was rejected by browser security policy.",
        ],
    )

    for name, item in file_stats.items():
        print(
            f"{name}: records={item['records']}, indicators={item['indicators']}, "
            f"regions={item['regions']}, year_range={item['year_min']}-{item['year_max']}, "
            f"non_null_numeric_values={item['non_null_numeric_values']}, "
            f"proxy_count={item['proxy_count']}, simulated_sum={item['simulated_sum']}"
        )
    print(f"added_numeric_records={sum(added_counts.values())} {added_counts}")


if __name__ == "__main__":
    main()
