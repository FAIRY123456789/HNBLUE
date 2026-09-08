from __future__ import annotations

import json
from pathlib import Path

import pandas as pd


ROOT = Path(__file__).resolve().parents[2]
NORM_DIR = ROOT / "data" / "results" / "hnblue_v2_public" / "normalized"


def csv_summary(name: str) -> dict[str, int | str]:
    df = pd.read_csv(NORM_DIR / name, encoding="utf-8")
    return {
        "file": name,
        "records": int(len(df)),
        "regions": int(df["region_id"].nunique()) if "region_id" in df else 0,
        "indicators": int(df["indicator_name"].nunique()) if "indicator_name" in df else 0,
        "proxy_count": int(pd.to_numeric(df["is_proxy"], errors="coerce").fillna(0).sum()) if "is_proxy" in df else 0,
        "simulated_sum": int(pd.to_numeric(df["is_simulated"], errors="coerce").fillna(0).sum()) if "is_simulated" in df else 0,
    }


def json_summary(name: str) -> dict[str, int | str]:
    data = json.loads((NORM_DIR / name).read_text(encoding="utf-8"))
    if "regions" in data:
        records = sum(len(region.get("points", [])) for region in data["regions"]) + len(data.get("supplement_metrics", []))
        regions = len(data["regions"])
        indicators = 1 + len({item["indicator_name"] for item in data.get("supplement_metrics", [])})
        proxy_count = sum(
            int(point.get("is_proxy", 0))
            for region in data["regions"]
            for point in region.get("points", [])
        ) + sum(int(item.get("is_proxy", 0)) for item in data.get("supplement_metrics", []))
    elif "metrics" in data:
        records = len(data["metrics"])
        regions = len({item["region_id"] for item in data["metrics"]})
        indicators = len({item["indicator_code"] for item in data["metrics"]})
        proxy_count = sum(int(item.get("is_proxy", 0)) for item in data["metrics"])
    else:
        records = len(data["cards"])
        regions = len({item["title"].split(" · ", 1)[0] for item in data["cards"]})
        indicators = len({item["title"].split(" · ", 1)[-1] for item in data["cards"]})
        proxy_count = sum(int(item.get("is_proxy", 0)) for item in data["cards"])
    return {
        "file": name,
        "records": int(records),
        "regions": int(regions),
        "indicators": int(indicators),
        "proxy_count": int(proxy_count),
        "simulated_sum": int(data.get("is_simulated", 0)),
    }


def main() -> None:
    files = [
        "display_mangrove_area_timeseries.csv",
        "display_remote_sensing_metrics.csv",
        "display_literature_carbon_records.csv",
        "display_species_allometry_records.csv",
        "display_data_sources.csv",
        "frontend_mangrove_area_timeseries.json",
        "frontend_remote_sensing_metrics.json",
        "frontend_literature_carbon_cards.json",
    ]
    for name in files:
        summary = json_summary(name) if name.endswith(".json") else csv_summary(name)
        print(
            f"{summary['file']}: records={summary['records']}, regions={summary['regions']}, "
            f"indicators={summary['indicators']}, proxy_count={summary['proxy_count']}, "
            f"simulated_sum={summary['simulated_sum']}"
        )
        if summary["simulated_sum"] != 0:
            raise ValueError(f"{name} contains simulated rows")


if __name__ == "__main__":
    main()
