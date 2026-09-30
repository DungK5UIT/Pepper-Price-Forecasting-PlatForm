# Pepper Price Forecasting Platform — brief for coding agents

A forecasting platform for Vietnamese black pepper (tiêu) prices: it collects
the daily domestic price and the weather in the growing provinces, keeps the
history, and publishes a short-term forecast with an honest uncertainty band.
It is also a long-term software-engineering portfolio project, so
architecture, tests, Git history and documentation are part of the product.

## Where things are

| Path | What |
|---|---|
| `frontend/` | Next.js + TypeScript. Server-renders two pages from the backend's public API. |
| `backend/` | Spring Boot 4.1, Java 21. Public API, access control, schema (Flyway, `src/main/resources/db/migration/`), daily collection, forecast orchestration. |
| `ml-service/` | FastAPI, Python 3.13. Stateless forecasting; training CLI. |
| `infra/` | Docker Compose (local), Render Blueprint (hosted). |
| `.github/workflows/` | CI (tests for all three) and the daily job trigger. |
| `docs/requirements.md` | What the system must do — FR/NFR with IDs. |
| `docs/test-cases.md` | Which test verifies which requirement, and what is missing. |
| `docs/risks.md` | Known risks and their status. |
| `docs/adr/` | Every significant decision, with alternatives. |
| `docs/api/`, `docs/database/`, `docs/architecture/` | API contract, schema, system design. |

## Commands

```bash
cd backend    && ./mvnw test        # mvnw.cmd on Windows; H2, no credentials
cd ml-service && pytest             # no network, no credentials
cd frontend   && npm run lint && npm run build
```

Running locally and the credentials each service needs:
`docs/development/setup.md`.

## Current state

All three services run on real data, with PostgreSQL on Supabase.
Deployment is configured for Vercel (frontend) and Render's free plan
(backend, ML service) — `infra/render/README.md`. A GitHub Actions schedule
triggers price collection, weather collection and the forecast refresh every
morning at 07:00 Vietnam time (ADR-0008). The naive baseline
beats the gradient-boosting model and is what ships (ADR-0004). Open risks —
start with RISK-01 to RISK-05 — are in `docs/risks.md`.

## Rules

- Work the way `CONTRIBUTING.md` describes: understand → plan (risks, test
  cases) → build under coding rules R1–R13 → review, static analysis, unit,
  integration.
- Boundaries: the frontend calls only the backend's public API; the backend
  owns the schema and all persistence; the ML service has no database access
  (ADR-0002, ADR-0003).
- A behaviour change updates `docs/requirements.md` and `docs/test-cases.md`
  in the same PR; a significant decision gets an ADR.
- Never commit `.env` files or credentials. Every credential is required at
  startup and has no default.
