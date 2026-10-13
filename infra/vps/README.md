# VPS deployment (backend + ML service)

Replaces Render (ADR-0008), whose free tier slept after 15 minutes idle and
cold-started the JVM for 30-60 s on the next visit. The frontend stays on
Vercel; the database stays on Supabase.

| Piece | How |
|---|---|
| Runtime | `compose.yaml`: backend, ml-service, ngrok — each memory-capped, the VPS is shared |
| Public URL | ngrok static domain (the VPS has no public IPv4, so the tunnel dials out) |
| Daily jobs | host cron 07:00 VN → `jobs.sh` → `/internal/v1/jobs/*` on 127.0.0.1 |
| CI | `.github/workflows/ci.yml` on every push / PR |
| CD backend | host cron 03:00 VN → `autodeploy.sh`: if `main` moved **and** its CI checks are green, pull and `docker compose up -d --build` |
| CD frontend | Vercel Git integration |

First install, as root on the VPS:

    git clone https://github.com/DungK5UIT/Pepper-Price-Forecasting-PlatForm /root/pepper
    bash /root/pepper/infra/vps/install.sh

Logs: `/var/log/giatieuviet-deploy.log`, `/var/log/giatieuviet-jobs.log`,
`docker compose logs` in this folder.
