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

## Status

Spring Boot 4.1 (Maven wrapper, Java 21) serving the public API defined in
[`docs/api/README.md`](../docs/api/README.md) from PostgreSQL (Supabase).

Schema and data both belong to this service: Flyway migrations in
`src/main/resources/db/migration/` create the tables and imported the history
an earlier prototype had already collected in the same database. See
[`docs/database/README.md`](../docs/database/README.md).

## Running

Requires Java 21 (`java -version`) and database credentials. Maven itself is
not needed — the wrapper fetches it.

```bash
cp .env.example .env     # then fill in SUPABASE_DB_PASSWORD
./mvnw test              # no credentials needed: tests use in-memory H2
./mvnw spring-boot:run   # http://localhost:8080
curl http://localhost:8080/actuator/health
```

Use `mvnw.cmd` instead of `./mvnw` in PowerShell/cmd.

On first run against a database that already contains other tables, Flyway
baselines at version 0 and then applies V1 onwards.

## Layout

```
src/main/java/com/giatieuviet/backend/
  config/        CORS (origins configurable via app.cors.allowed-origins)
  api/           controllers, one per resource group
  api/dto/       response records mirroring frontend/src/lib/types.ts
  api/error/     RFC 9457 problem responses, decided centrally
  domain/        Granularity, WeatherCondition — wire codes live here
  forecast/      calls the ML service and stores the run it returns
  internal/      endpoints for the ML service, not the public contract
  persistence/   JPA entities and Spring Data repositories
  service/       interfaces the controllers depend on
  service/db/    the implementations that read PostgreSQL
src/main/resources/db/migration/   Flyway migrations
```

## Configuration

| Property / variable | Default | Purpose |
|---|---|---|
| `SUPABASE_DB_URL` | — | JDBC URL, session pooler (port 5432) |
| `SUPABASE_DB_USER` | — | Database user |
| `SUPABASE_DB_PASSWORD` | — | Database password — from `.env`, never committed |
| `app.ml-service.base-url` | `http://localhost:8000` | Where the ML service listens |
| `app.forecast.refresh-cron` | `0 15 8 * * *` | When forecasts are regenerated |
| `app.forecast.refresh-on-startup` | `false` | Regenerate once at boot, for local runs |
| `app.cors.allowed-origins` | `http://localhost:3000` | Origins allowed to call `/api/**` |
| `management.endpoints.web.exposure.include` | `health,info` | Exposed actuator endpoints |

`.env` next to the pom is imported automatically when present, so deployments
can inject the same variables from the environment instead.

See [`docs/adr/0002-service-boundaries.md`](../docs/adr/0002-service-boundaries.md)
and [`docs/adr/0003-ml-service-data-access.md`](../docs/adr/0003-ml-service-data-access.md)
for the reasoning behind these boundaries.
