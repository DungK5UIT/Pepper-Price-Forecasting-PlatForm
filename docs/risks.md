# Risk Register

| Version | Date | Change |
|---|---|---|
| 1.0 | 2026-09-30 | First version, from a full review of the code, tests and deployment at commit `c0ad2f0`. |

What can go wrong, how we would find out, and what we do about it.
Requirements and test cases referenced here are in
[`requirements.md`](requirements.md) and [`test-cases.md`](test-cases.md).

**Likelihood / Impact:** H high · M medium · L low.
**Status:** *Open* (not handled yet) · *Mitigated* (handled, some residue)
· *Accepted* (known and deliberately left).

## Open

| ID | Risk (bad case) | L | I | Detected today by | Handling | Status |
|---|---|---|---|---|---|---|
| RISK-02 | **An outbound call hangs.** No connect/read timeout on the scraping, Open-Meteo or ML-service clients. A site that accepts the connection and never answers holds the job until the caller gives up. | M | H | Nothing | Timeouts on every client (NFR-04). TC-68. | Open |
| RISK-04 | **Monthly forecast labelled one month early.** The model predicts the move from the current (partial) month's row to the *next* month but labels it the *current* month's end. With a synthetic +2%/month series, the +2% point is dated 11 days ahead instead of 39 (end of next month). The naive model that ships keeps the median unchanged, so today only the monthly band is too wide; once a model beats the baseline, its dates are wrong. | H | M | Nothing — TC-43 asserts the current labels | Label horizon *h* as the end of month *m+h* where *m* is the last monthly row; add a regression test with a trending series. | Open |
| RISK-05 | **The numbers users see are untested.** `DatabasePriceService` and `DatabaseWeatherService` have no tests (controller tests mock them); the naive strategy that ships has no test; `train.py` is 0% covered; backend coverage is not measured; migrations never run in tests (H2 with `create-drop`). | H | M | Code review only | Write TC-02…TC-14, TC-47, TC-53; add JaCoCo and `--cov-branch` to CI; run migrations against PostgreSQL (Testcontainers). | Open |
| RISK-08 | **An empty table takes the whole dashboard down.** No price or no commentary → `IllegalStateException` → `500`; the page loads all sections with one `Promise.all`, so one `500` shows the error page instead of the other five sections. | L | M | Error page | Return `404`/empty for missing data; let each section fail on its own. | Open |
| RISK-09 | **GitHub disables the daily schedule.** Scheduled workflows are turned off after 60 days without repository activity. Last commit before this document: 2026-09-07. | M | H | GitHub email to the owner | Keep committing; the manual *Run workflow* button is the backstop. | Open |
| RISK-11 | **Whole history read on every request.** Every public price endpoint loads the full national series (2005 → today); the regional endpoint loads every regional row ever stored. Fine at a few thousand rows, linear growth after. | L | L | Nothing | Query only the rows needed (`ORDER BY … LIMIT`). | Open |
| RISK-12 | **Any `IllegalArgumentException` becomes a 400 with its message.** The handler meant for bad `granularity` values catches every `IllegalArgumentException`, including ones thrown by libraries, whose messages were not written for callers. | L | L | Nothing | A named domain exception for invalid input. | Open |

## Mitigated or accepted

| ID | Risk (bad case) | L | I | Handling | Status |
|---|---|---|---|---|---|
| RISK-01 | **A daily job fails silently.** The job endpoints caught every failure and answered `200`, so the GitHub Actions run stayed green while nothing was collected. | H | H | The endpoints now call the throwing service methods: no readable price source is a `502`, any other failure a `500`, so `curl --fail` fails the run and GitHub emails the owner (TC-67). Residue: forecast refreshes are not logged to `ingestion_run` or the health check, so a failure is visible in the workflow run only. | Mitigated |
| RISK-03 | **The ML service is asleep when the forecast refresh runs.** Both Render services sleep after 15 minutes idle, and the workflow woke only the backend. | H | M | The workflow wakes the ML service right after the backend, so its cold start overlaps the ingestion steps, and retries the refresh twice, 30 s apart (safe: a same-day refresh replaces its run). Residue: the backend's call to the ML service still has no timeout (RISK-02); not yet exercised against a real deployment. | Mitigated |
| RISK-06 | **A price site changes layout or disappears.** | M | H | Parse refuses unrecognisable pages and implausible prices (FR-09), the second site is read every run (FR-10), the run is logged and health goes `STALE`. Residue: the sites share an upstream, and a missed day cannot be backfilled. | Mitigated |
| RISK-07 | **The site says things that are not true.** It said the forecast combines weather and the USD/VND rate (the model uses neither, ADR-0004), showed a hard-coded "08:00" update time, and called the market commentary machine-generated. | H | M | Copy corrected to what the model does; the hero shows the price's own date instead of a fixed time. Residue: the commentary is static prototype text shown with its real date — keep, generate or remove is OQ-1. | Mitigated |
| RISK-10 | **Free-tier limits.** Render free instances sleep and share 750 instance-hours/month; free plans and their policies can change. | M | M | Two sleeping services fit the hours (ADR-0008); health checks show the state. | Accepted |
| RISK-13 | **Legacy prototype tables in the same database have RLS disabled.** | L | M | Only the backend connects, server-side; no anon key is shipped. Close it if a client ever gets a Supabase key. | Accepted |
| RISK-14 | **Two scheduler runs at once.** | L | L | Writes are idempotent; a unique-key clash fails one run, which is logged. | Accepted |
| RISK-15 | **The model artifact goes stale.** Training is manual and the artifact is committed. | M | L | The naive model depends only on long-run volatility; `/health` shows the loaded version. Decide a cadence (OQ-5). | Accepted |
| RISK-16 | **Docs and agent instructions drift from the code.** Found: `AGENTS.md` pointing at a non-existent path, READMEs describing removed crons and mock data. | M | L | Fixed alongside this document; docs are part of "done" for every change. | Mitigated |

## Release readiness

The system does its job on a normal day, a failed job now turns the
scheduled run red (RISK-01), and the ML service is woken before it is
needed (RISK-03). It is not ready to be trusted unattended until RISK-02 is
closed: a call with no timeout can still hang a morning's job.
