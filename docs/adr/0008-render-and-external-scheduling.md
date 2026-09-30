# ADR-0008: Hosted Deployment, Take Two — Render and External Scheduling

- **Status**: Accepted
- **Date**: 2026-09-07

## Context

ADR-0007 planned Fly.io for the backend and ML service. It was never
deployed: Fly requires a payment method on file to create an account, and
this project has none and isn't acquiring one. The frontend-on-Vercel half
of that plan is unaffected and stands.

Render.com's free web services need no card. Moving there changes two
things the Fly plan depended on, both real constraints rather than
preferences:

**Free web services sleep after 15 minutes with no inbound traffic**, and
there's no setting to force one always-on the way Fly's
`min_machines_running` did. The backend's in-process `@Scheduled` jobs
(`app.ingest.price.cron`, `app.ingest.weather.cron`,
`app.forecast.refresh-cron`) would silently stop firing while the process
slept — no error, just a quiet gap until the next incoming request happened
to wake it.

**Render's free plan has no "Private Service" resource type.** Only free
Web Services, Postgres, Key Value, and static sites get free instances, and
a free web service can send private-network requests but can't receive
them — so two free services can't reach each other privately at all. The
ML service, which stayed off the public internet entirely on Fly (ADR-0007,
mirroring `infra/docker/compose.yaml`'s "not published at all"), can no
longer be network-invisible for free.

## Decision

**Frontend stays on Vercel**, unchanged from ADR-0007.

**Backend and ML service move to Render**, one Blueprint
(`infra/render/render.yaml`) with two `runtime: docker` web services on the
free plan, building the same `backend/Dockerfile` and
`ml-service/Dockerfile` used everywhere else. No CLI, no login step for the
human running this — connect the repo in Render's dashboard and it deploys
on every push to `main`, same as Vercel already does for the frontend. This
also means the deploy jobs `.github/workflows/ci.yml` had for
`flyctl deploy` are gone; CI is tests only now.

**Scheduling moves out of the backend entirely, into a GitHub Actions
schedule** (`.github/workflows/scheduled-jobs.yml`) calling three new
endpoints:

- `POST /internal/v1/jobs/ingest-price`
- `POST /internal/v1/jobs/ingest-weather`
- `POST /internal/v1/jobs/refresh-forecast`

Each is a thin `JobTriggerController` calling the same
`ingestQuietly()`/`refreshQuietly()` methods the removed `@Scheduled`
methods called — no service-layer changes, and the existing
`PriceIngestionServiceTest`/`WeatherIngestionServiceTest`/
`ForecastRefreshServiceTest` coverage carries over unchanged, since those
tests already exercise the underlying methods directly rather than the
scheduler. The endpoints sit under `/internal/**`, so they're covered by
`SecurityConfig`'s existing rule with no new auth wiring — GitHub Actions
authenticates with the same `INTERNAL_API_USER`/`INTERNAL_API_PASSWORD`
already used for `/internal/v1/price-history`.

This is a real simplification, not a workaround for the sleep problem
alone: the old 07:00/07:10/08:15 stagger existed only to give each
fire-and-forget in-process job room before the next assumed it had run.
Explicit sequential steps in one workflow make the price → weather →
forecast ordering exact instead of implied by clock spacing, and the
backend genuinely doesn't need to be always-on anymore — it only has to be
awake for the length of one triggered call.

**The ML service gets a shared-secret check**, since it's now a public
`onrender.com` URL rather than network-invisible. `POST
/internal/v1/forecast` requires an `X-Internal-Token` header matching
`ML_SERVICE_TOKEN`, read from the environment with no default — same "a
blank fallback is how an internal endpoint ends up open" reasoning
ADR-0006 already used for the backend's own credential. `GET /health` stays
open, for the same reason `/actuator/health` does. `MlForecastClient` sends
the token on every call via a default header, from a new
`app.ml-service.token` property (`APP_MLSERVICE_TOKEN`, also no default).
This is the same "only the backend calls this" invariant from ADR-0002,
enforced at the application layer instead of the network layer because the
network layer is no longer free.

## Consequences

- The daily trigger now pays a cold-start cost the old in-process cron
  never did: `scheduled-jobs.yml`'s first step polls `/actuator/health`
  with retries before triggering anything, to absorb up to roughly a
  minute of wake-up time if the backend was asleep.
- GitHub Actions scheduled workflows aren't perfectly reliable — they can
  lag under platform load, and GitHub auto-disables a schedule after 60
  days with no repository activity. Accepted for a hobby project with
  regular commits; would need revisiting (or `workflow_dispatch` as a
  manual backstop, already added) if the repo ever goes quiet for that
  long.
- Render's 750 free instance-hours/month are shared across every free
  service in one workspace. Two services asleep most of the day
  comfortably fits; keeping both always-on the way the Fly plan intended
  would not have — this is a second, independent reason letting them sleep
  is correct, not merely tolerated.
- `APP_MLSERVICE_TOKEN` and `ML_SERVICE_TOKEN` have to be the same string
  on two separately-configured services, with no cross-check until a
  request actually fails with 401. Documented in
  `infra/render/README.md`'s runbook; a real (small) rotation cost `fly
  secrets set`-style network isolation didn't have.
- Render's free instance is 512MB RAM, against the 256MB ADR-0007 flagged
  as a real OOM risk for the JVM. Not the reason for this switch, but a
  welcome side effect of it.
- Bumping either service to a paid Render plan later would restore access
  to Private Services, at which point the `ML_SERVICE_TOKEN` check could be
  dropped in favor of network isolation again — or simply left in place as
  defence in depth, since it costs little now that it exists.

## Alternatives Considered

- **Keep the in-process cron and paper over sleep with a keep-alive
  ping** (a scheduled workflow just curling `/actuator/health` every ~10
  minutes to prevent Render from ever sleeping). Rejected: it doesn't
  remove the dependency on always-on, it just fights Render's free tier to
  fake one, at higher fragility (a missed ping, GitHub Actions running a
  few minutes late) than explicitly triggering the actual work. It would
  also burn most of the 750-hour monthly budget keeping a process alive
  for the sake of its clock rather than any real traffic.
- **Oracle Cloud's Always Free tier**, genuinely free forever. Rejected:
  it also requires card verification at signup, so it doesn't remove the
  constraint that started this pivot.
- **A custom header scheme richer than a single shared secret for the ML
  service** (HMAC-signed requests, short-lived tokens). Rejected as
  disproportionate: the forecast endpoint has no sensitive data behind it
  (the numbers it returns are derived from prices already public via
  `GET /api/**`), so the actual goal is keeping ADR-0002's "only the
  backend calls this" invariant legible and enforced, not defending a
  secret worth protecting elaborately.

### Update, 2026-09-30: job endpoints report failure

The endpoints above no longer call the `*Quietly()` methods. Those swallowed
every failure, so each endpoint answered `200` and the scheduled run stayed
green on a morning when nothing was collected (RISK-01). They now call
`ingest()`/`refresh()` directly, and `GlobalExceptionHandler` turns a failure
into `502` (no readable price source) or `500` (anything else), which
`curl --fail` turns into a failed run and GitHub into an email to the owner.
The quiet methods remain for the opt-in startup runs, where a failing website
should not stop the backend from booting.
