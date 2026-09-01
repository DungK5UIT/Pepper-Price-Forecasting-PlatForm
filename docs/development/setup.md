# Development Setup

Two components run today: the backend (Spring Boot) and the frontend
(Next.js). Neither needs a database yet — both serve stub data — so there
is nothing to install beyond the two runtimes.

## Prerequisites

| Tool | Version | Used by |
|---|---|---|
| JDK | 21 | `backend/` (Maven itself not needed — the wrapper fetches it) |
| Node.js | 20+ | `frontend/` |

## Backend

```bash
cd backend
./mvnw test              # unit + web-slice tests
./mvnw spring-boot:run   # http://localhost:8080
```

Smoke check: `curl http://localhost:8080/actuator/health` returns
`{"status":"UP"}`. The API contract is in
[`../api/README.md`](../api/README.md).

On PowerShell/cmd use `mvnw.cmd` instead of `./mvnw`.

## Frontend

```bash
cd frontend
npm install
npm run dev              # http://localhost:3000
npm run build
npm run lint
```

The frontend currently renders its own mock data
(`frontend/src/lib/mock-data.ts`) and does not call the backend yet, so
the two can be run independently. When they are wired together, the
backend already allows `http://localhost:3000` as a CORS origin
(`app.cors.allowed-origins`).

## Running both

Two terminals, one per component — there is no orchestration
(`infra/docker/`) yet, because nothing needs to be composed together
until a database is introduced.

## Not applicable yet

PostgreSQL/Redis setup, the ML service, and full-stack orchestration land
with the components that need them.
