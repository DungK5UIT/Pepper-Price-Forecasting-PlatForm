import math

import pandas as pd
import pytest

from ml_service.features import (
    MODEL_FEATURE_COLUMNS,
    build_monthly_frame,
    build_training_matrix,
    compute_features,
    latest_feature_row,
)


def monthly_series(prices, year=2026):
    return build_monthly_frame(
        [
            {"date": f"{year}-{month:02d}-01", "priceVnd": float(price)}
            for month, price in enumerate(prices, start=1)
        ]
    )


def test_observations_in_the_same_month_are_averaged():
    monthly = build_monthly_frame(
        [
            {"date": "2026-01-05", "priceVnd": 100.0},
            {"date": "2026-01-20", "priceVnd": 200.0},
            {"date": "2026-02-10", "priceVnd": 300.0},
        ]
    )

    assert list(monthly["price"]) == [150.0, 300.0]
    assert [month.strftime("%Y-%m") for month in monthly["month"]] == ["2026-01", "2026-02"]


def test_log_returns_and_lags_line_up_with_the_series():
    features = compute_features(monthly_series([100 * 1.1**step for step in range(4)]))

    # Constant 10% growth means a constant log return, lagged by one row.
    assert features["dlogP"].iloc[1] == pytest.approx(math.log(1.1))
    assert features["dlogP_lag_1"].iloc[2] == pytest.approx(math.log(1.1))
    assert pd.isna(features["dlogP_lag_1"].iloc[0])


def test_seasonality_repeats_every_twelve_months():
    monthly = build_monthly_frame(
        [{"date": f"2026-{month:02d}-01", "priceVnd": 100.0} for month in range(1, 13)]
        + [{"date": "2027-01-01", "priceVnd": 100.0}]
    )

    features = compute_features(monthly)

    assert features["sin_m"].iloc[0] == pytest.approx(features["sin_m"].iloc[12])
    assert features["cos_m"].iloc[0] == pytest.approx(features["cos_m"].iloc[12])


def test_training_target_is_the_cumulative_log_return_over_the_horizon():
    features = compute_features(monthly_series([100 * 1.1**step for step in range(7)]))

    _, target = build_training_matrix(features, horizon=2)

    # Two months of 10% growth compounds to log(1.21) regardless of the level.
    assert target.iloc[0] == pytest.approx(math.log(1.21))


def test_latest_row_fills_missing_lags_so_a_short_history_can_still_predict():
    row = latest_feature_row(compute_features(monthly_series([100])))

    assert list(row.columns) == MODEL_FEATURE_COLUMNS
    assert not row.isna().any().any()
