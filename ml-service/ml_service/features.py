"""Monthly feature engineering.

Ported from the prototype's `ml/features.py`, with the feature set cut down to
what the backend can supply today. The prototype used 12 features including
CPI, FX, and two production-side series; none of those are in this platform's
schema, and `weather_observation` only starts 2026-08 against 44 months of
prices, so only price-derived and seasonal features survive. See ADR-0004.
"""

from __future__ import annotations

import numpy as np
import pandas as pd

#: Order matters — the models are fitted on this exact column order.
MODEL_FEATURE_COLUMNS = ["dlogP_lag_1", "dlogP_lag_2", "dlogP_lag_3", "sin_m", "cos_m"]

#: Lags consumed by the features above, so callers know how much history is
#: eaten before the first usable row.
MAX_LAG = 3


def build_monthly_frame(rows: list[dict]) -> pd.DataFrame:
    """Collapse price observations into one row per month.

    ``rows`` are ``{"date": "YYYY-MM-DD", "priceVnd": float}`` as delivered by
    the backend. Months with several observations are averaged, which is how
    the prototype built its monthly series from daily scrapes.
    """
    if not rows:
        return pd.DataFrame(columns=["month", "price"])

    frame = pd.DataFrame(rows)
    frame["date"] = pd.to_datetime(frame["date"])
    frame = frame.rename(columns={"priceVnd": "price"})
    monthly = (
        frame.groupby(frame["date"].dt.to_period("M"))["price"]
        .mean()
        .reset_index()
        .rename(columns={"date": "month"})
    )
    monthly["month"] = monthly["month"].dt.to_timestamp()
    return monthly.sort_values("month").reset_index(drop=True)


def compute_features(monthly: pd.DataFrame) -> pd.DataFrame:
    """Add log price, its differences and lags, and month-of-year seasonality.

    Rows whose lags are not yet available keep NaN; callers decide whether to
    drop them (training) or fill them (inference), matching the prototype.
    """
    if monthly.empty:
        # Callers surface this as a bad request; blowing up inside pandas would
        # not say why.
        return pd.DataFrame(columns=["month", "price", "log_price", "dlogP", *MODEL_FEATURE_COLUMNS])

    features = monthly.copy()
    features["log_price"] = np.log(features["price"])
    features["dlogP"] = features["log_price"].diff()

    for lag in range(1, MAX_LAG + 1):
        features[f"dlogP_lag_{lag}"] = features["dlogP"].shift(lag)

    month_of_year = features["month"].dt.month
    features["sin_m"] = np.sin(2 * np.pi * month_of_year / 12)
    features["cos_m"] = np.cos(2 * np.pi * month_of_year / 12)

    return features


def build_training_matrix(features: pd.DataFrame, horizon: int) -> tuple[pd.DataFrame, pd.Series]:
    """Features and cumulative log-return target for one horizon.

    The target is ``log(P_t+h) - log(P_t)``: the model predicts the move from
    today, not the price level, so the level always comes from the anchor.
    """
    frame = features.copy()
    frame["target"] = frame["log_price"].shift(-horizon) - frame["log_price"]
    usable = frame.dropna(subset=MODEL_FEATURE_COLUMNS + ["target"])
    return usable[MODEL_FEATURE_COLUMNS], usable["target"]


def latest_feature_row(features: pd.DataFrame) -> pd.DataFrame:
    """The most recent row, as a single-row frame ready for prediction."""
    if features.empty:
        raise ValueError("No price history to build features from")
    return features[MODEL_FEATURE_COLUMNS].iloc[[-1]].fillna(0.0)
