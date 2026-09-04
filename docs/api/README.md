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
- No authentication on this contract: every endpoint below is public
  read-only data. A credential is required elsewhere — `/internal/**` and the
  actuator detail — see
  [`../adr/0006-internal-api-access.md`](../adr/0006-internal-api-access.md).
  Per-user auth arrives with the first user-specific resource.
- Only `GET` is permitted here. A write is refused with `401` rather than
  `405`: there are no write endpoints, so nothing is routed.
- Errors use RFC 9457 problem details (`application/problem+json`),
  produced centrally by `GlobalExceptionHandler`. Statuses are the usual
  ones — `400` for a value the domain rejects, `404` for an unknown path,
  `500` only for a genuine fault:

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
             "tempC": 27.4, "rainMm": 18.0, "isForecast": false }]
}]
```

One temperature per day, not a min/max range — that is what the upstream
source records. `condition` is derived from rainfall and wind, which are what
the source actually provides.

### `GET /api/v1/market-insight`

The narrative commentary shown alongside the numbers.

```json
{ "text": "Giá tiêu trong nước tiếp tục…", "updatedAtLabel": "08:00, 01/09" }
```

### `GET /actuator/health`

Liveness for deployment tooling. Not part of the versioned contract.

## Data source

Served from PostgreSQL (`backend/src/main/java/.../service/db/`), reading the
tables described in [`../database/README.md`](../database/README.md).

Two consequences worth knowing when reading the responses:

- `asOfDate` is the latest **observed** date, not today's date. Prices are
  collected during the day, so the newest row is routinely yesterday's.
- A change of `0` is common and real: pepper is quoted in 500–1000 VND steps
  and often does not move day to day.
