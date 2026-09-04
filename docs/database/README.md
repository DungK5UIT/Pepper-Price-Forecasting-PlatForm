# Database Documentation

PostgreSQL, owned exclusively by the Java backend (ADR-0002). The conceptual
model lives in [`../architecture/domain-model.md`](../architecture/domain-model.md);
this file documents what is actually in the database.

Migrations are Flyway SQL files in
`backend/src/main/resources/db/migration/`, applied by the backend at startup.

## Tables

| Table | Holds | Notes |
|---|---|---|
| `market_price` | One observed price per region per day | `region = 'national'` is the headline series (the mean of the others); the rest are the regional breakdown. Unique on (commodity, region, observed_date) — `source` is provenance, not identity, so a later run of the day corrects the row. |
| `weather_observation` | Temperature, rainfall and wind per province per day | `is_forecast` separates predicted days from observed ones. Unique on (province, observed_date). |
| `forecast` | One row per predicted point | Median plus a q10–q90 band, tagged with the `granularity` it was produced at and the `as_of_date` of the run. Unique on (commodity, granularity, as_of_date, target_date). |
| `market_insight` | The narrative commentary for a day | One row per `as_of_date`. |
| `ingestion_run` | One row per collection attempt | `status` is `success`, `partial` or `failed`; `detail` carries the failover reason, the disagreement between price sources, or the exception. The first place to look when the numbers stop moving. |

Row Level Security is enabled on all five with no policies. Only the backend
reaches them, over a direct connection as their owner (owners are exempt from
RLS), so the effect is to deny Supabase's `anon`/`authenticated` roles.

## Where the data comes from

`market_price` and `weather_observation` are filled each morning by the
backend's own collection jobs — two public price sites and Open-Meteo — see
[`../adr/0005-data-ingestion.md`](../adr/0005-data-ingestion.md). Rows dated
before 2026-09-04, and every row with a `source` of
`historical_backfill_2026-08_interpolated`, came from the earlier prototype by
way of `V2`.

`forecast` is written by the refresh that calls the ML service;
`market_insight` is still prototype data, with nothing generating it yet.

## Hosting

Supabase (project `PepperPrice-Forecasting`, `ap-southeast-1`), reached through
the **session pooler** on port 5432 — the backend holds a long-lived connection
pool and uses prepared statements, which the transaction pooler on 6543 does
not support.

## Tables this schema does not own

The same database still holds `raw_price_daily`, `raw_features_daily`,
`weather_daily`, `forecast_cache`, `ai_insight_cache` and `scrape_log` from an
earlier prototype, which may still be writing to them. `V2` imported their
history into the tables above and otherwise leaves them alone. They are not
part of this platform's schema and nothing here reads them at runtime.

RLS is disabled on those prototype tables. Not a live exposure while access
stays server-side, but worth closing if anything ever reaches this project with
a Supabase anon key.
