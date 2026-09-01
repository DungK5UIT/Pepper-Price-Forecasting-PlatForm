"""Assembles a full forecast response from a trained model.

Kept separate from the FastAPI layer so the same logic is testable without
going through HTTP.
"""

from __future__ import annotations

import calendar
from datetime import date, timedelta

from .features import build_monthly_frame, compute_features
from .interpolate import daily_log_return_sigma, expand, implied_daily_sigma
from .model import ForecastModel
from .schemas import ForecastPoint, ForecastRequest, ForecastResponse

#: Points every N days, per granularity the public API offers.
STEP_DAYS = {"day": 1, "week": 7}


def target_month_end(anchor: date, horizon_months: int) -> date:
    """Last day of the month a horizon lands in.

    Horizon 1 is the *current* month, not the next one: the model steps forward
    from the last completed monthly observation, which is the month before the
    as-of date.
    """
    total = anchor.month - 1 + horizon_months - 1
    year = anchor.year + total // 12
    month = total % 12 + 1
    return date(year, month, calendar.monthrange(year, month)[1])


def generate(model: ForecastModel, request: ForecastRequest) -> ForecastResponse:
    history = [{"date": row.date, "priceVnd": row.priceVnd} for row in request.history]
    features = compute_features(build_monthly_frame(history))

    horizons = [
        forecast
        for forecast in model.predict(features, request.anchorPrice)
        if forecast.horizon_months <= request.horizonMonths
    ]

    monthly_points = [
        ForecastPoint(
            targetDate=target_month_end(request.asOfDate, forecast.horizon_months),
            q10=forecast.q10,
            q50=forecast.q50,
            q90=forecast.q90,
            interpolated=False,
        )
        for forecast in horizons
    ]

    # Measured volatility if the recent daily prices support it; otherwise fall
    # back to whatever the monthly band implies.
    daily_prices = [(row.date, row.priceVnd) for row in request.history]
    sigma_day = daily_log_return_sigma(daily_prices)
    if sigma_day is None:
        sigma_day = implied_daily_sigma(horizons)

    # A horizon that has already ended (as-of falls on month end) gives no room
    # to interpolate into, so it contributes no node.
    nodes = [
        (days, point.q50)
        for point in monthly_points
        if (days := (point.targetDate - request.asOfDate).days) > 0
    ]

    points = {"month": monthly_points}
    for granularity, step in STEP_DAYS.items():
        points[granularity] = [
            ForecastPoint(
                targetDate=request.asOfDate + timedelta(days=point.day_offset),
                q10=point.q10,
                q50=point.q50,
                q90=point.q90,
                interpolated=True,
            )
            for point in expand(request.anchorPrice, nodes, sigma_day, step)
        ]

    return ForecastResponse(
        modelVersion=model.version,
        asOfDate=request.asOfDate,
        strategy=model.strategy,
        points=points,
    )
