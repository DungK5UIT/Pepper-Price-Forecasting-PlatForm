# ADR-0005: Daily Ingestion of Prices and Weather

- **Status**: Accepted
- **Date**: 2026-09-04

## Context

Every number the platform served was inherited. `market_price` stopped on
2026-08-31 and `weather_observation` on whatever day the earlier prototype
(`D:\DuBaoGiaTieu`) last ran; nothing in this codebase moved either forward.
The forecast was regenerated each morning against a price that never changed,
which made the whole pipeline look alive while its input stood still.

That also caps the model. ADR-0004 had to drop the weather features for lack
of history — one month, against three and a half years of prices — and no
amount of modelling fixes an input that is not being collected.

Two questions had to be answered: where collection runs, and what to do when a
source is wrong rather than merely down.

## Decision

**Collection runs in the Java backend, not the ML service.** ADR-0002 makes the
backend the system of record and ADR-0003 gives the ML service no database
access, so a Python collector would have to write its results back through the
backend anyway. Scraping is not modelling work, and putting it where the schema
and the transactions already live avoids a service boundary that buys nothing.

**Prices come from two sources, read in order.** `giacaphe.com` is primary,
`giatieu.com` the fallback. Both are read on every run, not just the first that
works, because a site that has quietly frozen still answers HTTP 200 and a
second reading is the only cheap way to notice.

They are **not independent**: giacaphe.com credits giatieu.com as its data
source, and their figures were byte-identical on the day this was built. So
their agreement is weak evidence that a price is correct, while their
disagreement remains strong evidence that something is wrong. The
implementation only acts on the second direction — a run is flagged, never
silently blessed.

**Weather comes from Open-Meteo**, free and keyless, for six provinces. The
platform has no billing story yet and the model needs daily aggregates rather
than station-level detail. A province that fails takes the whole run down: a
partial set of six looks complete to a reader.

**Writes are idempotent, keyed on what a reading is about.** Prices upsert on
`(commodity, region, observed_date)`; weather on `(province, observed_date)`.
This is what made `market_price_unique` change in V3 — `source` had been part
of the key, which let two sources record the same region on the same day as two
rows, and both the chart and the public API assume one price per region per
day. Source is provenance, not identity.

For weather, correcting a row is the normal path rather than an edge case: a
date arrives first as a forecast and is overwritten with the measured value
once it has passed.

**Every attempt is logged to `ingestion_run`**, successful or not, with a
status of `success`, `partial`, or `failed` and a free-text detail. A
collection job that quietly stops working is this platform's worst failure
mode — the API keeps serving the last good rows, nothing looks broken, and the
forecast is weeks stale before anyone notices.

**Failure is contained, never fatal.** A collection run that throws is logged
and swallowed at the scheduler boundary. Yesterday's data keeps serving, the
run is recorded as failed, and the site stays up.

## Consequences

- The price and weather series now advance on their own, at 07:00 and 07:10,
  ahead of the 08:15 forecast refresh that consumes them.
- Weather history starts accumulating from 2026-09-04. Restoring the features
  ADR-0004 dropped needs roughly a year of it, so this is a trigger to revisit
  rather than an immediate gain.
- The platform depends on the markup of two public websites. When one changes
  its layout the parser raises rather than returning nothing, so the failure is
  loud; the saved fixtures under `backend/src/test/resources/ingest/` are a
  place to reproduce a break, not a monitor that catches one.
- Nothing watches `ingestion_run` yet. It records the evidence; reading it is
  still manual, and alerting on it is open work.
- `robots.txt` for both price sources was re-verified on 2026-09-04: neither
  disallows `/gia-tieu-hom-nay/`, neither sets a crawl delay, and the job reads
  one page a day from each. This has to be re-checked, not assumed, if the
  frequency ever rises.

## Alternatives Considered

- **One source for prices.** Simpler, and the fallback rarely fires. Rejected
  because a missed day cannot be backfilled — neither site publishes history —
  so a day lost to one site being down is lost permanently.
- **A separate collector service.** Cleaner in isolation, but it would need
  database access this architecture deliberately restricts (ADR-0003), or a
  write API that does not exist yet. Revisit if collection grows beyond two
  sources or needs its own schedule and scaling.
- **Storing every source's reading and choosing at read time.** More faithful
  to what was observed, and it would keep the disagreement in the data rather
  than only in a log line. Rejected as premature: the public API and the chart
  both want one price per day, and the choice would then have to be made on
  every read instead of once on write.
