/**
 * Placeholder data layer.
 *
 * The Java backend owns all forecast, price and weather data (see
 * docs/architecture/overview.md) — the frontend must never talk to
 * Postgres or the ML service directly. Until that public REST API
 * exists, these functions return static mock data shaped the way the
 * real API responses are expected to look, so swapping them for real
 * `fetch()` calls later is a localized change.
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
  WeatherCondition,
} from "./types";

function pad(n: number): string {
  return String(n).padStart(2, "0");
}

function dayLabel(d: Date): string {
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}`;
}

function monthLabel(d: Date): string {
  return `T${d.getMonth() + 1}/${String(d.getFullYear()).slice(2)}`;
}

function addDays(d: Date, days: number): Date {
  const copy = new Date(d);
  copy.setDate(copy.getDate() + days);
  return copy;
}

function addMonths(d: Date, months: number): Date {
  const copy = new Date(d);
  copy.setMonth(copy.getMonth() + months);
  return copy;
}

/** Deterministic pseudo-random noise in [-0.5, 0.5], seeded by index. */
function noise(seed: number): number {
  const x = Math.sin(seed * 12.9898) * 43758.5453;
  return (x - Math.floor(x)) - 0.5;
}

interface SeriesConfig {
  granularity: Granularity;
  historyCount: number;
  forecastCount: number;
  stepDrift: number;
  step: (d: Date, n: number) => Date;
  label: (d: Date) => string;
}

function buildSeries(config: SeriesConfig, endPrice: number): PricePoint[] {
  const { historyCount, forecastCount, stepDrift, step, label } = config;
  const today = new Date();

  const rawHistory: number[] = [];
  let price = endPrice - stepDrift * historyCount;
  for (let i = 0; i < historyCount; i++) {
    price += stepDrift + noise(i) * stepDrift * 2.5;
    rawHistory.push(price);
  }
  const shift = endPrice - rawHistory[rawHistory.length - 1];

  const points: PricePoint[] = rawHistory.map((p, i) => ({
    label: label(step(today, -(historyCount - 1 - i))),
    actual: Math.round(p + shift),
  }));

  const lastPoint = points[points.length - 1];
  lastPoint.label = "Hôm nay";
  lastPoint.isToday = true;
  lastPoint.forecastQ10 = lastPoint.actual;
  lastPoint.forecastQ50 = lastPoint.actual;
  lastPoint.forecastQ90 = lastPoint.actual;

  let fPrice = endPrice;
  for (let i = 1; i <= forecastCount; i++) {
    fPrice += stepDrift * 1.1 + noise(historyCount + i) * stepDrift;
    const spread = stepDrift * 2 * i;
    points.push({
      label: label(step(today, i)),
      forecastQ50: Math.round(fPrice),
      forecastQ10: Math.round(fPrice - spread),
      forecastQ90: Math.round(fPrice + spread),
    });
  }

  return points;
}

const TODAY_PRICE_VND = 148700;

export function getTodayPrice(): TodayPrice {
  return {
    priceVnd: TODAY_PRICE_VND,
    changeVnd: 1200,
    changePercent: 0.82,
    asOfDate: dayLabel(new Date()),
    forecastLow: 146000,
    forecastHigh: 152000,
    forecastMedian: 149000,
    sourceLabel: "Bình quân 6 tỉnh trọng điểm",
  };
}

let cachedSeries: ForecastSeries | null = null;

export function getForecastSeries(): ForecastSeries {
  if (cachedSeries) return cachedSeries;

  cachedSeries = {
    day: buildSeries(
      {
        granularity: "day",
        historyCount: 15,
        forecastCount: 7,
        stepDrift: 130,
        step: (d, n) => addDays(d, n),
        label: dayLabel,
      },
      TODAY_PRICE_VND,
    ),
    week: buildSeries(
      {
        granularity: "week",
        historyCount: 10,
        forecastCount: 6,
        stepDrift: 620,
        step: (d, n) => addDays(d, n * 7),
        label: dayLabel,
      },
      TODAY_PRICE_VND,
    ),
    month: buildSeries(
      {
        granularity: "month",
        historyCount: 12,
        forecastCount: 6,
        stepDrift: 1650,
        step: (d, n) => addMonths(d, n),
        label: monthLabel,
      },
      TODAY_PRICE_VND,
    ),
  };

  return cachedSeries;
}

