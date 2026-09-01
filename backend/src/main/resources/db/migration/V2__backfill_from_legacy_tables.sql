-- One-time import of the history already collected in this database by the
-- earlier prototype (raw_price_daily, weather_daily, forecast_cache,
-- ai_insight_cache). Those tables are left untouched: the prototype's job may
-- still be writing to them, and this service does not own them.
--
-- Every statement is guarded on the source table existing, so this migration
-- is a no-op against a fresh database that never held the prototype's tables.
-- The inserts are idempotent, so re-running against a partially imported
-- database changes nothing.

do $$
begin
    if to_regclass('public.raw_price_daily') is not null then
        insert into market_price (region, price_vnd_per_kg, source, observed_date, created_at)
        select region, price_vnd_kg, source, date, scraped_at
        from raw_price_daily
        on conflict on constraint market_price_unique do nothing;
    end if;

    if to_regclass('public.weather_daily') is not null then
        insert into weather_observation (province, observed_date, temp_c, rainfall_mm,
                                         wind_speed_kmh, is_forecast, created_at)
        select province, date, temp_c, rainfall_mm, wind_speed_kmh, is_forecast, scraped_at
        from weather_daily
        on conflict on constraint weather_observation_unique do nothing;
    end if;

    -- The prototype stored a whole series per row as a JSON document; each
    -- point in it becomes a forecast row here.
    if to_regclass('public.forecast_cache') is not null then
        insert into forecast (granularity, as_of_date, target_date, predicted_price_q10,
                              predicted_price_q50, predicted_price_q90, model_version, created_at)
        select fc.granularity,
               fc.as_of_date,
               (pt ->> 'date')::date,
               (pt ->> 'price_q10')::numeric,
               (pt ->> 'price_q50')::numeric,
               (pt ->> 'price_q90')::numeric,
               'legacy_prototype',
               fc.updated_at
        from forecast_cache fc,
             lateral jsonb_array_elements(fc.payload -> 'points') pt
        on conflict on constraint forecast_unique do nothing;
    end if;

    if to_regclass('public.ai_insight_cache') is not null then
        insert into market_insight (as_of_date, insight_text, created_at)
        select as_of_date, insight, updated_at
        from ai_insight_cache
        on conflict (as_of_date) do nothing;
    end if;
end $$;
