# Backend

Java + Spring Boot service. The platform's system of record and
orchestrator.

**Owns**: the public REST API, authentication/authorization, the
PostgreSQL schema and its migrations (coordinated with
`db/migrations/`), persistence of users/market prices/data
sources/ingestion records/forecasts/job execution records, the daily
collection of prices and weather, and orchestration of calls to the ML
service's internal API.

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

Since [ADR-0005](../docs/adr/0005-data-ingestion.md) it also collects that data
itself — prices scraped from two public sites, weather from Open-Meteo — each
morning ahead of the forecast refresh, logging every attempt to
`ingestion_run`.

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
  ingest/        daily collection of prices and weather, and its run log
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
| `INTERNAL_API_USER` | — | Username the ML service presents to `/internal/**` |
| `INTERNAL_API_PASSWORD` | — | Its password. No default — the app refuses to start without one |
| `app.ingest.price.cron` | `0 0 7 * * *` | When prices are collected |
| `app.ingest.weather.cron` | `0 10 7 * * *` | When weather is collected |
| `app.ingest.run-on-startup` | `false` | Collect once at boot, for local runs |
| `app.ingest.staleness-threshold` | `PT26H` | How long a job may go quiet before health reports `STALE` |
| `app.ml-service.base-url` | `http://localhost:8000` | Where the ML service listens |
| `app.forecast.refresh-cron` | `0 15 8 * * *` | When forecasts are regenerated |
| `app.forecast.refresh-on-startup` | `false` | Regenerate once at boot, for local runs |
| `app.cors.allowed-origins` | `http://localhost:3000` | Origins allowed to call `/api/**` |
| `management.endpoints.web.exposure.include` | `health,info` | Exposed actuator endpoints |

## Access

| Surface | Who |
|---|---|
| `GET /api/**` | Anyone. Read-only, and the prices on it are already published by their sources. |
| `GET /actuator/health` | Anyone, so an uptime check needs no credential — but the per-component detail only shows to an authenticated caller. |
| `/internal/**` | The `INTERNAL` role only, over HTTP Basic. This is the ML service's training-data pull, which returns the whole price series in one request. |
| anything else | Closed. |

There are no user accounts and no mutating endpoints; the single credential is
a machine account, not a person. See
[`docs/adr/0006-internal-api-access.md`](../docs/adr/0006-internal-api-access.md).

`GET /actuator/health` reports an `ingestion` component alongside the usual
database and disk checks: when a collection job has not succeeded within the
staleness threshold the aggregate status becomes `STALE`. It stays HTTP 200
on purpose — the process is fine, its data is not — so a check has to read the
body rather than the status code. The breakdown needs the internal credential:

```bash
curl -su "$INTERNAL_API_USER:$INTERNAL_API_PASSWORD" localhost:8080/actuator/health
```

`.env` next to the pom is imported automatically when present, so deployments
can inject the same variables from the environment instead.

See [`docs/adr/0002-service-boundaries.md`](../docs/adr/0002-service-boundaries.md)
and [`docs/adr/0003-ml-service-data-access.md`](../docs/adr/0003-ml-service-data-access.md)
for the reasoning behind these boundaries.