export function getRegionPrices(): RegionPrice[] {
  return [
    { region: "Đắk Lắk", priceVnd: 149200, changeVnd: 1500 },
    { region: "Đắk Nông", priceVnd: 148900, changeVnd: 1100 },
    { region: "Gia Lai", priceVnd: 147600, changeVnd: 900 },
    { region: "Đồng Nai", priceVnd: 148300, changeVnd: -300 },
    { region: "Bình Phước", priceVnd: 149800, changeVnd: 1800 },
    { region: "Bà Rịa - Vũng Tàu", priceVnd: 147900, changeVnd: 200 },
  ];
}

export function getPeriodStats(): PeriodStat[] {
  return [
    { label: "30 ngày qua", changePercent: 4.2, changeVnd: 6100 },
    { label: "90 ngày qua", changePercent: 9.8, changeVnd: 13300 },
    { label: "1 năm qua", changePercent: 18.5, changeVnd: 23200 },
  ];
}

export function getMarketInsight(): MarketInsight {
  return {
    text: "Giá tiêu trong nước tiếp tục xu hướng tăng nhẹ nhờ nguồn cung nội địa thu hẹp cuối vụ và nhu cầu xuất khẩu ổn định sang Trung Quốc, Mỹ. Mưa lớn kéo dài tại Đắk Lắk và Đắk Nông trong tuần qua có thể ảnh hưởng tiến độ thu hoạch, tạo áp lực tăng giá ngắn hạn. Mô hình giữ mức tăng trung bình 1,5–2% trong 30 ngày tới, biên độ dao động nới rộng dần theo thời gian dự báo.",
    updatedAtLabel: `08:00, ${dayLabel(new Date())}`,
  };
}

interface DayTemplate {
  offset: string;
  condition: WeatherCondition;
  tempMin: number;
  tempMax: number;
  rainMm: number;
}

function buildProvince(province: string, templates: DayTemplate[]): ProvinceWeather {
  return {
    province,
    days: templates.map((t, i) => ({
      label: i === 0 ? "Hôm nay" : t.offset,
      condition: t.condition,
      tempMin: t.tempMin,
      tempMax: t.tempMax,
      rainMm: t.rainMm,
      isForecast: i > 0,
    })),
  };
}

const WEEKDAY_LABELS = ["Hôm nay", "Th3", "Th4", "Th5", "Th6", "Th7", "CN"];

