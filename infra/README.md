# Infrastructure

Deployment and local-orchestration configuration for the platform.

**Owns**: Docker Compose definitions for local development (`infra/docker/`)
and a Render Blueprint for the hosted backend and ML service
(`infra/render/`) — see
[`docs/adr/0008-render-and-external-scheduling.md`](../docs/adr/0008-render-and-external-scheduling.md).
GitHub Actions workflow files must live at `.github/workflows/` (a GitHub
platform requirement, not a choice); they run tests and trigger the daily
jobs, and don't invoke anything under `infra/` — Render deploys directly
from its own dashboard connection to the repo.

**Does not own**: application code or business logic for any component.

Redis, when introduced, will be configured here (see
`docs/architecture/overview.md`, "Deferred Infrastructure").
