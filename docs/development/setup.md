# Development Setup

Two components run today: the backend (Spring Boot) and the frontend
(Next.js). Neither needs a database yet — both serve stub data — so there
is nothing to install beyond the two runtimes.

## Prerequisites

| Tool | Version | Used by |
|---|---|---|
| JDK | 21 | `backend/` (Maven itself not needed — the wrapper fetches it) |
| Node.js | 20+ | `frontend/` |
| Python | 3.13 | `ml-service/` |

The database is hosted (Supabase), so there is nothing to install for it — only
credentials to configure.

## Backend

Needs database credentials — copy `backend/.env.example` to `backend/.env`
and fill in the Supabase password (Project Settings → Database → Connection
string → **Session pooler**). `.env` is git-ignored.

```bash
cd backend
./mvnw test              # no credentials needed: tests run on in-memory H2
./mvnw spring-boot:run   # http://localhost:8080
```

The first run applies the Flyway migrations.

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

## ML service

```bash
cd ml-service
pip install -r requirements-dev.txt
pytest                                            # no credentials needed
python -m uvicorn ml_service.main:app --port 8000
```

Retraining needs the backend up, since that is where training data comes from:
`python -m ml_service.train`. See [`../../ml-service/README.md`](../../ml-service/README.md).

## Running everything

Three terminals. The frontend needs the backend; the backend only needs the ML
service when it refreshes forecasts, and keeps serving the stored run if it is
down.

```bash
cd backend    && ./mvnw spring-boot:run                        # terminal 1
cd ml-service && python -m uvicorn ml_service.main:app --port 8000   # terminal 2
cd frontend   && npm run dev                                   # terminal 3
```

To regenerate forecasts immediately instead of waiting for the daily schedule:
`./mvnw spring-boot:run -Dspring-boot.run.arguments=--app.forecast.refresh-on-startup=true`.

There is no orchestration (`infra/docker/`) yet.

## Not applicable yet

PostgreSQL/Redis setup, the ML service, and full-stack orchestration land
with the components that need them.
