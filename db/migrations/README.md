# Database Migrations

**The migrations live in `backend/src/main/resources/db/migration/`**, not in
this directory.

The tool choice is **Flyway**, and Flyway resolves migrations from the
application classpath so they ship inside the backend's jar and run at startup.
Keeping the SQL here instead would mean pointing Flyway at a filesystem path
relative to the working directory — which breaks as soon as the backend runs as
a packaged jar or in a container. The original intent, "schema history
reviewable on its own", is served just as well by a dedicated directory inside
the module that owns the schema.

**Owns**: nothing any more — this directory is kept as a signpost.

Schema documentation derived from those migrations lives in
[`../../docs/database/README.md`](../../docs/database/README.md); the
conceptual model it implements is in
[`../../docs/architecture/domain-model.md`](../../docs/architecture/domain-model.md).
