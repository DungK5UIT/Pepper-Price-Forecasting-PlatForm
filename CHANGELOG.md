# Changelog

Notable changes, newest first, grouped by milestone. There are no release
tags yet; dates are when the work landed on `main`.

## Unreleased

### Documentation
- Requirements (35 functional, 13 non-functional), test-case traceability
  (73 cases, gaps named) and a risk register.
- `CONTRIBUTING.md` with the workflow and coding rules; pull request template.
- One agent brief (`AGENTS.md`, imported by `CLAUDE.md`); stale READMEs fixed.

### Changed
- Removed the empty `db/`, `scripts/` and `tests/` root directories (ADR-0001 update).

## 2026-09-07 — Hosted deployment

### Added
- Dockerfiles for all three services and a Compose file with one public port
  and two private services.
- Render Blueprint for the backend and ML service, Vercel for the frontend (ADR-0008).
- CI on every push and pull request: backend tests, ML tests, frontend lint and build.
- Daily jobs triggered by a GitHub Actions schedule through `/internal/v1/jobs/*`.
- Shared-secret header on the ML service's forecast endpoint.

### Changed
- In-process cron replaced by the external schedule, since free hosted
  services sleep when idle.
- CORS origin configurable per environment.
- The Fly.io plan (ADR-0007) superseded before it was deployed: it required a card.

## 2026-09-05 — Trading-day time

### Fixed
- Every date anchored to `Asia/Ho_Chi_Minh` through an injected `Clock`, so a
  UTC container no longer dates the morning's prices to the previous day.

## 2026-09-04 — The platform collects its own data

### Added
- Daily price collection from two public sites, read on every run and
  cross-checked, with a plausibility gate and idempotent writes (ADR-0005).
- Daily weather collection for six provinces from Open-Meteo.
- `ingestion_run` log and a `STALE` health status when a job goes quiet.
- HTTP Basic machine credential on `/internal/**` and the health detail (ADR-0006).
- Monthly price history back to 2005; the model re-measured on 261 months —
  the naive baseline still wins (ADR-0004 update).

### Fixed
- Unknown paths answer `404` instead of `500`.
- Startup jobs run in the same order as the morning schedule.

## 2026-09-01 — First vertical slice

### Added
- Next.js dashboard and weather pages.
- Spring Boot public API serving PostgreSQL through Flyway-owned schema.
- FastAPI ML service generating quantile forecasts, scored against a naive
  baseline on every training run (ADR-0004); backend refresh that stores them.

## 2026-08-10 — Foundation

### Added
- Monorepo skeleton, architecture overview, domain model, ADRs 0001–0003.
