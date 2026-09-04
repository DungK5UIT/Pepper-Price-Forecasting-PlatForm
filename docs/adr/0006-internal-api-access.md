# ADR-0006: Access Control for the Internal and Management APIs

- **Status**: Accepted
- **Date**: 2026-09-04

## Context

`/internal/v1/price-history` exists because ADR-0003 gives the ML service no
database access, so its training data has to come through the backend. It
returns the entire price series in one unauthenticated request.

The only thing standing in front of it was CORS scoped to `/api/**`, which
stops a browser on another origin and nothing else — not `curl`, not a script,
not anything that is not a browser. The same was true of `/actuator/**`: only
`health` and `info` are exposed, but nothing required a credential, and the
ingestion health detail added in ADR-0005 names jobs and timings.

None of this was urgent while the service ran on a laptop. It stops being
acceptable the moment the process is reachable from the internet, which is the
next thing this project wants to do.

## Decision

**Draw one line; do not build an auth system.** There are no user accounts,
no mutating endpoints, and no per-user data. Inventing a user model to hold a
single machine account would be the wrong shape, and building login before
there is anything to log in to would be building for an imagined product.

The line:

| Surface | Rule | Why |
|---|---|---|
| `GET /api/**` | open | The public contract. Read-only, and the prices on it are already published by giacaphe.com and giatieu.com. |
| `GET /actuator/health` | open | An uptime check must work without a credential. Detail is gated separately by `show-details=when-authorized`, so anonymous callers get the aggregate status and nothing more. |
| `/internal/**` | `INTERNAL` role, HTTP Basic | The ML service, and nothing else. |
| everything else | authenticated | A new endpoint has to be let out deliberately rather than inheriting access by not being mentioned. |

**One credential, in configuration, with no default.** `INTERNAL_API_USER` and
`INTERNAL_API_PASSWORD`, held in an `InMemoryUserDetailsManager`. A blank or
well-known fallback is precisely how an internal endpoint ends up effectively
open, so a deployment that has not set one fails to start.

**Stateless, no CSRF token.** Every caller authenticates per request and no
session cookie is issued, so there is nothing for a forged cross-site request
to ride on. Disabling CSRF here is not a shortcut; it is what the absence of
ambient credentials permits.

**HTTP Basic, not a bearer token or mTLS.** Basic over TLS is one credential,
no token lifecycle, and no key distribution — proportionate to one machine
caller. It is also plaintext without TLS, which makes TLS a deployment
requirement rather than a nice-to-have.

## Consequences

- `ml_service.train` now reads `INTERNAL_API_USER` / `INTERNAL_API_PASSWORD`
  from the environment and raises if they are missing. A default there would
  silently stop matching the backend the day it gets a real password, and the
  failure would look like an authentication bug rather than a missing setting.
- Deployment must terminate TLS in front of the backend. Basic auth over plain
  HTTP would put the credential on the wire in every training run.
- The controller slice tests (`@WebMvcTest`) run with the security filters off:
  the slice would otherwise apply Spring Security's defaults rather than this
  project's rules. Access is asserted against the real filter chain in
  `SecurityConfigTest` instead.
- Rotating the credential means restarting the backend and updating whatever
  runs training. Acceptable for one machine account; it would not be for many.
- This is not user authentication. If the product ever grows accounts — saved
  regions, price alerts — that is a different decision, needing a user table, a
  password or OAuth flow, and a session or token story. Nothing here forecloses
  it; nothing here provides it either.

## Alternatives Considered

- **Bind `/internal/**` to localhost, or drop it behind a firewall.** No
  credential to manage, and genuinely strong when the network boundary is
  real. Rejected because it makes correctness depend on deployment topology
  that does not exist yet, and the ML service is meant to be a separate process
  that could move.
- **A shared secret in a custom header.** Slightly simpler than Basic, but it
  is Basic with extra steps and none of the tooling — no browser prompt, no
  library support, no standard place to put it.
- **Removing `/internal/**` and having the backend push training data.** Would
  close the surface entirely. Rejected for now: training is initiated by a
  human running a CLI, and inverting that so the backend drives it is a larger
  change than the problem warrants.
