# Pepper Price Forecasting Platform

Production-oriented forecasting platform for Vietnamese black pepper (tiêu)
prices. Simultaneously a real product and a long-term software engineering
learning project — architecture, testing, Git history, and deployment
practice are treated as first-class, not incidental.

Global engineering workflow, git conventions, testing philosophy, and
review standards come from the Software Engineer OS (`~/.claude`, sourced
from `D:\Claude\Software-Engineer-OS`) and apply here without restatement.

## Current Phase

Phase 0 — foundation only. Repository structure and initial ADRs exist; no
application code, schema, or deployment yet. See `docs/architecture/` for
system design and `docs/adr/` for recorded decisions.

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
