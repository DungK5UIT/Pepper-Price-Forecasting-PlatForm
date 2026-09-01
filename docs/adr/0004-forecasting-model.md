# ADR-0004: Forecasting Model, Feature Set, and Baseline Selection

- **Status**: Accepted
- **Date**: 2026-09-01

## Context

The platform had no forecasting of its own: the rows it served were imported
from an earlier prototype (`D:\DuBaoGiaTieu`). That prototype's production
model is a quantile gradient-boosting design — one
`HistGradientBoostingRegressor(loss="quantile")` per (horizon × quantile),
predicting a cumulative log-return over monthly horizons, with daily and weekly
points interpolated rather than predicted.

Two constraints shape what can be reused:

- ADR-0003 gives the ML service no database access, so its inputs are whatever
  the backend's API supplies. The backend owns prices and weather; it owns
  neither the CPI, FX rate, nor the production-side series the prototype's 12
  features depend on.
- The history in this platform's database is 44 monthly price points
  (2023-01 → 2026-08), against the prototype's 216-row bootstrap series.
  `weather_observation` starts 2026-08, which is one month — not enough to lag.

## Decision

- **Port the prototype's model design**: same estimator and hyperparameters,
  same cumulative-log-return target, same monthly horizons, same
  interpolation of day/week points from the monthly median with a band that
  widens as `√t`.
- **Reduce the feature set** to what the backend can supply: `dlogP_lag_1..3`,
  `sin_m`, `cos_m`. Macro and weather features are dropped for lack of data,
  not for lack of relevance.
- **Score every trained model against a naive baseline** (random walk: the
  median stays at the anchor, the band comes from historical monthly
  volatility) on a walk-forward expanding-window split, using pinball loss,
  median absolute error, and coverage of the 10–90 band. **Whichever measures
  better is what the service serves**, recorded in
  `ml-service/ml_service/artifacts/metrics.json`.

On the first run, the baseline won and is what ships:

| | mean pinball (VND) | MAE (VND) | 10–90 coverage |
|---|---|---|---|
| GBM | 3,315.8 | 9,722.5 | 0.705 |
| naive | **2,877.9** | **7,130.8** | **0.869** |

## Consequences

- The forecasting pipeline is real end to end — trained, measured, served,
  persisted — while the model on it is honestly weak. A user reading the
  dashboard today is seeing "the price probably stays around here, and here is
  how uncertain that is", which is what the numbers support.
- The comparison is not a one-off: every training run re-runs it, so a future
  model has to earn its place against the baseline rather than being assumed
  better because it is fancier.
- Day and weekly points carry no information the monthly forecast does not
  already contain. They exist so the chart can draw a continuous line, and are
  flagged `interpolated` in the internal API.
- The GBM code stays in the repository even though the baseline currently
  wins — it is what more data will be tested against.

**Revisit when**: a year or so of daily weather accumulates (restoring the
weather features), the platform ingests CPI/FX (restoring the macro features),
or the price history grows enough that the GBM beats the baseline on the same
split.

## Alternatives Considered

- **Import the prototype's 216-month bootstrap series with its macro columns**:
  rejected for now — it needs new backend-owned tables and, more importantly, a
  live CPI/FX feed to be usable at prediction time; carrying stale macro values
  forward would make the extra features decorative.
- **Ship the GBM regardless, because it is the "real" model**: rejected — it
  measures worse on the data we have, and serving a worse forecast to look more
  sophisticated is not a trade this project makes.
- **Port the prototype's TensorFlow Conv1D-LSTM**: rejected — it needs far more
  data than 44 points, and the stack direction puts scikit-learn first, with
  deep learning introduced only when a specific model justifies it.
