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

Scaffolded: Spring Boot 4.1 (Maven wrapper, Java 21) serving the public
API defined in [`docs/api/README.md`](../docs/api/README.md).

**No database yet.** The `service/stub/` implementations return in-memory
data so the API contract and the frontend can be built against something
real-shaped. They are the seam where persistence lands: the interfaces in
`service/` stay, a JPA-backed implementation replaces the stubs, and
neither the controllers nor the DTOs change.

## Running

Requires Java 21 (`java -version`). Maven itself is not needed — the
wrapper fetches it.

```bash
./mvnw test              # unit + web-slice tests
./mvnw spring-boot:run   # http://localhost:8080
curl http://localhost:8080/actuator/health
```

Use `mvnw.cmd` instead of `./mvnw` in PowerShell/cmd.

## Layout

```
src/main/java/com/giatieuviet/backend/
  config/        CORS (origins configurable via app.cors.allowed-origins)
  api/           controllers, one per resource group
  api/dto/       response records mirroring frontend/src/lib/types.ts
  api/error/     RFC 9457 problem responses, decided centrally
  domain/        Granularity, WeatherCondition — wire codes live here
  service/       interfaces the controllers depend on
  service/stub/  TEMPORARY in-memory implementations (see Status)
```

## Configuration

| Property | Default | Purpose |
|---|---|---|
| `app.cors.allowed-origins` | `http://localhost:3000` | Origins allowed to call `/api/**` |
| `management.endpoints.web.exposure.include` | `health,info` | Exposed actuator endpoints |

See [`docs/adr/0002-service-boundaries.md`](../docs/adr/0002-service-boundaries.md)
and [`docs/adr/0003-ml-service-data-access.md`](../docs/adr/0003-ml-service-data-access.md)
for the reasoning behind these boundaries.
