# ML Service

Python + FastAPI service. Stateless ML/data-science computation for the
platform.

**Owns**: feature engineering, model training, model evaluation, and
forecast generation logic. Exposes an internal API consumed only by the
Java backend.

**Does not own**: persistence of platform state, authentication, or any
API surface exposed to the frontend. Has no direct PostgreSQL access — see
`docs/adr/0003-ml-service-data-access.md` for why, and the open question
it leaves for revisiting if training-data throughput ever requires it.

## Status

Running. The model design is ported from the earlier prototype: quantile
gradient boosting on a cumulative log-return target over monthly horizons, with
daily and weekly points interpolated from the monthly median. The feature set
is reduced to what the backend can supply, and every training run scores the
model against a naive baseline and keeps whichever is better — see
[`docs/adr/0004-forecasting-model.md`](../docs/adr/0004-forecasting-model.md).

**The baseline currently wins**, so what ships is the random walk: the median
stays at the latest observed price and the band widens with time. That is the
honest reading of 44 monthly data points, not a placeholder.

## Running

```bash
python -m venv .venv && .venv/Scripts/activate   # or source .venv/bin/activate
pip install -r requirements-dev.txt

pytest                                            # no network, no credentials
python -m uvicorn ml_service.main:app --port 8000
curl http://localhost:8000/health
```

`/health` reports which model version is loaded, so a stale artifact is visible
without digging.

## Training

Training data comes from the backend, which must be running (ADR-0003). Its
`/internal/**` endpoints require the machine credential the backend was started
with (ADR-0006), read from the environment:

```bash
export INTERNAL_API_USER=ml-service
export INTERNAL_API_PASSWORD=...      # the value in backend/.env
python -m ml_service.train --backend-url http://localhost:8080
```

Unset, the run fails with a message saying so rather than falling back to a
default that would stop matching the backend the day it gets a real password.

Writes `ml_service/artifacts/forecast_model.joblib` and
`ml_service/artifacts/metrics.json`. Both are committed so a fresh clone can
serve forecasts without database access.

`metrics.json` holds the walk-forward scores for both strategies, which
strategy was selected, and how much history it was trained on.

## Internal API

`POST /internal/v1/forecast` — called by the backend, never by a browser.

```json
{ "asOfDate": "2026-09-01", "anchorPrice": 135700,
  "history": [{ "date": "2023-01-01", "priceVnd": 58700 }],
  "horizonMonths": 2 }
```

Responds with `modelVersion`, `strategy`, and `points` keyed `month` / `week` /
`day`, each point `{ targetDate, q10, q50, q90, interpolated }`.

The `history` is used twice: monthly averages build the features, and the daily
portion measures the volatility that sets the band width. Points marked
`interpolated` are derived from the monthly median, not predicted.

## Layout

```
ml_service/
  main.py         FastAPI app
  schemas.py      request/response models
  features.py     monthly feature frame
  model.py        the two strategies, training and prediction
  interpolate.py  monthly median → day and week points
  train.py        training CLI: fetch, train, backtest, write artifacts
  artifacts/      committed model + metrics
```
