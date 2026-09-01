# Domain Model

Conceptual model of the core entities, their relationships, ownership, and
lifecycle. This is deliberately **not** a schema — no column types, no
constraints, no migrations. It exists to align on the shape of the domain
before any table is created.

## Entities

### DataSource
Reference configuration for a market/price data provider (e.g. a specific
exchange, association, or publication). Static, admin-managed. Owned by
the Java backend.

### MarketPrice
A single raw price observation: commodity, market/region, date, price,
unit, source. Immutable once ingested — corrections are new records, not
edits, so history stays auditable. Owned by the Java backend.

### WeatherObservation
Temperature, rainfall and wind for one growing province on one day, flagged as
observed or forecast. An input to price forecasting in its own right — heavy
rain or drought moves yields — and shown directly to users. Owned by the Java
backend.

### MarketInsight
Narrative commentary published for a given day, alongside the numbers.
Generated upstream rather than derived here; the backend stores and serves it.
Owned by the Java backend.

### DataIngestionRecord
The execution record of one ingestion run: which source, when, row counts,
success/failure. Append-only. Owned by the Java backend.

### Feature / FeatureSet
Engineered values derived from MarketPrice history (e.g. moving averages,
lag features, seasonality indicators). Computed by the ML service and
versioned alongside the model that consumes them — a feature set is
meaningful only in the context of the model version it was built for.

### ModelVersion
Metadata for one trained model artifact: algorithm, hyperparameters,
training data window, creation timestamp. Owned by the ML service;
referenced by the Java backend when serving forecasts so results are
traceable to the model that produced them.

### ModelEvaluationMetric
Evaluation results (e.g. RMSE, MAE) tied to a specific ModelVersion and
evaluation window. Owned by the ML service.

### Forecast
A prediction: commodity, target date, predicted price, confidence
interval, and the ModelVersion that generated it. Served to users through
the Java backend's public API.

### PredictionHistory
A Forecast paired with the actual outcome once it becomes known (i.e. once
the real MarketPrice for that date is ingested). Used to track forecast
accuracy over time. Derived, not independently entered.

### JobExecutionRecord
Cross-cutting execution log for background jobs — ingestion runs, training
runs, forecast generation runs: status, duration, error detail. Owned by
the Java backend, which orchestrates all job types.

### User
Authenticates through the Java backend. Owns future user-facing state
(saved views, alerts) — out of scope for Phase 0.

## Relationships

| From | To | Cardinality | Notes |
|---|---|---|---|
| DataSource | MarketPrice | 1 → * | A source produces many price observations over time |
| DataSource | DataIngestionRecord | 1 → * | Each ingestion run pulls from one source |
| MarketPrice | Feature | * → * (derived) | Features are computed over windows of price history, not a single record |
| ModelVersion | ModelEvaluationMetric | 1 → * | A model version is evaluated on potentially multiple windows/metrics |
| ModelVersion | Forecast | 1 → * | Every forecast is traceable to the model that produced it |
| Forecast | PredictionHistory | 1 → 0..1 | Populated once the actual outcome is known |
| JobExecutionRecord | (any job type) | cross-cutting | Not owned by a single entity; logs ingestion, training, and forecast jobs alike |

## Lifecycle Notes

- **MarketPrice** is append-only; it is the historical ground truth and
  must never be silently overwritten.
- **ModelVersion** is immutable once created — retraining produces a new
  version, not a mutation of an old one, so past forecasts remain
  explainable.
- **Forecast** records are generated, then later "closed out" by a
  PredictionHistory entry once the real price arrives — this is what makes
  accuracy tracking possible without re-deriving it from raw data each
  time.
- **DataIngestionRecord** and **JobExecutionRecord** exist purely for
  operational traceability — they are not domain data a user queries
  directly, but they are what makes ingestion/training failures
  diagnosable after the fact.

## Ownership Summary

- **Java backend**: User, DataSource, MarketPrice, WeatherObservation,
  MarketInsight, DataIngestionRecord, Forecast, PredictionHistory,
  JobExecutionRecord — the durable, business-facing state.
- **Python ML service**: Feature/FeatureSet, ModelVersion,
  ModelEvaluationMetric — computation artifacts, exchanged with the
  backend via the internal API rather than persisted independently (see
  ADR-0003).
