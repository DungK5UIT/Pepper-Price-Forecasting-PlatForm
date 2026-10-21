#!/usr/bin/env bash
# One-shot setup on the VPS (Ubuntu 24.04), run as root:
#   bash infra/vps/install.sh
# Asks only for the two secrets that exist outside this machine (Supabase DB
# password, ngrok authtoken) plus the ngrok domain; the internal credentials
# are generated here, since only this stack and its cron job use them.
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v docker >/dev/null || ! docker compose version >/dev/null 2>&1; then
  apt-get update -qq
  DEBIAN_FRONTEND=noninteractive apt-get install -y -qq docker.io docker-compose-v2 curl
  systemctl enable --now docker
fi
if [ ! -s .env ]; then
  ex=../../backend/.env.example
  url=$(grep '^SUPABASE_DB_URL=' "$ex" | cut -d= -f2-)
  usr=$(grep '^SUPABASE_DB_USER=' "$ex" | cut -d= -f2-)
  echo
  read -rsp "Supabase DB password (hidden): " dbpw; echo
  read -rp  "ngrok authtoken: " tok
  read -rp  "ngrok domain (xxx.ngrok-free.app): " dom
  dom=${dom#https://}; dom=${dom%/}
  umask 077
  cat > .env <<ENV
SUPABASE_DB_URL=$url
SUPABASE_DB_USER=$usr
SUPABASE_DB_PASSWORD=$dbpw
INTERNAL_API_USER=ml-service
INTERNAL_API_PASSWORD=$(openssl rand -hex 24)
APP_MLSERVICE_TOKEN=$(openssl rand -hex 24)
NGROK_AUTHTOKEN=$tok
NGROK_DOMAIN=$dom
ENV
fi
n=$(grep -cE '^[A-Z_]+=.+' .env || true)
[ "$n" -ge 8 ] || { echo "infra/vps/.env has only $n of 8 values - delete it and rerun"; exit 1; }
nice -n 10 docker compose --env-file .env up -d --build
docker image prune -f >/dev/null
chmod +x jobs.sh
chmod +x autodeploy.sh catchup.sh
cat > /etc/cron.d/giatieuviet <<CRON
# 00:00 UTC = 07:00 VN: daily price/weather/forecast jobs
0 0 * * * root $(pwd)/jobs.sh >> /var/log/giatieuviet-jobs.log 2>&1
# 20:00 UTC = 03:00 VN: redeploy if main moved and CI is green
0 20 * * * root $(pwd)/autodeploy.sh >> /var/log/giatieuviet-deploy.log 2>&1
# after a reboot / host stop, and every 6 h: rerun the jobs if health is STALE
@reboot root $(pwd)/catchup.sh >> /var/log/giatieuviet-jobs.log 2>&1
30 */6 * * * root $(pwd)/catchup.sh >> /var/log/giatieuviet-jobs.log 2>&1
CRON
echo
echo "Waiting for the backend..."
for i in $(seq 1 40); do
  curl -fs -o /dev/null http://127.0.0.1:8080/actuator/health && break
  sleep 5
done
docker compose --env-file .env ps
docker stats --no-stream
curl -s http://127.0.0.1:8080/actuator/health; echo
echo "Public URL: https://$(grep ^NGROK_DOMAIN= .env | cut -d= -f2)"
