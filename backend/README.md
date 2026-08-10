# Backend

Java + Spring Boot service. The platform's system of record and
orchestrator.

**Owns**: the public REST API, authentication/authorization, the
PostgreSQL schema and its migrations (coordinated with
`db/migrations/`), persistence of users/market prices/data
sources/ingestion records/forecasts/job execution records, and
orchestration of calls to the ML service's internal API.

**Does not own**: ML computation (feature engineering, training,
evaluation) — that's `ml-service/`'s responsibility, invoked over an
internal API rather than reimplemented here.

Not yet scaffolded. Will be initialized (Spring Initializr or equivalent)
when backend work actually begins. See `docs/adr/0002-service-boundaries.md`
and `docs/adr/0003-ml-service-data-access.md` for the reasoning behind
these boundaries.
