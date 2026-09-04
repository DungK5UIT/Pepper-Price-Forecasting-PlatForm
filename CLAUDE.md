# Pepper Price Forecasting Platform

Production-oriented forecasting platform for Vietnamese black pepper (tiêu)
prices. Simultaneously a real product and a long-term software engineering
learning project — architecture, testing, Git history, and deployment
practice are treated as first-class, not incidental.

Global engineering workflow, git conventions, testing philosophy, and
review standards come from the Software Engineer OS (`~/.claude`, sourced
from `D:\Claude\Software-Engineer-OS`) and apply here without restatement.

## Current Phase

Phase 1 — all three services running on real data. `frontend/` (Next.js)
fetches from `backend/` (Spring Boot 4.1 + Java 21) over the contract in
`docs/api/README.md`; the backend reads PostgreSQL on Supabase (schema owned by
Flyway migrations in `backend/src/main/resources/db/migration/`) and calls
`ml-service/` (FastAPI) to regenerate forecasts, which it persists.

The forecasting model is deliberately modest and measured against a naive
baseline — see `docs/adr/0004-forecasting-model.md`; the baseline currently
wins and is what ships.

Prices and weather are collected daily by the backend itself (ADR-0005),
ahead of the forecast refresh that consumes them; every attempt is logged to
`ingestion_run`.

`/internal/**` and the actuator detail sit behind a machine credential
(ADR-0006); the public read API stays open.

Not built yet: user accounts, deployment, and anything that actually sends an
alert when a collection run fails.

See `docs/architecture/` for system design and `docs/adr/` for recorded
decisions.

## Stack Direction

- Frontend: Next.js + TypeScript
- Backend: Java + Spring Boot + Spring Data JPA + PostgreSQL
- ML service: Python + FastAPI + pandas/NumPy/scikit-learn
- Infra: Docker Compose, Redis (introduced only on proven need)

## Boundaries (see ADRs for rationale)

- Frontend calls only the Java backend's public API — never Postgres or the
  ML service directly.
- The Java backend owns the schema, migrations, and all persistence.
- The Python ML service has no direct database access; it exchanges data
  with the Java backend through an internal API.
