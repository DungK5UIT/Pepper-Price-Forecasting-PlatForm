# API Documentation

The Java backend is the platform's only public API surface (see
`docs/architecture/overview.md`). This documents its contract.

This directory does not document the internal API between the Java
backend and the Python ML service — that's an implementation detail
between two components owned by the same team, not a public contract.

## Conventions

- Base path `/api/v1`. The version is in the path so a breaking change can
  ship as `/api/v2` while `/api/v1` keeps serving existing clients.
- JSON only. Money is in whole VND (`long`), never a formatted string —
  formatting is the frontend's concern.
- Optional fields are **omitted** rather than sent as `null`.
- No authentication yet: every endpoint below is public read-only data.
  Auth arrives with the first user-specific resource.
- Errors use RFC 9457 problem details (`application/problem+json`),
  produced centrally by `GlobalExceptionHandler`:

  ```json
  {
    "title": "Invalid request",
    "status": 400,
    "detail": "Unsupported granularity 'bogus'. Supported values: day, week, month",
    "instance": "/api/v1/prices/forecast"
  }
  ```

## Endpoints

### `GET /api/v1/prices/today`

Today's headline price plus the near-term forecast range.

```json
{
  "priceVnd": 148700, "changeVnd": 1200, "changePercent": 0.82,
  "asOfDate": "01/09", "forecastLow": 146000, "forecastHigh": 152000,
  "forecastMedian": 149000, "sourceLabel": "Bình quân 6 tỉnh trọng điểm"
}
```

### `GET /api/v1/prices/regions`

Latest price per growing region, with the change against the previous day.

```json
[{ "region": "Đắk Lắk", "priceVnd": 149200, "changeVnd": 1500 }]
```

### `GET /api/v1/prices/forecast?granularity=day|week|month`

The chart series: historical points carry `actual`, forecast points carry
the quantile band, and the single point marked `isToday` carries both so
the two lines meet. Defaults to `day`; an unsupported value is a `400`.

```json
[
  { "label": "31/08", "actual": 148467 },
  { "label": "Hôm nay", "isToday": true, "actual": 148700,
    "forecastQ10": 148700, "forecastQ50": 148700, "forecastQ90": 148700 },
  { "label": "02/09", "forecastQ10": 148570, "forecastQ50": 148830, "forecastQ90": 149090 }
]
```

### `GET /api/v1/prices/stats`

Change over fixed look-back windows.

```json
[{ "label": "30 ngày qua", "changePercent": 4.2, "changeVnd": 6100 }]
```

### `GET /api/v1/weather`

Weather per growing province: the current day first, then forecast days
(`isForecast: true`). `condition` is one of `sun`, `cloud`, `cloud-sun`,
`cloud-rain`, `wind`.

```json
[{
  "province": "Đắk Lắk",
  "days": [{ "label": "Hôm nay", "condition": "cloud-rain",
             "tempMin": 24, "tempMax": 31, "rainMm": 18.0, "isForecast": false }]
}]
```

### `GET /api/v1/market-insight`

The narrative commentary shown alongside the numbers.

```json
{ "text": "Giá tiêu trong nước tiếp tục…", "updatedAtLabel": "08:00, 01/09" }
```

### `GET /actuator/health`

Liveness for deployment tooling. Not part of the versioned contract.

## Data source

These endpoints are currently served from in-memory stubs in the
backend's `service/stub/` package — the shapes are final, the numbers are
not. They start returning real data when the database is wired up, with
no change to this contract.
