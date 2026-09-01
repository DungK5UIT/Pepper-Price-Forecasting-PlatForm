-- Core tables backing the public API, derived from the entities in
-- docs/architecture/domain-model.md. Owned by the backend: nothing else
-- writes here (ADR-0002).
--
-- Row Level Security is enabled with no policies on purpose. Only this
-- service reaches these tables, over a direct Postgres connection as their
-- owner (owners are exempt from RLS), so the effect is to deny Supabase's
-- anon/authenticated roles rather than to grant anyone anything.

create table market_price (
    id               bigint generated always as identity primary key,
    commodity        text           not null default 'black_pepper',
    region           text           not null,
    price_vnd_per_kg numeric(12, 2) not null,
    source           text           not null,
    observed_date    date           not null,
    created_at       timestamptz    not null default now(),
    constraint market_price_unique unique (commodity, region, observed_date, source)
);

create index market_price_observed_date_idx on market_price (observed_date desc);

alter table market_price enable row level security;

create table weather_observation (
    id             bigint generated always as identity primary key,
    province       text          not null,
    observed_date  date          not null,
    temp_c         numeric(5, 2),
    rainfall_mm    numeric(6, 2),
    wind_speed_kmh numeric(6, 2),
    is_forecast    boolean       not null default false,
    created_at     timestamptz   not null default now(),
    constraint weather_observation_unique unique (province, observed_date)
);

create index weather_observation_date_idx on weather_observation (observed_date);

alter table weather_observation enable row level security;

-- One row per forecast point. The upstream model publishes a whole series at
-- once; storing the points individually keeps them queryable by target date
-- instead of hiding them inside a document.
create table forecast (
    id                  bigint generated always as identity primary key,
    commodity           text           not null default 'black_pepper',
    granularity         text           not null,
    as_of_date          date           not null,
    target_date         date           not null,
    predicted_price_q10 numeric(12, 2),
    predicted_price_q50 numeric(12, 2) not null,
    predicted_price_q90 numeric(12, 2),
    model_version       text,
    created_at          timestamptz    not null default now(),
    constraint forecast_granularity_supported check (granularity in ('day', 'week', 'month')),
    constraint forecast_unique unique (commodity, granularity, as_of_date, target_date)
);

create index forecast_lookup_idx on forecast (granularity, as_of_date desc, target_date);

alter table forecast enable row level security;

create table market_insight (
    id           bigint generated always as identity primary key,
    as_of_date   date        not null unique,
    insight_text text        not null,
    created_at   timestamptz not null default now()
);

alter table market_insight enable row level security;
