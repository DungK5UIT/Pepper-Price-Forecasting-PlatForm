# Fly.io

`backend.toml` and `ml-service.toml` deploy the same two Dockerfiles used by
`infra/docker/compose.yaml` — no separate deploy-only image. The frontend is
not here: it deploys through Vercel's native Git integration, not Fly. See
[`../../docs/adr/0007-hosted-deployment.md`](../../docs/adr/0007-hosted-deployment.md)
for why.

## First deploy (one-time, human)

`flyctl` and a Fly.io account (with a payment method on file — Fly no longer
has a plan that needs none, though the setup below stays inside its included
Hobby allowance) are required; nothing here can be done from this repo's
sandbox tooling. Run from the repository root:

```bash
fly auth login

fly apps create giatieuviet-backend
fly apps create giatieuviet-ml-service

fly secrets set -a giatieuviet-backend \
  SUPABASE_DB_URL="jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres" \
  SUPABASE_DB_USER="postgres.<project-ref>" \
  SUPABASE_DB_PASSWORD="<from the Supabase dashboard>" \
  INTERNAL_API_USER="ml-service" \
  INTERNAL_API_PASSWORD="$(openssl rand -base64 32)"

# ml-service first — nothing depends on it existing yet. The backend's
# APP_MLSERVICE_BASEURL is a fixed hostname regardless of deploy order.
flyctl deploy --config infra/fly/ml-service.toml
flyctl deploy --config infra/fly/backend.toml
```

Run both `flyctl deploy` commands from the repository root — `context` and
`dockerfile` in each `.toml` are relative to the `.toml` file's own
directory, not the working directory.

Once the Vercel project exists (see the root README / ADR-0007) and its real
domain is known, fix the CORS placeholder committed in `backend.toml`:

```bash
fly secrets set -a giatieuviet-backend \
  APP_CORS_ALLOWED_ORIGINS="https://<actual-domain>.vercel.app"
```

For CI to deploy on every push to `main` (see `.github/workflows/ci.yml`),
generate a deploy token and add it as a GitHub repo secret named
`FLY_API_TOKEN` (GitHub has no CLI here either — use the web UI: repo →
Settings → Secrets and variables → Actions → New repository secret):

```bash
fly tokens create deploy -x 999999h -a giatieuviet-backend
```

## What can reach what

| Service | Public | Reachable by |
|---|---|---|
| `giatieuviet-backend` | `https://giatieuviet-backend.fly.dev` | anyone — Vercel's server-side fetches, `GET /api/**`, an operator |
| `giatieuviet-ml-service` | not published | `giatieuviet-backend` only, over Fly's private network at `giatieuviet-ml-service.internal:8000` |

This is a real change from `infra/docker/compose.yaml`'s local topology,
where the backend binds to `127.0.0.1` because frontend and backend share
one Docker host. Once the frontend moves to Vercel — a different network —
the backend has to become a public HTTPS endpoint for Vercel's server-side
fetches to reach it at all. See ADR-0007.

## Things that will bite

- **Secrets are `fly secrets set`, never `[env]` in the `.toml`.** The
  `.toml` files are committed; `[env]` in them is for non-secret config
  only (the ml-service address, the CORS origin).
- **The backend must stay always-on.** `app.ingest.price.cron`,
  `app.ingest.weather.cron`, and `app.forecast.refresh-cron` are in-process
  `@Scheduled` jobs — a machine that scales to zero stops them silently,
  with no error, until the next request happens to wake it.
  `min_machines_running = 1` and `auto_stop_machines = false` in
  `backend.toml` exist specifically to prevent this.
- **256MB may not be enough for the backend.** Watch
  `fly logs -a giatieuviet-backend` for OOM kills after the first deploy;
  see the comment in `backend.toml` and ADR-0007 for the fallback.
- **The ml-service has no public hostname at all**, not a hostname that
  merely returns 404/403. `curl https://giatieuviet-ml-service.fly.dev`
  should fail to connect, not return an HTTP error — that's what to check
  in the smoke test, not "does it 404."
