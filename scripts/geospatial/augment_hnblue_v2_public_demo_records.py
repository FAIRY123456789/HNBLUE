from __future__ import annotations

import json
from pathlib import Path

import pandas as pd


ROOT = Path(__file__).resolve().parents[2]
NORM_DIR = ROOT / "data" / "results" / "hnblue_v2_public" / "normalized"

AREA_CSV = NORM_DIR / "display_mangrove_area_timeseries.csv"
AREA_JSON = NORM_DIR / "frontend_mangrove_area_timeseries.json"
SOURCES_CSV = NORM_DIR / "display_data_sources.csv"

SUPPLEMENT_SOURCE_ID = "SRC_LIT_FORESTS_HI_MANGROVE_RS_2023"
SUPPLEMENT_PREFIX = "DEMOAREA"


def next_record_id(index: int) -> str:
    return f"{SUPPLEMENT_PREFIX}-{index:05d}"


def base_row(template: pd.Series, index: int, indicator_name: str, indicator_label: str, value: float, unit: str) -> dict[str, object]:
    return {
        "record_id": next_record_id(index),
        "source_id": SUPPLEMENT_SOURCE_ID,
        "source_name": template["source_name"],
        "source_url": template["source_url"],
        "source_type": "derived_from_peer_reviewed_remote_sensing_table",
        "region_id": template["region_id"],
        "region_name": template["region_name"],
        "region_level": template["region_level"],
        "year_or_period": "2020",
        "year_start": 2020,
        "year_end": 2020,
        "indicator_code": indicator_name.upper(),
        "indicator_name": indicator_name,
        "indicator_label": indicator_label,
        "indicator_group": "area_boundary",
        "value": float(value),
        "unit": unit,
        "method": "Derived from MDPI Forests 2023 Table 3 site-level 1990-2020 mangrove area values.",
        "quality_level": "A",
        "is_proxy": 0,
        "is_simulated": 0,
        "database_target": "satellite.public_area_observation",
        "frontend_group": "mangrove_area",
        "display_priority": 12,
        "notes": "Demo supplement metric for frontend ranking and composition display; not a field plot.",
    }


def build_supplement(area: pd.DataFrame) -> pd.DataFrame:
    base = area[
        (area["source_id"] == SUPPLEMENT_SOURCE_ID)
        & (area["indicator_name"] == "mangrove_area")
        & (area["year_start"].isin([1990, 2020]))
    ].copy()
    pivot = base.pivot_table(index=["region_id", "region_name"], columns="year_start", values="value", aggfunc="first")
    pivot = pivot.dropna(subset=[1990, 2020])
    total_2020 = float(pivot[2020].sum())
    rank_map = pivot[2020].rank(method="dense", ascending=False).astype(int).to_dict()

    rows: list[dict[str, object]] = []
    row_index = 1
    for region_id, region_name in pivot.index:
        template = base[(base["region_id"] == region_id) & (base["year_start"] == 2020)].iloc[0]
        area_1990 = float(pivot.loc[(region_id, region_name), 1990])
        area_2020 = float(pivot.loc[(region_id, region_name), 2020])
        share = 100.0 * area_2020 / total_2020
        index_1990 = 100.0 * area_2020 / area_1990
        rank = float(rank_map[(region_id, region_name)])

        rows.append(base_row(template, row_index, "mangrove_area_2020_share_of_tracked_sites", "2020 跟踪区域面积占比", share, "percent"))
        row_index += 1
        rows.append(base_row(template, row_index, "mangrove_area_2020_rank_among_tracked_sites", "2020 跟踪区域面积排名", rank, "rank"))
        row_index += 1
        rows.append(base_row(template, row_index, "mangrove_area_2020_index_1990_100", "2020 面积指数(1990=100)", index_1990, "index"))
        row_index += 1

    return pd.DataFrame(rows, columns=area.columns)


def rewrite_area_json(area: pd.DataFrame) -> None:
    timeseries = area[area["indicator_name"].isin(["mangrove_area", "remote_sensing_mangrove_area"])].copy()
    supplement = area[area["record_id"].astype(str).str.startswith(SUPPLEMENT_PREFIX)].copy()
    payload = {
        "dataset": "HNBLUE V2.0 public mangrove area timeseries",
        "is_simulated": 0,
        "regions": [],
        "supplement_metrics": [],
    }
    for region_id, group in timeseries.sort_values(["region_name", "year_start"]).groupby("region_id"):
        first = group.iloc[0]
        payload["regions"].append(
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
    for row in supplement.sort_values(["indicator_name", "value"], ascending=[True, False]).itertuples(index=False):
        payload["supplement_metrics"].append(
            {
                "record_id": row.record_id,
                "region_id": row.region_id,
                "region_name": row.region_name,
                "indicator_name": row.indicator_name,
                "indicator_label": row.indicator_label,
                "value": float(row.value),
                "unit": row.unit,
                "source_id": row.source_id,
                "is_proxy": int(row.is_proxy),
            }
        )
    AREA_JSON.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def refresh_source_counts() -> None:
    sources = pd.read_csv(SOURCES_CSV, encoding="utf-8")
    fact_files = [
        "display_mangrove_area_timeseries.csv",
        "display_remote_sensing_metrics.csv",
        "display_literature_carbon_records.csv",
        "display_species_allometry_records.csv",
    ]
    facts = pd.concat(
        [pd.read_csv(NORM_DIR / name, encoding="utf-8") for name in fact_files],
        ignore_index=True,
    )
    grouped = facts.groupby("source_id").agg(
        record_count=("record_id", "count"),
        proxy_count=("is_proxy", "sum"),
    )
    grouped["direct_count"] = grouped["record_count"] - grouped["proxy_count"]
    for source_id, row in grouped.iterrows():
        mask = sources["source_id"] == source_id
        if mask.any():
            sources.loc[mask, "record_count"] = int(row["record_count"])
            sources.loc[mask, "proxy_count"] = int(row["proxy_count"])
            sources.loc[mask, "direct_count"] = int(row["direct_count"])
    sources.to_csv(SOURCES_CSV, index=False, encoding="utf-8")


def main() -> None:
    area = pd.read_csv(AREA_CSV, encoding="utf-8")
    area = area[~area["record_id"].astype(str).str.startswith(SUPPLEMENT_PREFIX)].copy()
    supplement = build_supplement(area)
    updated = pd.concat([area, supplement], ignore_index=True)
    if int(updated["is_simulated"].sum()) != 0:
        raise ValueError("Supplement produced simulated rows")
    updated.to_csv(AREA_CSV, index=False, encoding="utf-8")
    rewrite_area_json(updated)
    refresh_source_counts()
    print(f"added_demo_numeric_records={len(supplement)}")
    print(f"area_records={len(updated)}")
    print(f"area_regions={updated['region_id'].nunique()}")
    print(f"area_indicators={updated['indicator_name'].nunique()}")
    print(f"area_proxy_count={int(updated['is_proxy'].sum())}")


if __name__ == "__main__":
    main()
