import json
from pathlib import Path

from hnblue_agent.tools import default_registry


ROOT = Path(__file__).resolve().parents[1]


def test_four_read_only_tools_have_versioned_schemas() -> None:
    schemas = sorted((ROOT / "capability_kit").glob("*.schema.json"))
    assert len(schemas) == 4
    for path in schemas:
        payload = json.loads(path.read_text(encoding="utf-8"))
        assert payload["x-hnblue"]["readOnly"] is True
        assert payload["x-hnblue"]["sourceIdentity"]
        assert payload["x-hnblue"]["toolVersion"] == "1.0.0"
        assert payload["x-hnblue"]["timeoutMs"] > 0
        assert payload["x-hnblue"]["errorCodes"]
        assert payload["x-hnblue"]["sourceContract"]
        assert payload["additionalProperties"] is False
    assert len(default_registry().describe()) == 4


def test_skill_forbids_write_and_fabricated_measurements() -> None:
    text = (ROOT / "skills" / "blue-carbon-analysis" / "SKILL.md").read_text(encoding="utf-8")
    assert "Do not call write" in text
    assert "Never manufacture a measurement" in text
    assert "synthetic-fixture" in text
