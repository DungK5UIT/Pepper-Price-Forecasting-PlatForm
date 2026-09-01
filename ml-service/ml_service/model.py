"""Quantile forecasting models.

Two strategies, both producing a q10/q50/q90 band per monthly horizon:

* ``gbm`` — the prototype's design: one
  :class:`~sklearn.ensemble.HistGradientBoostingRegressor` per
  (horizon, quantile), predicting the cumulative log-return.
* ``naive`` — a random walk: the median stays at the anchor and the band comes
  from historical monthly volatility.

The naive strategy is not a placeholder. With ~39 monthly training rows a
boosted tree can easily do worse than "the price stays where it is", so
training scores both and keeps whichever measures better (see ``train.py``).
"""

from __future__ import annotations

import math
from dataclasses import dataclass
from datetime import date
from typing import Literal

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import HistGradientBoostingRegressor

from .features import MODEL_FEATURE_COLUMNS, build_training_matrix, latest_feature_row

HORIZONS: tuple[int, ...] = (1, 2)
QUANTILES: tuple[float, ...] = (0.1, 0.5, 0.9)

#: 90th percentile of the standard normal — turns a sigma into a q10/q90 band.
Z90 = 1.2815515655446004

Strategy = Literal["gbm", "naive"]

#: Inherited from the prototype's `ml/train_lite.py`.
GBM_PARAMS = {
    "max_depth": 3,
    "max_iter": 150,
    "learning_rate": 0.05,
    "min_samples_leaf": 5,
    "random_state": 42,
}


@dataclass(frozen=True)
class HorizonForecast:
    horizon_months: int
    q10: float
    q50: float
    q90: float


@dataclass
class ForecastModel:
    """A trained model plus the volatility it falls back on."""

    version: str
    strategy: Strategy
    estimators: dict[tuple[int, float], HistGradientBoostingRegressor]
    monthly_sigma: float

    def predict(self, features: pd.DataFrame, anchor_price: float) -> list[HorizonForecast]:
        if self.strategy == "naive":
            return _naive_forecast(anchor_price, self.monthly_sigma)
        return _gbm_forecast(self.estimators, features, anchor_price)


def monthly_log_return_sigma(features: pd.DataFrame) -> float:
    """Standard deviation of month-on-month log returns, used by the band."""
    returns = features["dlogP"].dropna()
    if len(returns) < 2:
        return 0.0
    return float(returns.std(ddof=1))


def train(features: pd.DataFrame, strategy: Strategy = "gbm", version: str = "") -> ForecastModel:
    estimators: dict[tuple[int, float], HistGradientBoostingRegressor] = {}
    if strategy == "gbm":
        for horizon in HORIZONS:
            x, y = build_training_matrix(features, horizon)
            if x.empty:
                raise ValueError(
                    f"Not enough history to train horizon {horizon}: "
                    f"need at least {len(MODEL_FEATURE_COLUMNS)} usable months"
                )
            for quantile in QUANTILES:
                estimator = HistGradientBoostingRegressor(
                    loss="quantile", quantile=quantile, **GBM_PARAMS
                )
                estimator.fit(x, y)
                estimators[(horizon, quantile)] = estimator

    return ForecastModel(
        version=version or f"{strategy}-{date.today().isoformat()}",
        strategy=strategy,
        estimators=estimators,
        monthly_sigma=monthly_log_return_sigma(features),
    )


def _gbm_forecast(
    estimators: dict[tuple[int, float], HistGradientBoostingRegressor],
    features: pd.DataFrame,
    anchor_price: float,
) -> list[HorizonForecast]:
    row = latest_feature_row(features)
    log_anchor = math.log(anchor_price)

    forecasts = []
    for horizon in HORIZONS:
        # Quantile regressors are fitted independently, so nothing guarantees
        # q10 <= q50 <= q90 — sorting enforces it, as the prototype does.
        predictions = sorted(
            float(estimators[(horizon, quantile)].predict(row)[0]) for quantile in QUANTILES
        )
        q10, q50, q90 = (math.exp(log_anchor + delta) for delta in predictions)
        forecasts.append(HorizonForecast(horizon, q10, q50, q90))
    return forecasts


def _naive_forecast(anchor_price: float, monthly_sigma: float) -> list[HorizonForecast]:
    forecasts = []
    for horizon in HORIZONS:
        half_width = Z90 * monthly_sigma * math.sqrt(horizon)
        forecasts.append(
            HorizonForecast(
                horizon,
                anchor_price * math.exp(-half_width),
                anchor_price,
                anchor_price * math.exp(half_width),
            )
        )
    return forecasts


def save(model: ForecastModel, path) -> None:
    joblib.dump(model, path)


def load(path) -> ForecastModel:
    return joblib.load(path)


def pinball_loss(actual: float, predicted: float, quantile: float) -> float:
    """Standard quantile loss: under-prediction and over-prediction are
    penalised asymmetrically according to the quantile being estimated."""
    delta = actual - predicted
    return max(quantile * delta, (quantile - 1) * delta)


def mean_pinball(actuals: np.ndarray, predictions: np.ndarray, quantile: float) -> float:
    return float(np.mean([pinball_loss(a, p, quantile) for a, p in zip(actuals, predictions)]))
