"""Profile the external-dataset CSV files without loading them into a browser.

The script is deliberately dependency-free and streams every file. It records
shape, encoding, delimiter, missingness, duplicate rows, inferred field roles,
and a SHA-256 digest so the audit can be reproduced after source updates.
"""

from __future__ import annotations

import csv
import hashlib
import json
import math
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "data" / "processed" / "external_dataset_file_audit.json"
FILES = {
    "BAAD": [ROOT / "data/raw/baad/BAAD_cleaned.csv"],
    "Tallo": [ROOT / "data/raw/tallo/Tallo.csv"],
    "ChinAllomeTree": [
        ROOT / "data/raw/chinallometree/ChinAllomeTree_cleaned.csv",
        ROOT / "data/raw/chinallometree/ChinAllomeTree.xlsx - General.csv",
        ROOT / "data/raw/chinallometree/ChinAllomeTree.xlsx - Equation.csv",
    ],
    "GWM": sorted((ROOT / "data/raw/gwm").glob("*.csv")),
}

MISSING = {"", "na", "n/a", "null", "none", "nan", "-9999"}
GEO_TOKENS = ("lat", "lon", "country", "region", "province", "location", "site", "plot", "biome")
UNIT_TOKENS = ("unit", "_kg", "_g", "_m", "_cm", "_mm", "_ha", "biomass", "diameter", "height", "area")


def detect_encoding(path: Path) -> str:
    sample = path.read_bytes()[:262144]
    for encoding in ("utf-8-sig", "utf-8", "gb18030", "cp1252"):
        try:
            sample.decode(encoding)
            return encoding
        except UnicodeDecodeError:
            continue
    return "latin-1"


def detect_delimiter(path: Path, encoding: str) -> str:
    with path.open("r", encoding=encoding, newline="", errors="replace") as handle:
        sample = handle.read(65536)
    try:
        return csv.Sniffer().sniff(sample, delimiters=",\t;|").delimiter
    except csv.Error:
        return ","


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def is_number(value: str) -> bool:
    try:
        number = float(value)
        return math.isfinite(number)
    except (TypeError, ValueError):
        return False


def profile(path: Path, dataset: str) -> dict:
    encoding = detect_encoding(path)
    delimiter = detect_delimiter(path, encoding)
    row_count = 0
    malformed_rows = 0
    duplicate_rows = 0
    seen_hashes: set[bytes] = set()
    missing_counts: list[int] = []
    non_missing_counts: list[int] = []
    numeric_counts: list[int] = []
    distinct_samples: list[set[str]] = []

    with path.open("r", encoding=encoding, newline="", errors="replace") as handle:
        reader = csv.reader(handle, delimiter=delimiter)
        header = next(reader, [])
        header = [column.strip().lstrip("\ufeff") or f"column_{index + 1}" for index, column in enumerate(header)]
        width = len(header)
        missing_counts = [0] * width
        non_missing_counts = [0] * width
        numeric_counts = [0] * width
        distinct_samples = [set() for _ in range(width)]

        for raw_row in reader:
            if not raw_row or not any(cell.strip() for cell in raw_row):
                continue
            row_count += 1
            if len(raw_row) != width:
                malformed_rows += 1
            row = (raw_row + [""] * width)[:width]
            digest = hashlib.blake2b("\u001f".join(row).encode("utf-8", "replace"), digest_size=16).digest()
            if digest in seen_hashes:
                duplicate_rows += 1
            else:
                seen_hashes.add(digest)

            for index, value in enumerate(row):
                normalized = value.strip()
                if normalized.casefold() in MISSING:
                    missing_counts[index] += 1
                    continue
                non_missing_counts[index] += 1
                if is_number(normalized):
                    numeric_counts[index] += 1
                if len(distinct_samples[index]) < 101:
                    distinct_samples[index].add(normalized)

    columns = []
    for index, name in enumerate(header):
        non_missing = non_missing_counts[index]
        numeric_ratio = numeric_counts[index] / non_missing if non_missing else 0.0
        lower_name = name.casefold()
        columns.append(
            {
                "name": name,
                "inferred_type": "numeric" if non_missing and numeric_ratio >= 0.95 else "text",
                "missing_count": missing_counts[index],
                "missing_rate": round(missing_counts[index] / row_count, 6) if row_count else 0.0,
                "distinct_count_capped": len(distinct_samples[index]),
                "is_categorical_candidate": len(distinct_samples[index]) <= 100,
                "is_geographic_candidate": any(token in lower_name for token in GEO_TOKENS),
                "has_unit_signal": any(token in lower_name for token in UNIT_TOKENS),
            }
        )

    return {
        "dataset": dataset,
        "source_file": str(path.relative_to(ROOT)).replace("\\", "/"),
        "size_bytes": path.stat().st_size,
        "encoding": encoding,
        "delimiter": {",": "comma", "\t": "tab", ";": "semicolon", "|": "pipe"}.get(delimiter, delimiter),
        "record_count": row_count,
        "column_count": len(header),
        "columns": columns,
        "column_names": header,
        "missing_cell_count": sum(missing_counts),
        "duplicate_row_count": duplicate_rows,
        "malformed_row_count": malformed_rows,
        "numeric_fields": [column["name"] for column in columns if column["inferred_type"] == "numeric"],
        "categorical_fields": [column["name"] for column in columns if column["is_categorical_candidate"]],
        "geographic_fields": [column["name"] for column in columns if column["is_geographic_candidate"]],
        "unit_signal_fields": [column["name"] for column in columns if column["has_unit_signal"]],
        "sha256": sha256(path),
    }


def main() -> None:
    profiles = []
    for dataset, paths in FILES.items():
        for path in paths:
            if not path.exists():
                raise FileNotFoundError(path)
            profiles.append(profile(path, dataset))
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(
        json.dumps({"audit_version": 1, "files": profiles}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    for item in profiles:
        print(
            f"{item['dataset']}\t{Path(item['source_file']).name}\t"
            f"{item['record_count']} rows\t{item['column_count']} columns\t"
            f"{item['duplicate_row_count']} duplicates\t{item['missing_cell_count']} missing cells"
        )


if __name__ == "__main__":
    main()
