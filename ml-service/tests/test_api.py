from datetime import date, timedelta

import pytest
from fastapi.testclient import TestClient

from ml_service import main
from ml_service.features import build_monthly_frame, compute_features
from ml_service.model import train


@pytest.fixture
def history():
    """Three years of monthly prices with a mild trend, plus recent daily ones
    so the band has measured volatility to work with."""
    rows = []
    price = 100_000.0
    start = date(2023, 1, 1)
    for month in range(38):
        year = start.year + (start.month - 1 + month) // 12
        month_of_year = (start.month - 1 + month) % 12 + 1
        price *= 1.01 if month % 2 == 0 else 0.995
        rows.append({"date": date(year, month_of_year, 1).isoformat(), "priceVnd": round(price, 2)})

    last = date(2026, 2, 1)
    for day in range(1, 20):
        price *= 1.001
        rows.append({"date": (last + timedelta(days=day)).isoformat(), "priceVnd": round(price, 2)})
    return rows


@pytest.fixture
def client(history, tmp_path, monkeypatch):
    """A client backed by a model trained in-process, so the tests do not
    depend on whichever artifact happens to be committed."""
    features = compute_features(build_monthly_frame(history))
    model = train(features, strategy="gbm", version="test-model")

    main.load_model.cache_clear()
    monkeypatch.setattr(main, "load_model", lambda: model)
    return TestClient(main.app)


def request_body(history, **overrides):
    body = {
        "asOfDate": "2026-02-20",
        "anchorPrice": 135_000,
        "history": history,
        "horizonMonths": 2,
    }
    body.update(overrides)
    return body


def test_health_reports_the_loaded_model(client):
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "UP", "modelVersion": "test-model", "strategy": "gbm"}


def test_forecast_returns_all_three_granularities(client, history):
    response = client.post("/internal/v1/forecast", json=request_body(history))

    assert response.status_code == 200
    body = response.json()
    assert body["modelVersion"] == "test-model"
    assert set(body["points"]) == {"day", "week", "month"}
    assert len(body["points"]["month"]) == 2
    assert [point["interpolated"] for point in body["points"]["month"]] == [False, False]
    assert all(point["interpolated"] for point in body["points"]["day"])


def test_monthly_targets_land_on_month_ends_after_the_as_of_date(client, history):
    body = client.post("/internal/v1/forecast", json=request_body(history)).json()

    assert [point["targetDate"] for point in body["points"]["month"]] == ["2026-02-28", "2026-03-31"]


def test_quantiles_are_ordered_everywhere(client, history):
    body = client.post("/internal/v1/forecast", json=request_body(history)).json()

    for granularity, points in body["points"].items():
        for point in points:
            assert point["q10"] <= point["q50"] <= point["q90"], (granularity, point)


def test_a_single_horizon_can_be_requested(client, history):
    body = client.post("/internal/v1/forecast", json=request_body(history, horizonMonths=1)).json()

    assert len(body["points"]["month"]) == 1
    # Day points stop at the only monthly node, roughly a month out.
    assert body["points"]["day"][-1]["targetDate"] == "2026-02-28"


def test_rejects_a_request_without_history(client):
    response = client.post("/internal/v1/forecast", json=request_body([]))

    assert response.status_code == 422


def test_reports_degraded_when_no_model_is_trained(monkeypatch, tmp_path):
    main.load_model.cache_clear()
    monkeypatch.setattr(main, "ARTIFACT_PATH", tmp_path / "missing.joblib")

    response = TestClient(main.app).get("/health")

    assert response.json()["status"] == "DEGRADED"
    main.load_model.cache_clear()
