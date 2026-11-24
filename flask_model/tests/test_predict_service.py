from __future__ import annotations

import sys
from pathlib import Path

import pytest


MODULE_ROOT = Path(__file__).resolve().parents[1] / "carbon_model_api"
sys.path.insert(0, str(MODULE_ROOT))

from predict_service import batch_predict_carbon_stock, predict_carbon_stock  # noqa: E402


SAMPLE = {"treeHeight": 8.5, "dbh": 12.0, "canopy": 4.2, "age": 18}


def test_single_and_batch_prediction_contract() -> None:
    single = predict_carbon_stock(SAMPLE)
    batch = batch_predict_carbon_stock([SAMPLE, {**SAMPLE, "age": 25}])
    assert isinstance(single, float)
    assert len(batch) == 2


@pytest.mark.parametrize(
    "payload",
    [
        {"dbh": 12.0, "canopy": 4.2},
        {**SAMPLE, "treeHeight": 0},
        {**SAMPLE, "latitude": 91},
        {**SAMPLE, "age": -1},
    ],
)
def test_invalid_model_input_fails_closed(payload: dict) -> None:
    with pytest.raises(ValueError):
        predict_carbon_stock(payload)


def test_batch_limit_fails_closed() -> None:
    with pytest.raises(ValueError):
        batch_predict_carbon_stock([SAMPLE] * 101)
