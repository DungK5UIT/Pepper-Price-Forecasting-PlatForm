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

Needs three credentials — copy `backend/.env.example` to `backend/.env` and
fill them in. `.env` is git-ignored.

- `SUPABASE_DB_PASSWORD` — Project Settings → Database → Connection string →
  **Session pooler**.
- `INTERNAL_API_PASSWORD` — any long random string you generate
  (`openssl rand -base64 32`). It guards `/internal/**`; there is no default,
  so the application will not start until it is set.
- `APP_MLSERVICE_TOKEN` — same kind of value, generated the same way. Sent to
  the ML service on every forecast call; the same value has to be set as
  `ML_SERVICE_TOKEN` in the ML service's own environment (below), or every
  call gets a 401.

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
**the backend must be running** or pages fall back to their error state.
`npm run build` does not need it: both pages render per request.

Point it at a different backend with `API_BASE_URL` (see
`frontend/.env.example`); it defaults to `http://localhost:8080`.

## ML service

```bash
cd ml-service
pip install -r requirements-dev.txt
pytest                                            # no credentials needed

export ML_SERVICE_TOKEN=...   # same value as backend/.env's APP_MLSERVICE_TOKEN
python -m uvicorn ml_service.main:app --port 8000
```

Retraining needs the backend up, since that is where training data comes from,
and the same internal credential the backend was started with:

```bash
export INTERNAL_API_USER=ml-service
export INTERNAL_API_PASSWORD=...      # the value in backend/.env
python -m ml_service.train
``` See [`../../ml-service/README.md`](../../ml-service/README.md).

## Running everything

Three terminals. The frontend needs the backend; the backend only needs the ML
service when it refreshes forecasts, and keeps serving the stored run if it is
down.

```bash
cd backend    && ./mvnw spring-boot:run                              # terminal 1
cd ml-service && export ML_SERVICE_TOKEN=... && python -m uvicorn ml_service.main:app --port 8000   # terminal 2
cd frontend   && npm run dev                                         # terminal 3
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

## Everything at once, in containers

The three terminals above are the fast loop for editing code. To run the
platform the way it is deployed:

```bash
cp infra/docker/.env.example infra/docker/.env    # fill in the values
docker compose -f infra/docker/compose.yaml --env-file infra/docker/.env up --build
```

From the repository root. Only the frontend is published; the backend binds to
`127.0.0.1:8080` for operator access and the ML service is not published at
all. See [`../../infra/docker/README.md`](../../infra/docker/README.md).

Note that this writes to the real Supabase database, exactly as production
would.

## Daily schedule

The backend does not run these on its own — a GitHub Actions schedule
(`.github/workflows/scheduled-jobs.yml`) calls them once a day, since the
hosted backend sleeps when idle and an in-process cron would silently stop
firing (ADR-0008). Locally, trigger the same endpoints by hand:

```bash
curl -X POST -u "$INTERNAL_API_USER:$INTERNAL_API_PASSWORD" \
  http://localhost:8080/internal/v1/jobs/ingest-price      # regional prices, falls back to a second site
curl -X POST -u "$INTERNAL_API_USER:$INTERNAL_API_PASSWORD" \
  http://localhost:8080/internal/v1/jobs/ingest-weather    # Open-Meteo, six growing provinces
curl -X POST -u "$INTERNAL_API_USER:$INTERNAL_API_PASSWORD" \
  http://localhost:8080/internal/v1/jobs/refresh-forecast  # calls the ML service, stores the run
```

or use `--app.ingest.run-on-startup=true` / `--app.forecast.refresh-on-startup=true`
(above) to run them once as part of booting the backend.

Every collection attempt lands in `ingestion_run` with a status and a detail.
You do not have to query it by hand:

```bash
curl -su "$INTERNAL_API_USER:$INTERNAL_API_PASSWORD"   http://localhost:8080/actuator/health | jq .components.ingestion
```

reports `fresh` or `stale` per job, with the last successful run. Without the
credential the endpoint still answers, with the aggregate status only. The
aggregate status turns `STALE` — still HTTP 200, since the service itself is
healthy — when a job has been quiet for more than 26 hours. See
[`../adr/0005-data-ingestion.md`](../adr/0005-data-ingestion.md).

## Not applicable yet

Redis lands only when a measured need appears (see
[`../architecture/overview.md`](../architecture/overview.md), "Deferred
Infrastructure").
