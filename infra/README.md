# Infrastructure

Deployment and local-orchestration configuration for the platform.

**Owns**: Docker Compose definitions (`infra/docker/`), CI pipeline
configuration, and — later — any reverse proxy/HTTPS or cloud deployment
config, once there is something to deploy.

**Does not own**: application code or business logic for any component.

Empty until there are runnable services to orchestrate. Redis, when
introduced, will be configured here (see
`docs/architecture/overview.md`, "Deferred Infrastructure").
