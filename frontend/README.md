# Frontend

Next.js (App Router) + TypeScript UI for the platform.

**Owns**: presentation, client-side state, calling the Java backend's
public REST API.

**Does not own**: business logic, direct database access, direct calls to
the ML service — all data and computation come through the backend's API.

## Status

Two server-rendered routes, both reading the backend's public API
(`docs/api/README.md`) through `src/lib/api.ts`:

- `/` — price dashboard: today's price and week-ahead range, a
  day/week/month forecast chart, period stats, regional prices and a
  compact weather snapshot.
- `/weather` — 7-day weather for the six pepper-growing provinces.

Fetches run on the server and are cached for 5 minutes; the browser never
calls the backend. If the backend fails, `src/app/error.tsx` shows a reload
page.

## Development

```bash
npm install
npm run dev      # http://localhost:3000 — needs the backend on :8080
npm run lint
npm run build    # does not need the backend: pages render per request
```

`API_BASE_URL` points at the backend (default `http://localhost:8080`, see
`.env.example`).

## Structure

```
src/
  app/            # routes (page.tsx per route), root layout, error page, global CSS
  components/     # presentational components + the chart and granularity switch
  lib/            # api.ts (backend client), types.ts (API shapes), format.ts (VND, %, dates)
  assets/images/  # locally stored photos (Unsplash, free licence), statically imported
```

## Design system

Colors, type (Cormorant Garamond + Be Vietnam Pro) and the asymmetric
"blob corner" card radius are design tokens in `src/app/globals.css`
(Tailwind v4 `@theme`), not hardcoded per component.
