#!/usr/bin/env bash
# Catch-up for the daily jobs (cron @reboot and every 6 h).
# If the VPS was off at 07:00 VN, or a job failed, /actuator/health reports
# STALE instead of UP. Wait for the backend, then rerun jobs.sh only in that case.
set -uo pipefail
cd "$(dirname "$0")"
h=""
for i in $(seq 1 60); do
  h=$(curl -fs http://127.0.0.1:8080/actuator/health) && break
  sleep 10
done
date -u
if [ -z "$h" ]; then
  echo "backend not healthy after 10 min"; exit 1
fi
case "$h" in
  *'"status":"UP"'*) echo "health UP, nothing to catch up" ;;
  *) echo "health: $h -> running jobs"; ./jobs.sh ;;
esac
