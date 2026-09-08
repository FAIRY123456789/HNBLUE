"""Sync HNBLUE V2.0 normalized public data into Vue public assets."""

from __future__ import annotations

import csv
import hashlib
import json
import shutil
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SOURCE_DIR = ROOT / "data" / "results" / "hnblue_v2_public" / "normalized"
TARGET_DIR = ROOT / "frontend" / "public" / "data" / "hnblue_v2_public" / "normalized"

SYNC_FILES = [
    "frontend_mangrove_area_timeseries.json",
    "frontend_remote_sensing_metrics.json",
    "frontend_literature_carbon_cards.json",
    "display_mangrove_area_timeseries.csv",
    "display_remote_sensing_metrics.csv",
    "display_literature_carbon_records.csv",
    "display_species_allometry_records.csv",
    "display_data_sources.csv",
    "display_region_dictionary.csv",
]


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def count_rows(path: Path) -> int | None:
    if path.suffix.lower() == ".csv":
        with path.open("r", encoding="utf-8-sig", newline="") as handle:
            return max(sum(1 for _ in csv.reader(handle)) - 1, 0)
    if path.suffix.lower() == ".json":
        data = json.loads(path.read_text(encoding="utf-8"))
        if "regions" in data:
            return len(data["regions"])
        if "metrics" in data:
            return len(data["metrics"])
        if "cards" in data:
            return len(data["cards"])
        if isinstance(data, list):
            return len(data)
    return None


def main() -> None:
    if not SOURCE_DIR.exists():
        raise FileNotFoundError(f"Source directory not found: {SOURCE_DIR}")

    TARGET_DIR.mkdir(parents=True, exist_ok=True)
    manifest = {
        "dataset": "HNBLUE V2.0 public frontend data sync",
        "synced_at_utc": datetime.now(timezone.utc).isoformat(),
        "source_dir": str(SOURCE_DIR.relative_to(ROOT)),
        "target_dir": str(TARGET_DIR.relative_to(ROOT)),
        "is_simulated_required": 0,
        "files": [],
    }

    for name in SYNC_FILES:
        source = SOURCE_DIR / name
        target = TARGET_DIR / name
        if not source.exists():
            raise FileNotFoundError(f"Required source file not found: {source}")
        shutil.copy2(source, target)
        manifest["files"].append(
            {
                "name": name,
                "bytes": target.stat().st_size,
                "sha256": sha256(target),
                "row_count": count_rows(target),
            }
        )

    manifest_path = TARGET_DIR / "sync_manifest.json"
    manifest_path.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(manifest, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
