-- Data starts arriving on its own: this migration adds the log that records
-- every collection attempt, and tightens market_price to one row per region
-- per day.
--
-- Why the constraint changes: market_price_unique included `source`, which let
-- two sources record the same region on the same day as two rows. The public
-- API and the chart both assume one price per region per day, and the daily
-- job commits to exactly one source per run, so `source` is provenance rather
-- than part of the identity. Verified empty of violations before the change;
-- V2 still names the constraint, and runs against the older definition on a
-- fresh database, so it needs no edit.

alter table market_price drop constraint market_price_unique;
alter table market_price add constraint market_price_unique
    unique (commodity, region, observed_date);

-- One row per attempt, successful or not. A collection job that quietly stops
-- working is the failure mode that matters here: the API keeps serving the
-- last good data and nothing looks wrong until the forecast is weeks stale.
create table ingestion_run (
    id           bigint generated always as identity primary key,
    job_name     text        not null,
    status       text        not null,
    rows_written integer     not null default 0,
    -- Free text: the failover reason, the discrepancy between sources, or the
    -- exception. Null when the run was unremarkable.
    detail       text,
    started_at   timestamptz not null,
    finished_at  timestamptz not null default now(),
    constraint ingestion_run_status_supported check (status in ('success', 'partial', 'failed'))
);

create index ingestion_run_lookup_idx on ingestion_run (job_name, finished_at desc);

alter table ingestion_run enable row level security;
