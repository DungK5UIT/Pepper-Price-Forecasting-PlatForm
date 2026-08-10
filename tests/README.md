# Cross-Service Tests

Integration and end-to-end tests that span more than one component (e.g.
frontend-to-backend API contract tests, full ingestion-to-forecast
scenarios).

**Does not own**: unit tests for an individual component — those live
inside that component (`frontend/`, `backend/`, `ml-service/`), colocated
with the code they test.

Empty until there are multiple components to test together.
