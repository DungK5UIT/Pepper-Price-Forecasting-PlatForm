"""FastAPI application exposing the internal forecasting API.

Only the Java backend calls this (ADR-0002); nothing here is reachable by the
frontend, and the service holds no state of its own beyond the trained model
it loads from disk.
"""

from __future__ import annotations

import logging
import os
from functools import lru_cache
from pathlib import Path

from fastapi import Depends, FastAPI, Header, HTTPException

from . import model as model_module
from .forecasting import generate
from .schemas import ForecastRequest, ForecastResponse

logger = logging.getLogger(__name__)

ARTIFACT_PATH = Path(__file__).parent / "artifacts" / "forecast_model.joblib"

# This service now runs as its own public deployment with no private network
# to rely on instead (ADR-0008), so /internal/v1/forecast checks this shared
# secret to keep ADR-0002's "only the backend calls this" true in practice.
# No default: a blank fallback is how an internal endpoint ends up open.
try:
    INTERNAL_TOKEN = os.environ["ML_SERVICE_TOKEN"]
except KeyError as error:
    raise RuntimeError(
        "ML_SERVICE_TOKEN is not set. See ml-service/.env.example."
    ) from error

app = FastAPI(
    title="Pepper price ML service",
    description="Internal forecasting API consumed by the Java backend.",
    version="0.1.0",
)


def verify_token(x_internal_token: str = Header(default="")) -> None:
    if x_internal_token != INTERNAL_TOKEN:
        raise HTTPException(status_code=401, detail="Missing or invalid X-Internal-Token")


@lru_cache(maxsize=1)
def load_model() -> model_module.ForecastModel:
    """Loaded once and memoised — the artifact does not change while running."""
    if not ARTIFACT_PATH.exists():
        raise FileNotFoundError(
            f"No trained model at {ARTIFACT_PATH}. Run: python -m ml_service.train"
        )
    return model_module.load(ARTIFACT_PATH)


@app.get("/health")
def health() -> dict[str, str]:
    """Liveness plus which model is loaded, so a stale artifact is visible."""
    try:
        loaded = load_model()
    except FileNotFoundError:
        return {"status": "DEGRADED", "detail": "no trained model"}
    return {"status": "UP", "modelVersion": loaded.version, "strategy": loaded.strategy}


@app.post("/internal/v1/forecast", response_model=ForecastResponse, dependencies=[Depends(verify_token)])
def forecast(request: ForecastRequest) -> ForecastResponse:
    try:
        loaded = load_model()
    except FileNotFoundError as error:
        raise HTTPException(status_code=503, detail=str(error)) from error

    try:
        return generate(loaded, request)
    except ValueError as error:
        # Raised when the supplied history is too short to build features from.
        raise HTTPException(status_code=422, detail=str(error)) from error
