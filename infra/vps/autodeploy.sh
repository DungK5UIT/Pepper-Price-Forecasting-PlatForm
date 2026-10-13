#!/usr/bin/env bash
# Pull-based CD (cron, once a day): redeploy when main has a new commit whose
# GitHub CI checks all passed. The VPS has no public IPv4, so GitHub cannot
# push to it; it pulls instead. Public repo -> the API needs no token.
set -euo pipefail
cd "$(dirname "$0")/../.."
repo=DungK5UIT/Pepper-Price-Forecasting-PlatForm
date -u
git fetch -q origin main
new=$(git rev-parse origin/main)
cur=$(git rev-parse HEAD)
if [ "$new" = "$cur" ]; then echo "up to date at ${cur:0:7}"; exit 0; fi
verdict=$(curl -fsS -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/$repo/commits/$new/check-runs?per_page=100" | python3 -c '
import json, sys
runs = json.load(sys.stdin)["check_runs"]
if not runs: print("pending"); sys.exit()
if any(r["status"] != "completed" for r in runs): print("pending"); sys.exit()
bad = [r["name"] for r in runs if r["conclusion"] not in ("success", "skipped", "neutral")]
print("failed:" + ",".join(bad) if bad else "green")')
echo "main ${new:0:7} (running ${cur:0:7}): CI $verdict"
[ "$verdict" = green ] || exit 0
git checkout -q -B main "$new"
cd infra/vps
nice -n 10 docker compose --env-file .env up -d --build
docker image prune -f >/dev/null
for i in $(seq 1 40); do
  curl -fs -o /dev/null http://127.0.0.1:8080/actuator/health && { echo "deployed ${new:0:7}, backend UP"; exit 0; }
  sleep 5
done
echo "deployed ${new:0:7} but backend not healthy after 200 s"; exit 1
