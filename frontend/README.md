# Frontend

Next.js (App Router) + TypeScript UI for the platform.

**Owns**: presentation, client-side state, calling the Java backend's
public REST API.

**Does not own**: business logic, direct database access, direct calls to
the ML service — all data and computation come through the backend's API.

## Status

Scaffolded with `create-next-app` (TypeScript, Tailwind CSS v4, App
Router). Two routes are implemented against **mock data** — the Java
backend does not exist yet, so `src/lib/mock-data.ts` stands in for it.
Every function there is shaped like a future API response and is the only
place that needs to change once real endpoints exist.

- `/` — price dashboard: today's price, a day/week/month forecast chart,
  regional prices, and a compact weather snapshot.
- `/weather` — 7-day weather detail for the 6 key pepper-growing
  provinces.

## Development

```bash
npm install
npm run dev      # http://localhost:3000
npm run build
npm run lint
```

## Structure

```
src/
  app/            # routes (page.tsx per route), root layout, global CSS
  components/      # presentational + a few client components (charts, toggles)
  lib/            # types.ts (shared shapes) + mock-data.ts (placeholder data layer)
  assets/images/   # locally-sourced photos (Unsplash, free license), statically imported
```

## Design system

Colors, type (Cormorant Garamond + Be Vietnam Pro) and the asymmetric
"blob corner" card radius are design tokens in `src/app/globals.css`
(Tailwind v4 `@theme`), not hardcoded per component.
