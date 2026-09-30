## What and why

<!-- One or two sentences: what changes, and the problem it solves. -->

Requirements: FR-__ · Test cases: TC-__ · Risks: RISK-__

## How it was checked

<!-- Numbers, not adjectives. Say what was NOT run. -->

- [ ] Self-review of the diff
- [ ] Lint / static analysis: ___
- [ ] Unit tests: ___ passed · C0 ___% / C1 ___% on the business logic touched
- [ ] Integration tests: ___ passed
- [ ] UI change tried in a browser (golden path, one bad input, phone width) — or n/a

## Checklist

- [ ] Every test case listed above exists and passes
- [ ] `docs/requirements.md` / `docs/test-cases.md` updated if behaviour changed
- [ ] ADR added if a significant decision was made
- [ ] Coding rules R1–R13 checked (`CONTRIBUTING.md`); any rule broken is named below
- [ ] No secrets, `.env`, build output or IDE files in the diff

## Rules knowingly broken

<!-- "None", or rule + reason. -->
