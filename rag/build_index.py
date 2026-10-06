from __future__ import annotations

import argparse
import json
from pathlib import Path

from hnblue_rag import write_artifacts


def main() -> None:
    parser = argparse.ArgumentParser(description="Build portable HNBLUE RAG chunk artifacts.")
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    manifest = write_artifacts(args.source, args.output)
    print(json.dumps({
        "source_files": manifest["source_files"],
        "chunks": manifest["chunks"],
        "excluded_files": len(manifest["excluded_files"]),
        "chunks_sha256": manifest["chunks_sha256"],
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
