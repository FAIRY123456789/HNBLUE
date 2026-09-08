from __future__ import annotations

from pathlib import Path

import pandas as pd


ROOT = Path(__file__).resolve().parents[2]
NORM_DIR = ROOT / "data" / "results" / "hnblue_v2_public" / "normalized"

FILES = [
    "display_mangrove_area_timeseries.csv",
    "display_remote_sensing_metrics.csv",
    "display_literature_carbon_records.csv",
    "display_species_allometry_records.csv",
    "display_data_sources.csv",
    "display_region_dictionary.csv",
]


def summarize(path: Path) -> dict[str, object]:
    df = pd.read_csv(path, encoding="utf-8")
    numeric_count = int(pd.to_numeric(df["value"], errors="coerce").notna().sum()) if "value" in df else 0
    indicator_count = int(df["indicator_name"].nunique()) if "indicator_name" in df else 0
    region_count = int(df["region_id"].nunique()) if "region_id" in df else 0
    if "year_start" in df:
        years = pd.to_numeric(df["year_start"], errors="coerce")
        year_min = None if years.dropna().empty else int(years.min())
        year_max = None if years.dropna().empty else int(years.max())
    else:
        year_min = None
        year_max = None
    simulated_sum = int(pd.to_numeric(df["is_simulated"], errors="coerce").fillna(0).sum()) if "is_simulated" in df else 0
    proxy_count = int(pd.to_numeric(df["is_proxy"], errors="coerce").fillna(0).sum()) if "is_proxy" in df else 0
    return {
        "file": path.name,
        "records": int(len(df)),
        "indicators": indicator_count,
        "regions": region_count,
        "year_min": year_min,
        "year_max": year_max,
        "non_null_numeric_values": numeric_count,
        "proxy_count": proxy_count,
        "simulated_sum": simulated_sum,
    }


def main() -> None:
    for name in FILES:
        summary = summarize(NORM_DIR / name)
        print(
            f"{summary['file']}: records={summary['records']}, "
            f"indicators={summary['indicators']}, regions={summary['regions']}, "
            f"year_range={summary['year_min']}-{summary['year_max']}, "
            f"non_null_numeric_values={summary['non_null_numeric_values']}, "
            f"proxy_count={summary['proxy_count']}, simulated_sum={summary['simulated_sum']}"
        )
        if summary["simulated_sum"] != 0:
            raise ValueError(f"{name} contains simulated rows")


if __name__ == "__main__":
    main()
