from __future__ import annotations

from pathlib import Path

import pandas as pd


ROOT = Path(__file__).resolve().parents[2]
RESULT_DIR = ROOT / "data" / "results" / "hnblue_v2_public"
FILES = [
    "region_mangrove_area_timeseries.csv",
    "region_remote_sensing_metrics.csv",
    "literature_carbon_numeric_records.csv",
]
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


def summarize(path: Path) -> dict[str, object]:
    df = pd.read_csv(path, encoding="utf-8")
    missing = [column for column in REQUIRED_COLUMNS if column not in df.columns]
    if missing:
        raise ValueError(f"{path.name} missing required columns: {missing}")
    numeric = pd.to_numeric(df["value"], errors="coerce")
    years = pd.to_numeric(df["year_or_period"].astype(str).str.extract(r"(\d{4})")[0], errors="coerce")
    return {
        "file": path.name,
        "records": len(df),
        "fields": list(df.columns),
        "non_null_numeric_values": int(numeric.notna().sum()),
        "regions": int(df["region_name"].nunique(dropna=True)),
        "indicators": int(df["indicator_name"].nunique(dropna=True)),
        "year_min": None if years.dropna().empty else int(years.min()),
        "year_max": None if years.dropna().empty else int(years.max()),
        "is_simulated_sum": int(pd.to_numeric(df["is_simulated"], errors="coerce").fillna(0).sum()),
    }


def main() -> None:
    for name in FILES:
        summary = summarize(RESULT_DIR / name)
        print(f"\n{name}")
        print(f"records: {summary['records']}")
        print(f"fields: {summary['fields']}")
        print(f"non_null_numeric_values: {summary['non_null_numeric_values']}")
        print(f"regions: {summary['regions']}")
        print(f"indicators: {summary['indicators']}")
        print(f"year_range: {summary['year_min']}-{summary['year_max']}")
        print(f"is_simulated_sum: {summary['is_simulated_sum']}")


if __name__ == "__main__":
    main()
