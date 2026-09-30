# Render.com

`render.yaml` deploys the same two Dockerfiles used by
`infra/docker/compose.yaml` — no separate deploy-only image. The frontend is
not here: it deploys through Vercel's native Git integration, not Render.
See
[`../../docs/adr/0008-render-and-external-scheduling.md`](../../docs/adr/0008-render-and-external-scheduling.md)
for why Render, in place of the Fly.io plan in
[ADR-0007](../../docs/adr/0007-hosted-deployment.md).

## First deploy (one-time, human)

No CLI, no `auth login` — everything happens in Render's dashboard,
authorized against your GitHub account. No payment method needed for two
free web services.

1. [dashboard.render.com](https://dashboard.render.com) → sign in with
   GitHub → authorize Render's GitHub App for this repo.
2. **New** → **Blueprint** → select this repo. When asked for the Blueprint
   file path, enter `infra/render/render.yaml` (Render looks at the repo
   root by default; this is a one-time setting for this project).
3. Render reads the file and prompts for every `sync: false` value:
   - `SUPABASE_DB_URL`, `SUPABASE_DB_USER`, `SUPABASE_DB_PASSWORD` — from
     the Supabase dashboard, Session pooler connection string (same as
     `backend/.env.example`).
   - `INTERNAL_API_USER`, `INTERNAL_API_PASSWORD` — same values as your
     local `backend/.env`, or generate fresh ones
     (`openssl rand -base64 32`) if you'd rather rotate for production.
   - `APP_MLSERVICE_TOKEN` and `ML_SERVICE_TOKEN` — **the same value**,
     generated once (`openssl rand -base64 32`), entered on both services.
4. Deploy. First build pulls base images for both Dockerfiles and takes a
   few minutes.
5. Once both services show **Live**, confirm the ml-service's actual
   `onrender.com` URL matches what's hardcoded in `render.yaml`'s
   `APP_MLSERVICE_BASEURL` (Render normally uses the service name verbatim,
   but a name collision would change it). If it differs, edit
   `render.yaml` and push — the Blueprint auto-redeploys on every push to
   `main`.

For `.github/workflows/scheduled-jobs.yml` to trigger the daily jobs, add
`INTERNAL_API_USER` and `INTERNAL_API_PASSWORD` as GitHub Actions repo
secrets too (Settings → Secrets and variables → Actions) — same values as
step 3.

## What can reach what

| Service | Public | Reachable by |
|---|---|---|
| `giatieuviet-backend` | `https://giatieuviet-backend.onrender.com` | anyone — Vercel's server-side fetches, `GET /api/**`, the GitHub Actions schedule, an operator |
| `giatieuviet-ml-service` | `https://giatieuviet-ml-service.onrender.com` | anyone, technically — but `/internal/v1/forecast` rejects any request without the correct `X-Internal-Token` header (see ADR-0008) |

Unlike the Fly.io plan, the ml-service is **not** network-invisible here:
Render's free plan has no Private Service resource type, and free web
services can't receive private-network traffic even from another service in
the same account. The `ML_SERVICE_TOKEN` check is what stands in for that —
application-layer instead of network-layer, same invariant (ADR-0002: only
the backend calls this).

## Things that will bite

- **Both services sleep after 15 minutes with no inbound traffic**, and
  cold-start in under a minute on the next request. This is expected, not
  a problem to work around — see ADR-0008 for why the daily jobs moved to
  GitHub Actions instead of staying on an in-process cron that needed the
  backend always-on.
- **750 free instance-hours/month, shared across every free service in the
  workspace.** Two services asleep most of the day comfortably fits; two
  services kept artificially always-on would not.
- **Secrets are `sync: false` prompts, never committed values.** The
  non-secret `envVars` in `render.yaml` (the ml-service URL, the CORS
  origin) are fine to commit; nothing else is.
- **`APP_MLSERVICE_TOKEN` and `ML_SERVICE_TOKEN` must be the same string**
  on both services — there's no cross-check at deploy time, only a 401 at
  request time if they drift.
- **Supabase's pooler drops a TLS handshake not finished within about
  2 s**, and on the free CPU a freshly started JVM's first handshake is
  slower than that: the boot failed with `SSL error: Broken pipe` /
  `Remote host terminated the handshake` while the same connection worked
  from a laptop. The backend retries its first connection for 60 s
  (`spring.datasource.hikari.initialization-fail-timeout`); forcing TLS 1.2
  did not help.
- **The backend writes to the real Supabase database** on every deploy
  (Flyway migrations) and every triggered job. Same as the Fly and Docker
  Compose plans.
