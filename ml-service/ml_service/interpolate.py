"""Turning monthly forecasts into daily and weekly points.

The model only predicts monthly horizons. Daily and weekly points are
*derived*, not predicted: the median follows the monthly path interpolated in
log space, and the uncertainty band widens with the square root of time, the
way a random walk's does. Ported from the prototype's
`backend/app/services/forecast_service.py`.

Because they are derived, these points carry no information the monthly
forecast does not already contain — they exist so the chart can show a
continuous line.
"""

from __future__ import annotations

import math
from dataclasses import dataclass

from .model import Z90, HorizonForecast

DAYS_PER_MONTH = 30.0

#: Fewer daily returns than this and the measured volatility is too noisy to
#: trust, so the band falls back to what the monthly quantiles imply.
MIN_OBS_FOR_VOLATILITY = 10


@dataclass(frozen=True)
class InterpolatedPoint:
    day_offset: int
    q10: float
    q50: float
    q90: float


def daily_log_return_sigma(daily_prices: list[tuple[object, float]]) -> float | None:
    """Daily volatility measured from observed prices, or None if too sparse.

    Gaps are normalised by ``sqrt(gap_days)`` so that a price two days apart
    does not look twice as volatile as one a day apart.
    """
    scaled_returns = []
    for (previous_date, previous_price), (current_date, current_price) in zip(
        daily_prices, daily_prices[1:]
    ):
        gap_days = (current_date - previous_date).days
        if gap_days <= 0 or previous_price <= 0 or current_price <= 0:
            continue
        scaled_returns.append(math.log(current_price / previous_price) / math.sqrt(gap_days))

    if len(scaled_returns) < MIN_OBS_FOR_VOLATILITY:
        return None

    mean = sum(scaled_returns) / len(scaled_returns)
    variance = sum((value - mean) ** 2 for value in scaled_returns) / (len(scaled_returns) - 1)
    return math.sqrt(variance)


def implied_daily_sigma(forecasts: list[HorizonForecast]) -> float:
    """Daily volatility backed out of the monthly band widths."""
    per_month = []
    for forecast in forecasts:
        if forecast.q10 <= 0 or forecast.q90 <= 0:
            continue
        sigma_horizon = (math.log(forecast.q90) - math.log(forecast.q10)) / (2 * Z90)
        per_month.append(sigma_horizon / math.sqrt(forecast.horizon_months))

    if not per_month:
        return 0.0
    return (sum(per_month) / len(per_month)) / math.sqrt(DAYS_PER_MONTH)


def _drift_at(path: list[tuple[float, float]], day: float) -> float:
    """Piecewise-linear interpolation of the median's log-drift."""
    if day <= path[0][0]:
        return path[0][1]
    for (day_before, drift_before), (day_after, drift_after) in zip(path, path[1:]):
        if day <= day_after:
            span = day_after - day_before
            if span <= 0:
                return drift_after
            weight = (day - day_before) / span
            return drift_before + weight * (drift_after - drift_before)
    return path[-1][1]


def expand(
    anchor_price: float,
    nodes: list[tuple[int, float]],
    sigma_day: float,
    step_days: int,
) -> list[InterpolatedPoint]:
    """Points every ``step_days`` along the monthly median path.

    ``nodes`` are ``(days_from_anchor, median_price)`` per monthly horizon. The
    series stops at the last node — the monthly forecast is not extrapolated
    past its own horizon.
    """
    if anchor_price <= 0 or not nodes:
        return []

    log_anchor = math.log(anchor_price)
    path = [(0.0, 0.0)] + [
        (float(days), math.log(price) - log_anchor) for days, price in nodes if price > 0
    ]

    max_days = int(path[-1][0])
    points = []
    for day in range(step_days, max_days + 1, step_days):
        median = math.exp(log_anchor + _drift_at(path, day))
        half_width = Z90 * sigma_day * math.sqrt(day)
        points.append(
            InterpolatedPoint(
                day_offset=day,
                q10=median * math.exp(-half_width),
                q50=median,
                q90=median * math.exp(half_width),
            )
        )
    return points
