# Frontend image: Next.js standalone server on Node 22.
#
# The build needs no backend: both pages render per request (see their
# `export const dynamic`), so nothing is fetched while this image is built.

# --- dependencies --------------------------------------------------------
FROM node:22-alpine AS deps
WORKDIR /app
# Its own layer, keyed on the lockfile, so a source edit does not reinstall.
COPY package.json package-lock.json ./
RUN npm ci

# --- build ---------------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules node_modules/
COPY . .
# Telemetry off: a build should not talk to anyone it was not asked to.
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build

# --- runtime -------------------------------------------------------------
FROM node:22-alpine AS runtime
WORKDIR /app

ENV TZ=Asia/Ho_Chi_Minh \
    NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME=0.0.0.0

RUN addgroup --system --gid 10001 app \
 && adduser --system --uid 10001 --ingroup app app
USER app

# The three pieces a standalone build needs. `server.js` serves `public/` and
# `.next/static/` itself once they sit beside it, which is why they are copied
# in rather than left behind — there is no CDN in front of this.
COPY --from=build --chown=app:app /app/.next/standalone ./
COPY --from=build --chown=app:app /app/.next/static ./.next/static
COPY --from=build --chown=app:app /app/public ./public

EXPOSE 3000

CMD ["node", "server.js"]
