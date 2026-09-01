export type Granularity = "day" | "week" | "month";

export interface PricePoint {
  label: string;
  isToday?: boolean;
  actual?: number;
  forecastQ10?: number;
  forecastQ50?: number;
  forecastQ90?: number;
}

export type ForecastSeries = Record<Granularity, PricePoint[]>;

export interface TodayPrice {
  priceVnd: number;
  changeVnd: number;
  changePercent: number;
  asOfDate: string;
  forecastLow: number;
  forecastHigh: number;
  forecastMedian: number;
  sourceLabel: string;
}

export interface RegionPrice {
  region: string;
  priceVnd: number;
  changeVnd: number;
}

export interface PeriodStat {
  label: string;
  changePercent: number;
  changeVnd: number;
}

export interface MarketInsight {
  text: string;
  updatedAtLabel: string;
}

export type WeatherCondition = "sun" | "cloud" | "cloud-sun" | "cloud-rain" | "wind";

export interface WeatherDay {
  label: string;
  condition: WeatherCondition;
  /** One reading per day — the upstream source records a single temperature, not a range. */
  tempC: number;
  rainMm: number;
  isForecast: boolean;
}

export interface ProvinceWeather {
  province: string;
  days: WeatherDay[];
}
