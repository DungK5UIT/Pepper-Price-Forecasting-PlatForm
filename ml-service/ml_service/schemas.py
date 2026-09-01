"""Request and response models for the internal API.

Only the Java backend calls this service (ADR-0002), so these shapes are an
internal contract between the two, not a public one.
"""

from __future__ import annotations

from datetime import date

from pydantic import BaseModel, Field


class PriceObservation(BaseModel):
    date: date
    priceVnd: float = Field(gt=0)


class ForecastRequest(BaseModel):
    asOfDate: date
    anchorPrice: float = Field(gt=0, description="Latest observed price the forecast starts from")
    history: list[PriceObservation] = Field(
        description="Nationwide price history, oldest first. Monthly features are "
        "aggregated from it, and its daily portion measures volatility."
    )
    horizonMonths: int = Field(default=2, ge=1, le=2)


class ForecastPoint(BaseModel):
    targetDate: date
    q10: float
    q50: float
    q90: float
    interpolated: bool


class ForecastResponse(BaseModel):
    modelVersion: str
    asOfDate: date
    strategy: str
    points: dict[str, list[ForecastPoint]]
