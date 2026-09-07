# Docker

`compose.yaml` runs the whole platform — frontend, backend, ML service — the
way it is meant to be deployed. There is no PostgreSQL service: the database
is Supabase, and a local one would be a different database with a different
migration history (see [`../../docs/database/README.md`](../../docs/database/README.md)).

## Running it

```bash
cp infra/docker/.env.example infra/docker/.env    # then fill in both passwords
docker compose -f infra/docker/compose.yaml --env-file infra/docker/.env up --build
```

From the repository root, not from this directory — the build contexts are
relative to it. Then open <http://localhost:3000>.

The first build pulls three base images and takes a while. After that, layers
are cached on the lockfile and the pom, so a source edit rebuilds only the
application layer.

## What can reach what

This is the point of the file, not an accident of it:

| Service | Published | Reachable by |
|---|---|---|
| `frontend` | `:3000` | anyone — the only thing a browser talks to |
| `backend` | `127.0.0.1:8080` | the frontend, plus the operator on this machine |
| `ml-service` | not published | the backend only |

The frontend fetches entirely in Server Components, so a browser never calls
the backend at all. That is what lets the backend bind to localhost here and
lets `/internal/**` stay off the internet entirely on a host — a stronger
guarantee than the credential in front of it (ADR-0006), which remains as
defence in depth.

Reproduce that shape wherever this is deployed: **one public port, two private
services**.

## Images

| | Base | Notes |
|---|---|---|
| `backend` | `eclipse-temurin:21` → `21-jre` | Two stages; tests run in CI, not during the build. Heap sized from the container limit, not the host's memory. |
| `ml-service` | `python:3.13-slim` | Single stage — the dependencies are wheels with nothing to compile. Ships the committed model artifact, so it needs no training run and no database. |
| `frontend` | `node:22-alpine` | Three stages, `output: "standalone"`. Needs no backend at build time: both pages render per request. |

All three run as an unprivileged user.

## Things that will bite

- **Timezone.** Every image sets `TZ=Asia/Ho_Chi_Minh` for readable logs, but
  the backend does not depend on it — `app.time-zone` pins the dates it
  stores. Do not remove one assuming the other covers it.
- **`depends_on` is ordering, not readiness.** A request arriving before the
  backend is up renders the error boundary and recovers on refresh.
- **Secrets are environment variables, never image layers.** `.dockerignore`
  excludes every `.env`; keep it that way.
- **The backend writes to the real Supabase database.** Bringing this up runs
  the same migrations and, if collection is enabled, the same collection jobs
  as production.
