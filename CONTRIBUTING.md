# Contributing

How work is done in this repository — by a person or a coding agent.

## Workflow

Pick a size first.

- **Small** — one file or one concern, easy to undo: change it, verify it.
- **Standard** — everything else:
  1. **Understand.** Read the code involved (sketch the flow if it is new to
     you) and the requirement in [`docs/requirements.md`](docs/requirements.md).
     If the requirement is missing or unclear, write it down and ask.
  2. **Plan.** State the scope, the risks (bad cases and how each is
     handled) and the test cases that prove it works — add them to
     [`docs/test-cases.md`](docs/test-cases.md).
  3. **Build**, following the coding rules below.
  4. **Check, in this order:** self-review → static analysis (lint) → unit
     tests → integration tests.

A behaviour change updates `requirements.md` and `test-cases.md` in the same
PR. A significant decision gets an ADR ([`docs/adr/template.md`](docs/adr/template.md)).
A new risk goes into [`docs/risks.md`](docs/risks.md).

## Definition of done

1. The requirement is met — not just the plan.
2. Lint and tests ran; the PR states the numbers (tests passed, C0/C1 of the
   business logic touched). Anything not run is said to be not run.
3. Every test case the requirement lists exists and passes.
4. UI changes were tried in a browser: golden path, one bad input, phone width.
5. Docs and comments match the code.
6. Any rule below knowingly broken is named in the PR, with the reason.

## Coding rules

| # | Rule | In this codebase |
|---|---|---|
| R1 | Build only what the requirement asks for. | No endpoint, field, parameter or flag without a requirement ID. |
| R2 | Algorithms and resources fit the real scale. | Push filtering and `ORDER BY … LIMIT` into the query; no N+1. Jobs may run twice at once — writes stay idempotent. |
| R3 | Right data types. | Prices: `BigDecimal` in entities, `numeric` in the DB, whole-VND `long` in the API. Dates: `LocalDate` from the injected `Clock`, never `LocalDate.now()`. Closed sets: enums. |
| R4 | Tests come from the requirement. | Each test case in `docs/test-cases.md` names its FR. Business logic (services, domain, ML model code): C0 100%, C1 measured. |
| R5 | Log at the right level, never secrets. | SLF4J with placeholders / Python `logging`. ERROR = failed and needs a person; WARN = handled degradation; INFO = job milestones. Credentials are logged by name only. |
| R6 | Entry points only wire. | `BackendApplication`, `ml_service/main.py` app setup, `layout.tsx`: no business logic. |
| R7 | No hand-written Java accessors. | Lombok `@Getter`/`@Setter` for new classes; entities without `@Data`, with `@NoArgsConstructor(access = PROTECTED)`; DTOs are `record`s. Existing entities move to Lombok in their own PR. |
| R8 | Keep build output, IDE files and secrets out of git. | Check `git status` before committing; `.env` is ignored everywhere, `.env.example` is committed. |
| R9 | Formatted, named for meaning, documented. | Public classes and methods get Javadoc / docstrings / TSDoc stating the contract; inline comments say *why*. Lambda variables are named (`price -> …`). |
| R10 | Every test scenario in the spec is implemented. | Walk the PR's TC IDs before asking for review. |
| R11 | No contract change without agreement. | Changing a parameter, field or response shape of `docs/api/README.md` or the internal APIs needs a requirement update and a yes first. |
| R12 | Understand the need before coding. | Ask until the test cases can be written. |
| R13 | Business logic lives in services, not controllers. | Controllers parse, call one service method, map the result. Domain errors are named exceptions mapped in `GlobalExceptionHandler`. |

## Commits and pull requests

- Branch from `main`; one branch = one coherent change.
- [Conventional commits](https://www.conventionalcommits.org/):
  `feat(backend): …`, `fix(ml-service): …`, `docs: …`, `test: …`,
  `refactor: …`, `chore: …`. The subject says what changed; the body says
  why.
- Small commits, each one building and passing tests. Merge with
  "Create a merge commit" or "Rebase and merge" so the steps stay visible.
- CI must be green before merging. No `wip` / `fix typo` commits left in
  history.

## AI-assisted work

Coding agents follow [`AGENTS.md`](AGENTS.md) and this file. A prompt states
the role, the task, the context (requirement IDs, files, rules), the
expected output and the constraints. Generated code is read line by line and
tested before it is committed, and commits carry a `Co-Authored-By` trailer
naming the model.
