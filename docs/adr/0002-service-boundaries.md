# ADR-0002: Service Boundary Between Java Backend and Python ML Service

- **Status**: Accepted
- **Date**: 2026-08-10

## Context

The platform needs both conventional application/business logic (auth,
persistence, API surface) and ML/data-science work (feature engineering,
model training, forecasting). Both Java/Spring Boot and Python are in the
intended stack, so the line between "what runs where" needs to be decided
before any code is written, or logic will end up duplicated or
inconsistently placed across the two services.

## Decision

- The **Java backend** owns all business/application concerns: users,
  authentication, market price data, data source configuration, the
  public REST API, job orchestration, and the PostgreSQL schema
  (migrations included).
- The **Python ML service** owns all ML/data-science concerns: feature
  engineering, model training, model evaluation, and forecast generation
  logic. It is invoked by the Java backend via an internal API and is
  stateless from the rest of the platform's perspective — it does not
  expose anything to the frontend and does not independently own
  user-facing state.

## Consequences

- Any future "should this logic live in Java or Python" question has a
  default answer: business/persistence/API-contract logic → Java;
  model/statistical computation → Python.
- The frontend and any external client only ever need to know about one
  API surface (the Java backend's).
- The ML service can be developed, tested, and even swapped out
  (different framework, different model) without touching the backend's
  API contract with the frontend, as long as the internal contract with
  the backend holds.
- Trade-off: every piece of data the ML service needs must cross the
  internal API boundary rather than being queried directly — addressed
  specifically in ADR-0003.

## Alternatives Considered

- **Single Java service with an embedded ML library** (e.g. Java ML
  tooling instead of a separate Python service): rejected — Python's
  data-science ecosystem (pandas, scikit-learn, and future
  TensorFlow/PyTorch if justified) is materially better suited to this
  work, and the project's stack direction already calls for Python here.
- **Python service also handling business logic/API/auth**: rejected —
  would blur the "system of record" role and duplicate concerns better
  handled once, in one place.
