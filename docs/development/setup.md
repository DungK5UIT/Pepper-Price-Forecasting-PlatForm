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

The frontend reads everything from the backend (`src/lib/api.ts`), so
**the backend must be running** or pages fall back to their error state —
including during `npm run build`, which prerenders them.

Point it at a different backend with `API_BASE_URL` (see
`frontend/.env.example`); it defaults to `http://localhost:8080`.

## Running both

Two terminals, backend first:

```bash
cd backend  && ./mvnw spring-boot:run   # terminal 1
cd frontend && npm run dev              # terminal 2
```

There is no orchestration (`infra/docker/`) yet — nothing needs composing
until a database is introduced.

## Not applicable yet

PostgreSQL/Redis setup, the ML service, and full-stack orchestration land
with the components that need them.
