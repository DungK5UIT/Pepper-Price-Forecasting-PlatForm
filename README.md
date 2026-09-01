# Pepper Price Forecasting Platform

A production-oriented forecasting platform for Vietnamese black pepper (tiêu)
prices — built as a long-term software engineering learning project. Beyond
forecasting itself, the project is meant to demonstrate real engineering
practice: clean architecture, tested code, migrations, CI/CD, observability,
and defensible technical decisions.

## Status

**Phase 1 — First vertical slice.** All three services run, on real data:

- **frontend/** — two pages (price dashboard, weather), rendering data
  fetched from the backend's API.
- **backend/** — Spring Boot serving the public API contract in
  [`docs/api/README.md`](docs/api/README.md) from PostgreSQL, with the schema
  owned by Flyway migrations, and orchestrating forecast generation.
- **ml-service/** — FastAPI, generating the forecasts the backend stores. The
  model is honest about its size: it is scored against a naive baseline every
  training run, and the baseline currently wins
  ([ADR-0004](docs/adr/0004-forecasting-model.md)).
- **PostgreSQL** — hosted on Supabase; see
  [`docs/database/README.md`](docs/database/README.md).

Still absent: ingestion jobs — prices and weather are whatever an earlier
prototype collected, and nothing here refreshes them yet — plus auth and
deployment.

## Architecture at a Glance

```
Frontend (Next.js) → Java Backend (Spring Boot) → PostgreSQL
                              ↓ internal API
                       Python ML Service (FastAPI)
```

- **frontend/** — Next.js UI. Talks only to the Java backend's public API.
- **backend/** — Spring Boot. System of record: owns the schema, migrations,
  public API, auth, and orchestrates the ML service.
- **ml-service/** — FastAPI. Stateless ML/data-science computation: feature
  engineering, training, evaluation, forecast generation.
- **db/migrations/** — Versioned schema migrations.
- **infra/** — Deployment and local-orchestration configuration.
- **tests/** — Cross-service integration/e2e tests (unit tests live inside
  each component).

Full detail: [`docs/architecture/overview.md`](docs/architecture/overview.md)
and [`docs/architecture/domain-model.md`](docs/architecture/domain-model.md).

Architecture decisions are recorded in [`docs/adr/`](docs/adr/).

## Development Setup

Backend: `cd backend && ./mvnw spring-boot:run` (JDK 21).
Frontend: `cd frontend && npm install && npm run dev` (Node 20+).

Full detail, including tests: [`docs/development/setup.md`](docs/development/setup.md).

## Documentation

- [`docs/architecture/`](docs/architecture/) — system design, domain model
- [`docs/adr/`](docs/adr/) — architecture decision records
- [`docs/api/`](docs/api/) — API contract (placeholder)
- [`docs/database/`](docs/database/) — schema documentation (placeholder)
