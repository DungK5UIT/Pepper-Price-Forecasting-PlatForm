# Development Setup

Three components run today: the backend (Spring Boot), the frontend
(Next.js), and the ML service (FastAPI). The backend needs database
credentials; the other two need nothing beyond their runtime.

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

The first run applies the Flyway migrations. Collection is off at startup by
default — a boot should not depend on public websites being reachable.

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

To do a morning's work immediately instead of waiting for the schedule:

```bash
# collect today's prices and weather (reaches two public sites and Open-Meteo)
./mvnw spring-boot:run -Dspring-boot.run.arguments=--app.ingest.run-on-startup=true
# regenerate the forecast (needs the ML service up)
./mvnw spring-boot:run -Dspring-boot.run.arguments=--app.forecast.refresh-on-startup=true
```

Both write to the real database, and both are idempotent — running them twice
in a day corrects rows rather than duplicating them.

There is no orchestration (`infra/docker/`) yet.

## Daily schedule

The backend runs these on its own once it is up, all times local:

| Time | Job | What it does |
|---|---|---|
| 07:00 | `price_ingestion` | Scrapes today's regional prices, falls back to a second site |
| 07:10 | `weather_ingestion` | Reads Open-Meteo for the six growing provinces |
| 08:15 | forecast refresh | Calls the ML service and stores the run |

Every collection attempt lands in `ingestion_run` with a status and a detail.
You do not have to query it by hand:

```bash
curl -s http://localhost:8080/actuator/health | jq .components.ingestion
```

reports `fresh` or `stale` per job, with the last successful run. The
aggregate status turns `STALE` — still HTTP 200, since the service itself is
healthy — when a job has been quiet for more than 26 hours. See
[`../adr/0005-data-ingestion.md`](../adr/0005-data-ingestion.md).

## Not applicable yet

Redis and full-stack orchestration land with the components that need them.
