# ADR-0007: Hosted Deployment — Vercel and Fly.io

- **Status**: Accepted
- **Date**: 2026-09-07

## Context

Everything up to this point runs on one machine. `infra/docker/compose.yaml`
publishes the frontend, binds the backend to `127.0.0.1`, and does not
publish the ML service at all — correct only because all three share one
Docker host and the frontend fetches the backend server-side, over the
internal Compose network. That shape has no meaning once the project is
meant to be reachable by someone who is not running it locally, which is the
actual point of building it as a portfolio piece rather than an exercise.

There is no budget for this. Whatever hosts it has to stay at $0, which
rules out a VPS and anything billed by the hour regardless of load.

The frontend needs nothing beyond what Next.js's Server Components require
at request time — no persistent process, no state. Vercel's free tier is
built for exactly that and needs no container. The backend and ML service
are already two Dockerfiles, container-shaped by construction — Fly.io runs
them as committed, with no rewrite, on its Hobby allowance (three
`shared-cpu-1x-256mb` machines included). Fly no longer has a plan that
needs no payment method on file, but staying inside that allowance keeps the
bill at $0 unless the allowance itself changes later.

## Decision

**Frontend on Vercel, backend and ML service on Fly.io, one Fly app each.**
Fly was chosen over Render and Cloud Run specifically for its private
networking (any app can resolve any other app in the same org at
`<name>.internal`, with zero extra configuration) — it reproduces the "ML
service reachable only by the backend" boundary this project already
committed to in ADR-0006 and `infra/docker/compose.yaml`, rather than
requiring a separate VPC or firewall product bolted on afterward.

**No custom deploy step for the frontend.** Vercel's Git integration deploys
production on push to `main` and a preview on every pull request, for free,
with none of that built here.

**Fly config lives at `infra/fly/`, not inside `backend/` or `ml-service/`.**
`infra/` is already this repo's one place for deployment topology
(`infra/docker/`); putting `backend.toml` and `ml-service.toml` there keeps
each service directory deploy-target-agnostic and keeps every deploy target
discoverable from one place.

**The backend becomes a public HTTPS endpoint.** This is the one real
topology change here, not an oversight: Vercel's server-side fetches run on
Vercel's network, not the backend's, so `127.0.0.1` binding stops applying
the moment the two are no longer co-located. Fly terminates TLS at its edge
(`force_https = true` in `backend.toml`), which is what turns ADR-0006's
"deployment must terminate TLS in front of the backend" from a documented
intention into an actual guarantee — Basic Auth on `/internal/**` still
depends on it.

**The ML service stays private.** `ml-service.toml` declares no
`[http_service]` block at all, so Fly allocates it no public hostname —
only the private one, `giatieuviet-ml-service.internal:8000`, resolvable by
other apps in the same org and nothing else. This reproduces
`infra/docker/compose.yaml`'s "not published at all" on infrastructure that
does not share a Docker network with anything.

**Both Fly apps run in `sin` (Singapore)**, matching the Supabase project's
`ap-southeast-1` region, so the backend's connection-pooled queries — the
thing it does on nearly every request — don't cross a region each time.

**The backend stays always-on**: `min_machines_running = 1`,
`auto_stop_machines = false`. `app.ingest.price.cron`,
`app.ingest.weather.cron`, and `app.forecast.refresh-cron` are in-process
`@Scheduled` jobs; a machine that scales to zero stops them with no error,
silently, until something happens to wake it back up. The ML service has no
equivalent setting to make, because a service with no public proxy in front
of it has nothing to apply autostop to — once `flyctl deploy` starts it, it
simply keeps running, which happens to be exactly the no-cold-start behavior
wanted for a demo anyway.

## Consequences

- Two always-on `shared-cpu-1x-256mb` machines fit inside Fly's Hobby
  allowance with one machine of headroom. This should stay $0/month barring
  a change to Fly's pricing terms — worth re-checking against Fly's current
  pricing at deploy time, not assumed permanent.
- 256MB is a real risk for the backend specifically: Spring Boot, JPA,
  Flyway, and Spring Security together are not a small footprint, and
  `-XX:MaxRAMPercentage=75` sizes the heap from whatever the container is
  given, not from a size already known to be enough. If it OOM-kills in
  practice, the documented fallback is `memory = "512mb"` in `backend.toml`
  — outside the free allowance, roughly $2-3/month — a small, explicit,
  revisitable cost rather than a silently restarting backend.
- `app.cors.allowed-origins` needs a real value once the backend is public,
  even though nothing calls it cross-origin yet (the frontend still fetches
  server-side only). Left wrong, it would be a one-line reason a future
  browser-side fetch quietly fails with no useful error.
- The ML service having no public hostname means there is no Fly-managed
  HTTP health check for it either — that is an `[http_service]` feature.
  Its health is observed through the backend's existing graceful
  degradation (it keeps serving the last stored forecast if the ML service
  is unreachable) and through the post-deploy smoke test in
  `infra/fly/README.md`, not an automated probe.
- CI (`.github/workflows/ci.yml`) is introduced alongside this ADR rather
  than separately — a deploy pipeline with no test gate in front of it is a
  worse decision than either piece shipped alone.

## Alternatives Considered

- **Render or Google Cloud Run instead of Fly.io.** Both would run the same
  Dockerfiles. Rejected in favor of Fly for the reason in the Decision
  section: private networking that matches an already-committed boundary
  with zero extra configuration, rather than a capability that has to be
  added on top.
- **Let the backend scale to zero, like a typical free-tier deploy.**
  Rejected: it would silently stop the daily ingestion and forecast
  refresh — the platform's actual product — for a cost saving that is not
  needed at this scale, since two always-on machines already fit the
  included allowance.
- **Deploy the frontend to Fly too, for one platform instead of two.**
  Rejected: Vercel is free, needs no Dockerfile changes, and gives PR
  preview deployments for nothing — the more capable option for an SSR
  Next.js app, not a compromise made for convenience.
- **A custom GitHub Actions job to deploy the frontend.** Rejected in favor
  of Vercel's native Git integration, which already does this and does it
  better — preview URLs per PR, a rollback UI — than a bespoke script would.
