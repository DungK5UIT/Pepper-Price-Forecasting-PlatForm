# Database Migrations

Versioned PostgreSQL schema migrations (Flyway or Liquibase — tool choice
deferred until the first migration is actually written), tracked
independently of application code so schema history is reviewable on its
own.

**Owns**: the schema definition and its evolution over time, applied by
the Java backend at startup (or by CI, depending on the pipeline design
chosen later).

**Does not own**: conceptual domain modeling — that lives in
`docs/architecture/domain-model.md` and should inform migrations here, not
the other way around. Schema-level documentation derived from these
migrations lives in `docs/database/`.

Empty until the first entity from the domain model is implemented.
