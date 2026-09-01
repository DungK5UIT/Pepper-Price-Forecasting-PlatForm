# Pepper Price Forecasting Platform

Production-oriented forecasting platform for Vietnamese black pepper (tiêu)
prices. Simultaneously a real product and a long-term software engineering
learning project — architecture, testing, Git history, and deployment
practice are treated as first-class, not incidental.

Global engineering workflow, git conventions, testing philosophy, and
review standards come from the Software Engineer OS (`~/.Codex`, sourced
from `D:\Codex\Software-Engineer-OS`) and apply here without restatement.

## Current Phase

Phase 1 — all three services running on real data. `frontend/` (Next.js)
fetches from `backend/` (Spring Boot 4.1 + Java 21) over the contract in
`docs/api/README.md`; the backend reads PostgreSQL on Supabase (schema owned by
Flyway migrations in `backend/src/main/resources/db/migration/`) and calls
`ml-service/` (FastAPI) to regenerate forecasts, which it persists.

The forecasting model is deliberately modest and measured against a naive
baseline — see `docs/adr/0004-forecasting-model.md`; the baseline currently
wins and is what ships.

Not built yet: ingestion jobs (prices and weather are still whatever an earlier
prototype collected), auth, deployment.
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
