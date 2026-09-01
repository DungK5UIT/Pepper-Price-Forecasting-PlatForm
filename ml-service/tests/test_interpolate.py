import math
from datetime import date, timedelta

import pytest

from ml_service.interpolate import (
    MIN_OBS_FOR_VOLATILITY,
    daily_log_return_sigma,
    expand,
    implied_daily_sigma,
)
from ml_service.model import Z90, HorizonForecast


def test_volatility_needs_enough_observations():
    start = date(2026, 8, 1)
    too_few = [(start + timedelta(days=i), 100.0 + i) for i in range(MIN_OBS_FOR_VOLATILITY)]

    # N prices give N-1 returns, one short of the minimum.
    assert daily_log_return_sigma(too_few) is None
    assert daily_log_return_sigma(too_few + [(start + timedelta(days=20), 120.0)]) is not None


def test_volatility_normalises_gaps_so_sparse_days_are_not_read_as_volatile():
    start = date(2026, 8, 1)
    daily = [(start, 100.0)]
    for i in range(1, 15):
        # Every step is a 1% move but spread over four days.
        daily.append((start + timedelta(days=4 * i), daily[-1][1] * 1.01))

    sigma = daily_log_return_sigma(daily)

    # Constant scaled returns means no dispersion at all.
    assert sigma == pytest.approx(0.0, abs=1e-12)


def test_implied_sigma_reverses_the_band_construction():
    sigma_month = 0.05
    horizon = 1
    half_width = Z90 * sigma_month * math.sqrt(horizon)
    forecast = HorizonForecast(horizon, 100 * math.exp(-half_width), 100.0, 100 * math.exp(half_width))

    assert implied_daily_sigma([forecast]) == pytest.approx(sigma_month / math.sqrt(30.0))


def test_median_follows_the_monthly_path_and_stops_at_its_last_node():
    points = expand(anchor_price=100.0, nodes=[(30, 110.0), (60, 121.0)], sigma_day=0.0, step_days=1)

    assert len(points) == 60
    assert points[-1].day_offset == 60
    # Log-linear interpolation puts the halfway point at the geometric mean.
    assert points[14].q50 == pytest.approx(100.0 * (1.1 ** 0.5), rel=1e-6)
    assert points[-1].q50 == pytest.approx(121.0)


def test_band_widens_with_the_square_root_of_time():
    sigma_day = 0.01
    points = expand(anchor_price=100.0, nodes=[(60, 100.0)], sigma_day=sigma_day, step_days=1)

    def half_width(point):
        return math.log(point.q90) - math.log(point.q50)

    assert half_width(points[0]) == pytest.approx(Z90 * sigma_day * math.sqrt(1))
    # Four times the elapsed days should double the width, not quadruple it.
    assert half_width(points[15]) == pytest.approx(2 * half_width(points[3]), rel=1e-9)


def test_weekly_steps_are_a_seven_day_sample_of_the_same_path():
    daily = expand(100.0, [(28, 110.0)], sigma_day=0.01, step_days=1)
    weekly = expand(100.0, [(28, 110.0)], sigma_day=0.01, step_days=7)

    assert [point.day_offset for point in weekly] == [7, 14, 21, 28]
    assert weekly[0].q50 == pytest.approx(daily[6].q50)


def test_no_points_without_a_usable_anchor_or_nodes():
    assert expand(0.0, [(30, 110.0)], 0.01, 1) == []
    assert expand(100.0, [], 0.01, 1) == []
