from __future__ import annotations

from pathlib import Path

import pandas as pd


ROOT = Path(__file__).resolve().parents[2]
OUT_DIR = ROOT / "data" / "results" / "hnblue_v2_public"

REQUIRED_COLUMNS = [
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

FORESTS_2023_URL = "https://www.mdpi.com/1999-4907/14/11/2217"
FRONTIERS_2024_URL = (
    "https://www.frontiersin.org/journals/forests-and-global-change/"
    "articles/10.3389/ffgc.2024.1349057/full"
)
FRONTIERS_2022_URL = (
    "https://www.frontiersin.org/journals/marine-science/"
    "articles/10.3389/fmars.2022.932984/full"
)
WATER_2022_URL = "https://www.mdpi.com/2073-4441/14/20/3278"


def record(
    source_name: str,
    source_url: str,
    source_type: str,
    region_name: str,
    year_or_period: str | int,
    indicator_name: str,
    value: float,
    unit: str,
    method: str,
    quality_level: str = "A",
    is_proxy: int = 0,
    is_simulated: int = 0,
    notes: str = "",
) -> dict[str, object]:
    return {
        "source_name": source_name,
        "source_url": source_url,
        "source_type": source_type,
        "region_name": region_name,
        "year_or_period": str(year_or_period),
        "indicator_name": indicator_name,
        "value": float(value),
        "unit": unit,
        "method": method,
        "quality_level": quality_level,
        "is_proxy": int(is_proxy),
        "is_simulated": int(is_simulated),
        "notes": notes,
    }


def build_area_timeseries() -> pd.DataFrame:
    source_name = (
        "Using Medium-Resolution Remote Sensing Satellite Images to Evaluate "
        "Recent Changes and Future Development Trends of Mangrove Forests on "
        "Hainan Island, China"
    )
    site_values = {
        "Dongzhaigang": [1615, 1540, 1664, 1723, 1787, 1740, 1751],
        "Dongfang": [26, 38, 30, 60, 67, 77, 80],
        "Huiwen": [1055, 989, 817, 826, 869, 878, 904],
        "Guannan": [227, 191, 193, 174, 173, 156, 167],
        "Xinyinggang": [328, 301, 289, 230, 273, 290, 298],
        "Yangpugang": [747, 730, 498, 492, 609, 627, 682],
        "Huachangwan": [355, 300, 234, 220, 367, 334, 327],
        "Puqian": [170, 167, 147, 145, 160, 138, 141],
        "Maniaogang": [55, 30, 46, 50, 192, 112, 124],
    }
    years = [1990, 1995, 2000, 2005, 2010, 2015, 2020]

    rows: list[dict[str, object]] = []
    for site, values in site_values.items():
        for year, value in zip(years, values, strict=True):
            rows.append(
                record(
                    source_name=source_name,
                    source_url=FORESTS_2023_URL,
                    source_type="peer_reviewed_remote_sensing_table",
                    region_name=f"Hainan Island - {site}",
                    year_or_period=year,
                    indicator_name="mangrove_area",
                    value=value,
                    unit="ha",
                    method=(
                        "Published Table 3 mangrove area interpreted from "
                        "Landsat TM/ETM+/OLI medium-resolution satellite images."
                    ),
                    notes=(
                        "Site-level protected-area / growth-area value; keep as "
                        "regional remote-sensing statistic, not plot observation."
                    ),
                )
            )

    rows.extend(
        [
            record(
                source_name="Incorporating coastal blue carbon into subnational greenhouse gas inventories",
                source_url=FRONTIERS_2022_URL,
                source_type="peer_reviewed_inventory_table",
                region_name="Hainan Province",
                year_or_period=2010,
                indicator_name="mangrove_area",
                value=4190.7,
                unit="ha",
                method="Published 2010 mangrove area compiled from high-resolution Google Earth imagery.",
                notes="Province-level inventory area; compatible with display layer.",
            ),
            record(
                source_name="Incorporating coastal blue carbon into subnational greenhouse gas inventories",
                source_url=FRONTIERS_2022_URL,
                source_type="peer_reviewed_inventory_table",
                region_name="Hainan Province",
                year_or_period=2020,
                indicator_name="mangrove_area",
                value=4644.1,
                unit="ha",
                method="Published 2020 mangrove area compiled from official vectorized survey.",
                notes="Province-level inventory area; compatible with display layer.",
            ),
            record(
                source_name="Spatial Variation of Soil Organic Carbon from Bamen Bay Mangrove in Southern China",
                source_url=WATER_2022_URL,
                source_type="peer_reviewed_study_area_text",
                region_name="Wenchang / Bamen Bay Nature Reserve",
                year_or_period=2022,
                indicator_name="mangrove_area",
                value=1223.3,
                unit="hm2",
                method="Published study-area description for Bamen Bay Nature Reserve.",
                notes="Bamen Bay baseline area; same numeric magnitude as hectare because 1 hm2 = 1 ha.",
            ),
            record(
                source_name="Spatial Variation of Soil Organic Carbon from Bamen Bay Mangrove in Southern China",
                source_url=WATER_2022_URL,
                source_type="peer_reviewed_study_area_text",
                region_name="Wenchang / Bamen Bay Nature Reserve",
                year_or_period=2022,
                indicator_name="protected_area_total_area",
                value=2948.0,
                unit="hm2",
                method="Published study-area description for Bamen Bay Nature Reserve.",
                notes="Reserve extent context, retained separately from mangrove area.",
            ),
        ]
    )
    return pd.DataFrame(rows, columns=REQUIRED_COLUMNS)


def build_remote_sensing_metrics(area_df: pd.DataFrame) -> pd.DataFrame:
    rows: list[dict[str, object]] = []

    for _, src in area_df.iterrows():
        if src["source_type"] == "peer_reviewed_remote_sensing_table":
            item = src.to_dict()
            item["indicator_name"] = "remote_sensing_mangrove_area"
            rows.append(item)

    lulc_area = {
        2000: {
            "cropland_area": 9008.65,
            "forestland_area": 21819.14,
            "grassland_area": 1234.45,
            "water_land_area": 1281.41,
            "built_land_area": 785.87,
            "unused_land_area": 141.25,
        },
        2010: {
            "cropland_area": 8891.93,
            "forestland_area": 21742.29,
            "grassland_area": 1135.91,
            "water_land_area": 1507.49,
            "built_land_area": 907.81,
            "unused_land_area": 93.54,
        },
        2020: {
            "cropland_area": 8701.35,
            "forestland_area": 21465.89,
            "grassland_area": 1155.04,
            "water_land_area": 1476.57,
            "built_land_area": 1391.35,
            "unused_land_area": 89.45,
        },
    }
    lulc_prop = {
        2000: {
            "cropland_proportion": 26.29,
            "forestland_proportion": 63.67,
            "grassland_proportion": 3.60,
            "water_land_proportion": 3.74,
            "built_land_proportion": 2.29,
            "unused_land_proportion": 0.41,
        },
        2010: {
            "cropland_proportion": 25.94,
            "forestland_proportion": 63.43,
            "grassland_proportion": 3.31,
            "water_land_proportion": 4.40,
            "built_land_proportion": 2.65,
            "unused_land_proportion": 0.27,
        },
        2020: {
            "cropland_proportion": 25.38,
            "forestland_proportion": 62.62,
            "grassland_proportion": 3.37,
            "water_land_proportion": 4.31,
            "built_land_proportion": 4.06,
            "unused_land_proportion": 0.26,
        },
    }
    for year, indicators in lulc_area.items():
        for indicator, value in indicators.items():
            rows.append(
                record(
                    source_name=(
                        "The impact of land use change on carbon storage and multi-scenario "
                        "prediction in Hainan Island using InVEST and CA-Markov models"
                    ),
                    source_url=FRONTIERS_2024_URL,
                    source_type="peer_reviewed_remote_sensing_model_table",
                    region_name="Hainan Island",
                    year_or_period=year,
                    indicator_name=indicator,
                    value=value,
                    unit="km2",
                    method="Published Table 4 land-use area derived from remote-sensing land-use maps.",
                    notes="Regional LULC covariate for blue-carbon model context.",
                )
            )
    for year, indicators in lulc_prop.items():
        for indicator, value in indicators.items():
            rows.append(
                record(
                    source_name=(
                        "The impact of land use change on carbon storage and multi-scenario "
                        "prediction in Hainan Island using InVEST and CA-Markov models"
                    ),
                    source_url=FRONTIERS_2024_URL,
                    source_type="peer_reviewed_remote_sensing_model_table",
                    region_name="Hainan Island",
                    year_or_period=year,
                    indicator_name=indicator,
                    value=value,
                    unit="percent",
                    method="Published Table 4 land-use proportion derived from remote-sensing land-use maps.",
                    notes="Regional LULC covariate for blue-carbon model context.",
                )
            )

    carbon_metrics = {
        2000: {
            "aboveground_biomass_carbon_pool": 84.31,
            "belowground_biomass_carbon_pool": 19.46,
            "soil_carbon_pool": 331.53,
            "dead_organic_carbon_pool": 8.07,
            "total_carbon_storage": 443.37,
            "carbon_density": 129.37,
        },
        2010: {
            "aboveground_biomass_carbon_pool": 83.93,
            "belowground_biomass_carbon_pool": 19.30,
            "soil_carbon_pool": 330.36,
            "dead_organic_carbon_pool": 7.99,
            "total_carbon_storage": 441.59,
            "carbon_density": 128.82,
        },
        2020: {
            "aboveground_biomass_carbon_pool": 83.07,
            "belowground_biomass_carbon_pool": 19.13,
            "soil_carbon_pool": 327.39,
            "dead_organic_carbon_pool": 7.89,
            "total_carbon_storage": 437.47,
            "carbon_density": 127.62,
        },
    }
    for year, indicators in carbon_metrics.items():
        for indicator, value in indicators.items():
            unit = "Mg/hm2" if indicator == "carbon_density" else "Tg"
            rows.append(
                record(
                    source_name=(
                        "The impact of land use change on carbon storage and multi-scenario "
                        "prediction in Hainan Island using InVEST and CA-Markov models"
                    ),
                    source_url=FRONTIERS_2024_URL,
                    source_type="peer_reviewed_remote_sensing_model_table",
                    region_name="Hainan Island",
                    year_or_period=year,
                    indicator_name=indicator,
                    value=value,
                    unit=unit,
                    method="Published Table 5 InVEST carbon result driven by 2000/2010/2020 land-use maps.",
                    is_proxy=1,
                    notes="Model-derived regional carbon proxy; not field plot or flux observation.",
                )
            )

    return pd.DataFrame(rows, columns=REQUIRED_COLUMNS)


def source_type(row: pd.Series) -> str:
    url = str(row.get("source_url", ""))
    data_type = str(row.get("data_type", ""))
    if "frontiersin.org" in url:
        return "peer_reviewed_literature"
    if "mdpi.com" in url:
        return "peer_reviewed_literature"
    if "hainan.gov.cn" in url:
        return "government_public_page"
    if "springer.com" in url:
        return "peer_reviewed_literature"
    if "allometry" in data_type:
        return "allometry_method_literature"
    return "public_numeric_record"


def normalize_literature_frame(path: Path) -> pd.DataFrame:
    df = pd.read_csv(path, encoding="utf-8")
    df["value"] = pd.to_numeric(df["value"], errors="coerce")
    df = df[df["value"].notna()]
    df = df[df.get("is_simulated", 0).fillna(0).astype(int) == 0]
    if "data_type" in df:
        df = df[~df["data_type"].astype(str).str.contains("dataset_metadata|field_presence", na=False)]

    out = pd.DataFrame(
        {
            "source_name": df["source_name"].fillna(""),
            "source_url": df["source_url"].fillna(""),
            "source_type": df.apply(source_type, axis=1),
            "region_name": df["region_name"].fillna(df.get("site_name", "")).fillna(""),
            "year_or_period": df["year_or_period"].astype(str),
            "indicator_name": df["indicator_name"].astype(str),
            "value": df["value"].astype(float),
            "unit": df["unit"].fillna("").astype(str),
            "method": (
                df.get("extraction_method", "").fillna("").astype(str)
                + "; "
                + df.get("table_or_figure", "").fillna("").astype(str)
                + "; "
                + df.get("conversion_rule", "").fillna("").astype(str)
            ),
            "quality_level": df["quality_level"].fillna("B").astype(str),
            "is_proxy": df["is_proxy"].fillna(0).astype(int),
            "is_simulated": df["is_simulated"].fillna(0).astype(int),
            "notes": df["notes"].fillna("").astype(str),
        }
    )
    return out[REQUIRED_COLUMNS]


def build_literature_records() -> pd.DataFrame:
    frames = [
        normalize_literature_frame(ROOT / "data" / "staging" / "literature" / "literature_carbon_records.csv"),
        normalize_literature_frame(ROOT / "data" / "staging" / "species" / "species_and_allometry_records.csv"),
        normalize_literature_frame(ROOT / "data" / "staging" / "flux" / "flux_or_proxy_candidate_records.csv"),
    ]
    df = pd.concat(frames, ignore_index=True)
    df = df[df["source_url"].astype(str).str.len() > 0]
    df = df[df["source_name"].astype(str).str.len() > 0]
    df = df[df["is_simulated"].astype(int) == 0]
    return df.drop_duplicates().reset_index(drop=True)


def validate_file(path: Path) -> dict[str, object]:
    df = pd.read_csv(path, encoding="utf-8")
    numeric = pd.to_numeric(df["value"], errors="coerce")
    years = pd.to_numeric(df["year_or_period"].astype(str).str.extract(r"(\d{4})")[0], errors="coerce")
    return {
        "file": path.name,
        "records": int(len(df)),
        "columns": list(df.columns),
        "non_null_numeric_values": int(numeric.notna().sum()),
        "regions": int(df["region_name"].nunique(dropna=True)),
        "indicators": int(df["indicator_name"].nunique(dropna=True)),
        "year_min": None if years.dropna().empty else int(years.min()),
        "year_max": None if years.dropna().empty else int(years.max()),
        "simulated_sum": int(df["is_simulated"].sum()),
    }


def write_manifest(stats: list[dict[str, object]]) -> None:
    lines = [
        "# HNBLUE V2.0 Public Numeric Result Manifest",
        "",
        "Generated by `scripts/geospatial/build_hnblue_v2_public_results.py`.",
        "",
        "## Result Files",
        "",
    ]
    for item in stats:
        direct = "yes"
        proxy = "mixed"
        review = "yes"
        if item["file"] == "region_mangrove_area_timeseries.csv":
            proxy = "no"
            review = "boundary names and table lineage only"
        elif item["file"] == "region_remote_sensing_metrics.csv":
            direct = "yes for display; model-carbon rows are proxy"
            proxy = "mixed"
            review = "yes before database fact-table typing"
        elif item["file"] == "literature_carbon_numeric_records.csv":
            direct = "yes for curated numeric display"
            proxy = "mixed; GHG inventory rows are proxy"
            review = "yes for government-page unit ambiguity rows"
        lines.extend(
            [
                f"### `{item['file']}`",
                f"- records: {item['records']}",
                f"- indicators: {item['indicators']}",
                f"- regions: {item['regions']}",
                f"- year range: {item['year_min']} - {item['year_max']}",
                f"- non-null numeric values: {item['non_null_numeric_values']}",
                f"- simulated rows: {item['simulated_sum']}",
                f"- can enter V2.0 database: {direct}",
                f"- proxy status: {proxy}",
                f"- manual review: {review}",
                "",
            ]
        )

    lines.extend(
        [
            "## Source And Method Notes",
            "",
            "- `region_mangrove_area_timeseries.csv`: uses MDPI Forests 2023 Table 3 site-level Hainan mangrove areas for 1990, 1995, 2000, 2005, 2010, 2015 and 2020; adds Hainan 2010/2020 inventory areas from Frontiers 2022 and Bamen Bay 2022 reserve/mangrove area from MDPI Water 2022.",
            "- `region_remote_sensing_metrics.csv`: uses the same MDPI Forests 2023 remote-sensing mangrove areas; adds Frontiers 2024 Table 4 Hainan land-use area/proportion and Table 5 InVEST carbon results. InVEST carbon rows are marked `is_proxy=1`.",
            "- `literature_carbon_numeric_records.csv`: filters existing staging literature/species/flux CSVs to numeric, non-simulated, source-traceable records. Product metadata and field-presence rows are excluded.",
            "",
            "## Blockers And Attempts",
            "",
            "- Earth Engine Python API probe: `import ee` returned unavailable in the local Python environment.",
            "- NASA POWER API probe command: `python -c \"import requests; ... requests.get('https://power.larc.nasa.gov/...')\"` failed in the sandbox with `PermissionError: [WinError 10013]`.",
            "- Escalated network retry for the NASA POWER API request timed out in automatic approval review. No API-derived weather value was written to the CSVs.",
            "- GEE Code Editor asset prepared at `scripts/geospatial/gee_hnblue_public_metrics.js` for authenticated execution.",
            "",
            "## Excluded Records",
            "",
            "- Remote-sensing product candidates with only product name, URL, spatial resolution or plan text were excluded from result CSVs.",
            "- Rows with missing numeric `value`, source URL or source name were excluded.",
            "- BAAD/Tallo field-presence rows were excluded because they describe schema availability, not a numeric ecological result.",
            "",
        ]
    )
    (OUT_DIR / "data_result_manifest.md").write_text("\n".join(lines), encoding="utf-8")


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    area = build_area_timeseries()
    remote = build_remote_sensing_metrics(area)
    literature = build_literature_records()

    outputs = {
        "region_mangrove_area_timeseries.csv": area,
        "region_remote_sensing_metrics.csv": remote,
        "literature_carbon_numeric_records.csv": literature,
    }
    for name, df in outputs.items():
        df = df[REQUIRED_COLUMNS].copy()
        df["value"] = pd.to_numeric(df["value"], errors="raise")
        if int(df["is_simulated"].sum()) != 0:
            raise ValueError(f"{name} contains simulated rows")
        df.to_csv(OUT_DIR / name, index=False, encoding="utf-8")

    stats = [validate_file(OUT_DIR / name) for name in outputs]
    write_manifest(stats)
    for item in stats:
        print(
            f"{item['file']}: records={item['records']}, "
            f"columns={item['columns']}, non_null_numeric_values={item['non_null_numeric_values']}, "
            f"regions={item['regions']}, indicators={item['indicators']}, "
            f"year_range={item['year_min']}-{item['year_max']}, simulated_sum={item['simulated_sum']}"
        )


if __name__ == "__main__":
    main()
