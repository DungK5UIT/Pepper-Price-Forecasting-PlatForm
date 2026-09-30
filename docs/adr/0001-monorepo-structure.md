# ADR-0001: Monorepo over Polyrepo

- **Status**: Accepted
- **Date**: 2026-08-10

## Context

The platform spans three deployable components (Next.js frontend, Spring
Boot backend, FastAPI ML service) plus shared infrastructure config and
database migrations. At project start there is a single owner (one
engineer) and the components are under active, tightly-coupled early
development — API contracts and domain model will shift together across
components frequently.

## Decision

Use a single repository (this one) containing all components, each in its
own top-level directory with clear ownership, rather than splitting into
per-component repositories.

## Consequences

- Cross-component changes (e.g. an API contract change touching both
  backend and frontend) land in one coherent commit/PR instead of being
  split and coordinated across repos.
- Single CI/CD surface to set up initially, instead of N.
- Git history for the whole platform is visible in one place — useful both
  operationally and as a demonstration of engineering work over time.
- Trade-off: as the project grows, a monorepo requires more deliberate
  directory/ownership discipline to avoid components leaking into each
  other (mitigated by the boundaries in `docs/architecture/overview.md`
  and ADR-0002).
- This is revisited if the project gains multiple independent teams or if
  a component needs an independent release cadence/CI pipeline that a
  monorepo makes awkward — not a permanent commitment.

### Update, 2026-09-30: placeholder directories removed

The first layout reserved `db/migrations/`, `scripts/` and `tests/` at the
root. None of them ever held anything: migrations had to live inside the
backend so Flyway loads them from the jar, no cross-service script became
necessary, and there are no cross-service tests yet. Each held only a README
explaining that it was empty. They are removed so a first-time reader sees
only directories that do something:

```
frontend/  backend/  ml-service/  infra/  docs/  .github/
```

A directory comes back when something real needs it. The decision itself —
one repository — is unchanged, and matters more now that the repository is
also how the project is presented: one link shows every component, its
tests, its history and the reasoning behind it.

## Alternatives Considered

- **Polyrepo** (one repo per component): rejected for now — coordination
  overhead across repos isn't justified by a single-owner, early-stage
  project where components change together.