export function getProvinceWeather(): ProvinceWeather[] {
  return [
    buildProvince(
      "Đắk Lắk",
      [
        { offset: WEEKDAY_LABELS[0], condition: "cloud-rain", tempMin: 24, tempMax: 31, rainMm: 18 },
        { offset: WEEKDAY_LABELS[1], condition: "cloud-rain", tempMin: 23, tempMax: 29, rainMm: 24 },
        { offset: WEEKDAY_LABELS[2], condition: "cloud-sun", tempMin: 24, tempMax: 30, rainMm: 6 },
        { offset: WEEKDAY_LABELS[3], condition: "cloud-rain", tempMin: 23, tempMax: 28, rainMm: 20 },
        { offset: WEEKDAY_LABELS[4], condition: "cloud", tempMin: 24, tempMax: 30, rainMm: 9 },
        { offset: WEEKDAY_LABELS[5], condition: "cloud-rain", tempMin: 23, tempMax: 29, rainMm: 22 },
        { offset: WEEKDAY_LABELS[6], condition: "cloud-sun", tempMin: 24, tempMax: 31, rainMm: 5 },
      ],
    ),
    buildProvince(
      "Đắk Nông",
      [
        { offset: WEEKDAY_LABELS[0], condition: "cloud-rain", tempMin: 23, tempMax: 29, rainMm: 22 },
        { offset: WEEKDAY_LABELS[1], condition: "cloud-rain", tempMin: 22, tempMax: 28, rainMm: 26 },
        { offset: WEEKDAY_LABELS[2], condition: "cloud", tempMin: 23, tempMax: 29, rainMm: 10 },
        { offset: WEEKDAY_LABELS[3], condition: "cloud-rain", tempMin: 22, tempMax: 27, rainMm: 19 },
        { offset: WEEKDAY_LABELS[4], condition: "cloud-sun", tempMin: 23, tempMax: 28, rainMm: 8 },
        { offset: WEEKDAY_LABELS[5], condition: "cloud-rain", tempMin: 22, tempMax: 28, rainMm: 21 },
        { offset: WEEKDAY_LABELS[6], condition: "cloud-sun", tempMin: 23, tempMax: 29, rainMm: 7 },
      ],
    ),
    buildProvince(
      "Gia Lai",
      [
        { offset: WEEKDAY_LABELS[0], condition: "cloud-sun", tempMin: 24, tempMax: 32, rainMm: 8 },
        { offset: WEEKDAY_LABELS[1], condition: "cloud", tempMin: 24, tempMax: 31, rainMm: 12 },
        { offset: WEEKDAY_LABELS[2], condition: "sun", tempMin: 25, tempMax: 32, rainMm: 4 },
        { offset: WEEKDAY_LABELS[3], condition: "cloud-rain", tempMin: 24, tempMax: 30, rainMm: 15 },
        { offset: WEEKDAY_LABELS[4], condition: "cloud-sun", tempMin: 24, tempMax: 31, rainMm: 6 },
        { offset: WEEKDAY_LABELS[5], condition: "cloud", tempMin: 24, tempMax: 30, rainMm: 11 },
        { offset: WEEKDAY_LABELS[6], condition: "sun", tempMin: 25, tempMax: 32, rainMm: 3 },
      ],
    ),
    buildProvince(
      "Đồng Nai",
      [
        { offset: WEEKDAY_LABELS[0], condition: "cloud-rain", tempMin: 25, tempMax: 33, rainMm: 15 },
        { offset: WEEKDAY_LABELS[1], condition: "cloud-rain", tempMin: 25, tempMax: 32, rainMm: 18 },
        { offset: WEEKDAY_LABELS[2], condition: "cloud-sun", tempMin: 26, tempMax: 33, rainMm: 6 },
        { offset: WEEKDAY_LABELS[3], condition: "cloud-rain", tempMin: 25, tempMax: 31, rainMm: 20 },
        { offset: WEEKDAY_LABELS[4], condition: "cloud-sun", tempMin: 26, tempMax: 33, rainMm: 5 },
        { offset: WEEKDAY_LABELS[5], condition: "cloud", tempMin: 25, tempMax: 32, rainMm: 14 },
        { offset: WEEKDAY_LABELS[6], condition: "sun", tempMin: 26, tempMax: 33, rainMm: 4 },
      ],
    ),
    buildProvince(
      "Bình Phước",
      [
        { offset: WEEKDAY_LABELS[0], condition: "cloud-sun", tempMin: 25, tempMax: 34, rainMm: 3 },
        { offset: WEEKDAY_LABELS[1], condition: "cloud", tempMin: 25, tempMax: 33, rainMm: 7 },
        { offset: WEEKDAY_LABELS[2], condition: "sun", tempMin: 26, tempMax: 34, rainMm: 2 },
        { offset: WEEKDAY_LABELS[3], condition: "cloud", tempMin: 25, tempMax: 32, rainMm: 10 },
        { offset: WEEKDAY_LABELS[4], condition: "cloud-sun", tempMin: 26, tempMax: 34, rainMm: 3 },
        { offset: WEEKDAY_LABELS[5], condition: "cloud", tempMin: 25, tempMax: 33, rainMm: 6 },
        { offset: WEEKDAY_LABELS[6], condition: "sun", tempMin: 26, tempMax: 34, rainMm: 2 },
      ],
    ),
    buildProvince(
      "Bà Rịa - Vũng Tàu",
      [
        { offset: WEEKDAY_LABELS[0], condition: "sun", tempMin: 26, tempMax: 32, rainMm: 1 },
        { offset: WEEKDAY_LABELS[1], condition: "cloud-sun", tempMin: 26, tempMax: 31, rainMm: 3 },
        { offset: WEEKDAY_LABELS[2], condition: "sun", tempMin: 27, tempMax: 33, rainMm: 0 },
        { offset: WEEKDAY_LABELS[3], condition: "cloud", tempMin: 26, tempMax: 31, rainMm: 5 },
        { offset: WEEKDAY_LABELS[4], condition: "cloud-sun", tempMin: 27, tempMax: 32, rainMm: 2 },
        { offset: WEEKDAY_LABELS[5], condition: "cloud", tempMin: 26, tempMax: 31, rainMm: 4 },
        { offset: WEEKDAY_LABELS[6], condition: "sun", tempMin: 27, tempMax: 33, rainMm: 1 },
      ],
    ),
  ];
}
