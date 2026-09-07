# Infrastructure

Deployment and local-orchestration configuration for the platform.

**Owns**: Docker Compose definitions for local development (`infra/docker/`)
and Fly.io deploy configuration for the hosted backend and ML service
(`infra/fly/`) — see
[`docs/adr/0007-hosted-deployment.md`](../docs/adr/0007-hosted-deployment.md).
GitHub Actions workflow files must live at `.github/workflows/` (a GitHub
platform requirement, not a choice); that is the one piece of deploy
tooling not here, though it invokes what is.

**Does not own**: application code or business logic for any component.

Redis, when introduced, will be configured here (see
`docs/architecture/overview.md`, "Deferred Infrastructure").
