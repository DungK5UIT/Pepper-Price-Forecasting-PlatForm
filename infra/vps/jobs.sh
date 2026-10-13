#!/usr/bin/env bash
# Daily jobs (cron 00:00 UTC = 07:00 Asia/Ho_Chi_Minh): price -> weather -> forecast.
# Replaces .github/workflows/scheduled-jobs.yml now that the backend never sleeps.
set -euo pipefail
cd "$(dirname "$0")"
u=$(grep '^INTERNAL_API_USER=' .env | cut -d= -f2-)
p=$(grep '^INTERNAL_API_PASSWORD=' .env | cut -d= -f2-)
b=http://127.0.0.1:8080/internal/v1/jobs
date -u
for job in ingest-price ingest-weather refresh-forecast; do
  curl --fail --silent --show-error --retry 2 --retry-delay 30 --retry-all-errors \
    -u "$u:$p" -X POST "$b/$job"
  echo " $job ok"
done
