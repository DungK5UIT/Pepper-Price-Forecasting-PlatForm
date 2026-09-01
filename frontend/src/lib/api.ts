/**
 * Client for the Java backend's public API — the only data source this app is
 * allowed to read from (see docs/architecture/overview.md and ADR-0002).
 *
 * These run on the server (the pages calling them are Server Components), so
 * the base URL is a server-only variable and never reaches the browser.
 */

import type {
  ForecastSeries,
  Granularity,
  MarketInsight,
  PeriodStat,
  PricePoint,
  ProvinceWeather,
  RegionPrice,
  TodayPrice,
} from "./types";

const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

/**
 * Prices and weather are refreshed once a day upstream, so a few minutes of
 * staleness costs nothing and saves a round trip per render.
 */
const REVALIDATE_SECONDS = 300;

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    next: { revalidate: REVALIDATE_SECONDS },
  });

  if (!response.ok) {
    throw new Error(`Backend returned ${response.status} ${response.statusText} for ${path}`);
  }

  return (await response.json()) as T;
}

export function getTodayPrice(): Promise<TodayPrice> {
  return getJson<TodayPrice>("/api/v1/prices/today");
}

export function getRegionPrices(): Promise<RegionPrice[]> {
  return getJson<RegionPrice[]>("/api/v1/prices/regions");
}

export function getPeriodStats(): Promise<PeriodStat[]> {
  return getJson<PeriodStat[]>("/api/v1/prices/stats");
}

export function getProvinceWeather(): Promise<ProvinceWeather[]> {
  return getJson<ProvinceWeather[]>("/api/v1/weather");
}

export function getMarketInsight(): Promise<MarketInsight> {
  return getJson<MarketInsight>("/api/v1/market-insight");
}

function getForecastPoints(granularity: Granularity): Promise<PricePoint[]> {
  return getJson<PricePoint[]>(`/api/v1/prices/forecast?granularity=${granularity}`);
}

/**
 * All three granularities are fetched up front so switching between them in
 * the chart stays instant — the series are small and change together.
 */
export async function getForecastSeries(): Promise<ForecastSeries> {
  const [day, week, month] = await Promise.all([
    getForecastPoints("day"),
    getForecastPoints("week"),
    getForecastPoints("month"),
  ]);

  return { day, week, month };
}
