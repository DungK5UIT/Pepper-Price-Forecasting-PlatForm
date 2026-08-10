# Architecture Overview

## System Diagram

```
                     ┌─────────────────┐
                     │  Frontend        │
                     │  (Next.js)       │
                     └────────┬─────────┘
                              │ REST (public API)
                     ┌────────▼─────────┐
                     │  Java Backend     │
                     │  (Spring Boot)    │
                     │  - Auth           │
                     │  - Business logic │
                     │  - API contracts  │
                     │  - Schema owner   │
                     └───┬──────────┬────┘
                         │          │ REST (internal API)
              ┌──────────▼──┐   ┌───▼───────────────┐
              │ PostgreSQL   │   │ Python ML Service  │
              │ (owned by    │   │ (FastAPI)          │
              │  backend)    │   │ - Feature eng.     │
              └──────────────┘   │ - Training          │
                                  │ - Evaluation         │
                                  │ - Forecast generation│
                                  └──────────────────────┘
```

Redis is not part of the current system — see "Deferred Infrastructure"
below.

## Component Responsibilities

### Frontend (`frontend/`)
Next.js + TypeScript. Renders the UI and calls the Java backend's public
REST API. Holds no direct connection to PostgreSQL, Redis, or the ML
service — every data need goes through the backend's API, so the backend
remains the single point of validation and access control.

### Java Backend (`backend/`)
Spring Boot. The system of record and orchestrator:
- Owns the PostgreSQL schema and all migrations (`db/migrations/`).
- Exposes the public REST API consumed by the frontend.
- Owns authentication/authorization.
- Persists users, market prices, data source config, ingestion records,
  forecasts, and job execution records.
- Calls the ML service's internal API when a forecast needs to be
  generated or a model needs to be trained/evaluated, and persists the
  results it gets back.

### Python ML Service (`ml-service/`)
FastAPI. A stateless computation service from the platform's perspective:
- Performs feature engineering over data supplied by the Java backend.
- Trains and evaluates forecasting models.
- Generates forecasts and returns them (plus evaluation metrics) to the
  Java backend.
- Does not persist platform state on its own and is never called directly
  by the frontend.

See ADR-0002 for why the boundary is drawn this way, and ADR-0003 for why
the ML service does not get direct database access.

### PostgreSQL
Single relational store, owned exclusively by the Java backend. All schema
changes go through versioned migrations in `db/migrations/`.

### Redis (deferred)
Not introduced yet. Candidate future uses — response caching, rate
limiting, async job queueing between the backend and ML service — are
real possibilities, but none is implemented until a concrete need is
observed (e.g. a measured latency problem, or a training job that
genuinely needs to run out-of-band). Adding it speculatively would violate
the project's "prove need before building" principle.

### Infra (`infra/`)
Docker Compose definitions and CI configuration for running the stack
locally and building it in CI. Empty until there are services to compose.

## Processing Model

Forecast generation is synchronous REST (backend calls ML service and
waits for a response) until there's a concrete reason — a measured latency
or throughput problem — to move to an async job queue. This is a default,
not a locked decision; revisit if training/inference time makes synchronous
calls impractical.

## Deferred Infrastructure

Explicitly not part of Phase 0, added only when justified by real
next-step work: Redis, Docker Compose files, CI pipelines, object storage
(e.g. MinIO), reverse proxy/HTTPS termination, cloud deployment topology.
