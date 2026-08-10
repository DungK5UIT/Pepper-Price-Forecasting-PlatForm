# Pepper Price Forecasting Platform

A production-oriented forecasting platform for Vietnamese black pepper (tiêu)
prices — built as a long-term software engineering learning project. Beyond
forecasting itself, the project is meant to demonstrate real engineering
practice: clean architecture, tested code, migrations, CI/CD, observability,
and defensible technical decisions.

## Status

**Phase 0 — Foundation.** The repository structure, component boundaries,
and initial architecture decisions are in place. No application code,
schema, or deployment exists yet.

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

Not yet applicable — no service has code to run. See
[`docs/development/setup.md`](docs/development/setup.md), which will be
filled in as each component is scaffolded.

## Documentation

- [`docs/architecture/`](docs/architecture/) — system design, domain model
- [`docs/adr/`](docs/adr/) — architecture decision records
- [`docs/api/`](docs/api/) — API contract (placeholder)
- [`docs/database/`](docs/database/) — schema documentation (placeholder)
