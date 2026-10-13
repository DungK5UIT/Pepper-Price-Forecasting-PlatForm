#!/usr/bin/env bash
# One-shot setup on the VPS (Ubuntu 24.04), run as root from the repo root:
#   bash infra/vps/install.sh
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v docker >/dev/null || ! docker compose version >/dev/null 2>&1; then
  apt-get update -y
  apt-get install -y docker.io docker-compose-v2
  systemctl enable --now docker
fi
if [ ! -s .env ]; then
  echo
  echo "Paste the whole content of backend/.env, then press Enter and Ctrl-D:"
  cat > .env.tmp
  grep -E '^(SUPABASE_DB_URL|SUPABASE_DB_USER|SUPABASE_DB_PASSWORD|INTERNAL_API_USER|INTERNAL_API_PASSWORD|APP_MLSERVICE_TOKEN)=' .env.tmp | tr -d '\r' > .env
  rm -f .env.tmp
  read -rp "ngrok authtoken: " tok; echo "NGROK_AUTHTOKEN=$tok" >> .env
  read -rp "ngrok static domain (xxx.ngrok-free.app): " dom; echo "NGROK_DOMAIN=${dom#https://}" >> .env
  chmod 600 .env
fi
n=$(grep -cE '^[A-Z_]+=.+' .env || true)
[ "$n" -ge 8 ] || { echo "infra/vps/.env has only $n of 8 values set - fix it and rerun"; exit 1; }
nice -n 10 docker compose --env-file .env up -d --build
docker image prune -f >/dev/null
echo
echo "Waiting for the backend..."
for i in $(seq 1 30); do
  if docker compose --env-file .env exec -T backend sh -c 'exec 3<>/dev/tcp/127.0.0.1/8080' 2>/dev/null; then break; fi
  sleep 5
done
docker compose --env-file .env ps
docker stats --no-stream
echo
echo "Public URL: https://$(grep ^NGROK_DOMAIN= .env | cut -d= -f2)"
