"""Train the forecasting model and measure whether it is worth using.

Run against a running backend, which is where the training data comes from
(ADR-0003 — this service has no database access):

    python -m ml_service.train --backend-url http://localhost:8080

Writes ``artifacts/forecast_model.joblib`` and ``artifacts/metrics.json``.

The backtest is not decoration. The available history is ~44 monthly points, at
which size a boosted tree can easily be beaten by assuming the price does not
move. Both strategies are scored on the same expanding-window split and the
better one is what gets saved.
"""

from __future__ import annotations

import argparse
import json
import logging
from datetime import date, datetime, timezone
from pathlib import Path

import httpx
import pandas as pd

from .features import MODEL_FEATURE_COLUMNS, build_monthly_frame, build_training_matrix, compute_features
from .model import HORIZONS, QUANTILES, Strategy, mean_pinball, save, train

logger = logging.getLogger(__name__)

ARTIFACT_DIR = Path(__file__).parent / "artifacts"

#: Mirrors the prototype's 12-month sufficiency gate: fewer than a year of
#: history and a fold is not worth scoring.
MIN_TRAIN_ROWS = 12


def fetch_history(backend_url: str) -> list[dict]:
    response = httpx.get(
        f"{backend_url.rstrip('/')}/internal/v1/price-history",
        params={"region": "national"},
        timeout=30.0,
    )
    response.raise_for_status()
    return response.json()


def backtest(features: pd.DataFrame, strategy: Strategy) -> dict:
    """Expanding-window walk-forward: train on everything up to a month, then
    predict the following one and two months and score against what happened."""
    actuals: dict[float, list[float]] = {quantile: [] for quantile in QUANTILES}
    predictions: dict[float, list[float]] = {quantile: [] for quantile in QUANTILES}
    inside_band = 0
    absolute_errors = []

    for cutoff in range(MIN_TRAIN_ROWS, len(features) - 1):
        window = features.iloc[: cutoff + 1]
        anchor_price = float(window["price"].iloc[-1])
        try:
            model = train(window, strategy=strategy)
        except ValueError:
            continue

        for forecast in model.predict(window, anchor_price):
            future_index = cutoff + forecast.horizon_months
            if future_index >= len(features):
                continue
            actual = float(features["price"].iloc[future_index])

            for quantile, predicted in zip(QUANTILES, (forecast.q10, forecast.q50, forecast.q90)):
                actuals[quantile].append(actual)
                predictions[quantile].append(predicted)

            absolute_errors.append(abs(actual - forecast.q50))
            if forecast.q10 <= actual <= forecast.q90:
                inside_band += 1

    scored = len(absolute_errors)
    if scored == 0:
        return {"scoredPredictions": 0}

    pinball = {
        f"q{int(quantile * 100)}": round(
            mean_pinball(actuals[quantile], predictions[quantile], quantile), 1
        )
        for quantile in QUANTILES
    }
    return {
        "scoredPredictions": scored,
        "pinballVnd": pinball,
        "meanPinballVnd": round(sum(pinball.values()) / len(pinball), 1),
        "medianAbsoluteErrorVnd": round(sum(absolute_errors) / scored, 1),
        # The 10–90 band should contain ~80% of outcomes if it is honest.
        "bandCoverage": round(inside_band / scored, 3),
    }


def main() -> None:
    logging.basicConfig(level=logging.INFO, format="%(message)s")
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--backend-url", default="http://localhost:8080")
    parser.add_argument("--output-dir", type=Path, default=ARTIFACT_DIR)
    arguments = parser.parse_args()

    history = fetch_history(arguments.backend_url)
    monthly = build_monthly_frame(history)
    features = compute_features(monthly)
    logger.info(
        "Fetched %d observations across %d months (%s to %s)",
        len(history),
        len(monthly),
        monthly["month"].min().date(),
        monthly["month"].max().date(),
    )

    scores = {strategy: backtest(features, strategy) for strategy in ("gbm", "naive")}
    for strategy, score in scores.items():
        logger.info("%-6s %s", strategy, score)

    comparable = {
        strategy: score["meanPinballVnd"]
        for strategy, score in scores.items()
        if score.get("scoredPredictions")
    }
    if not comparable:
        raise SystemExit("Not enough history to score any strategy — train aborted")
    selected: Strategy = min(comparable, key=comparable.get)  # type: ignore[assignment]
    logger.info("Selected strategy: %s (lower mean pinball wins)", selected)

    version = f"{selected}-{date.today().isoformat()}"
    model = train(features, strategy=selected, version=version)

    arguments.output_dir.mkdir(parents=True, exist_ok=True)
    save(model, arguments.output_dir / "forecast_model.joblib")

    metrics = {
        "trainedAt": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "modelVersion": version,
        "selectedStrategy": selected,
        "features": MODEL_FEATURE_COLUMNS,
        "monthlyPoints": len(monthly),
        "historyRange": {
            "from": str(monthly["month"].min().date()),
            "to": str(monthly["month"].max().date()),
        },
        "trainingRows": {
            f"horizon{horizon}": int(len(build_training_matrix(features, horizon)[1]))
            for horizon in HORIZONS
        },
        "backtest": scores,
    }
    (arguments.output_dir / "metrics.json").write_text(
        json.dumps(metrics, indent=2) + "\n", encoding="utf-8"
    )
    logger.info("Wrote artifacts to %s", arguments.output_dir)


if __name__ == "__main__":
    main()
