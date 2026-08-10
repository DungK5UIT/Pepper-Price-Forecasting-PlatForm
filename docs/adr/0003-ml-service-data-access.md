# ADR-0003: ML Service Has No Direct Database Access

- **Status**: Accepted
- **Date**: 2026-08-10

## Context

Given the Java-backend/Python-ML-service split in ADR-0002, the ML service
needs market price and feature data as input, and needs to return
forecasts/metrics as output. There are two general ways to move that data:
give the ML service its own PostgreSQL credentials and let it read/write
directly, or route everything through the Java backend's API.

## Decision

The Python ML service does **not** get direct PostgreSQL access. All data
in and out flows through the Java backend's internal API: the backend
supplies the data the ML service needs for training/feature engineering,
and the ML service returns forecasts and evaluation metrics for the
backend to persist.

## Consequences

- The Java backend remains the single point of schema ownership and data
  validation — no second system can write malformed or unvalidated rows
  into PostgreSQL.
- The database credential/security surface stays smaller: one service
  holds production DB credentials, not two.
- Schema changes only ever require coordinating within the backend, not
  across two independently-evolving services' data-access code.
- Trade-off, explicitly accepted for now: bulk reads for model training
  (potentially large historical windows) go through a REST call rather
  than a direct query, which is less efficient than direct DB access at
  scale.

## Open Question (not resolved by this ADR)

If training-data pull performance through the API proves inadequate (e.g.
demonstrated latency or throughput problems once training routinely runs
against real history), the response should be evaluated then — options
include a dedicated bulk-export endpoint, a read replica the ML service
can query, or a feature store — rather than pre-building for a performance
problem that doesn't exist yet. Revisit only on measured evidence.

## Alternatives Considered

- **Direct read-only DB access for the ML service**: rejected for now —
  adds a second credentialed consumer of the schema and a second thing to
  keep in sync with migrations, for a performance benefit that isn't yet
  needed at this project's data volume.
- **Shared feature store (e.g. a dedicated feature-serving system)**:
  rejected for now — real infrastructure to operate, unjustified before
  there's a training/serving workload that needs it.
