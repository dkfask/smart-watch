#!/usr/bin/env bash
# ============================================================================
# scripts/deploy/deploy.sh — First-time application deployment
# ============================================================================
# Run as:    the `deploy` user, ONCE after scripts/deploy/bootstrap.sh.
# Purpose:   Clone the repo, generate secrets, build images, start the stack,
#            and verify health.
#
# Required env (override as needed):
#   SMART_WATCH_REPO_URL   git URL (default: https://github.com/<user>/smart-watch.git)
#   SMART_WATCH_BRANCH     git branch (default: main)
#   SMART_WATCH_HOME       install path (default: /home/deploy/smart-watch)
#
# Idempotent: re-running is safe — git pull + `docker compose up -d` again.
# ============================================================================
set -euo pipefail

# ---- Configuration ----------------------------------------------------------
REPO_URL="${SMART_WATCH_REPO_URL:-https://github.com/your-org/smart-watch.git}"
BRANCH="${SMART_WATCH_BRANCH:-main}"
HOME_DIR="${SMART_WATCH_HOME:-/home/deploy/smart-watch}"
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-240}"  # 4 min
HEALTH_INTERVAL_SECONDS=5

# ---- Helpers ----------------------------------------------------------------
log()  { printf '\033[1;34m[deploy]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[deploy][warn]\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31m[deploy][fatal]\033[0m %s\n' "$*" >&2; exit 1; }

# ---- 1. Prereq checks --------------------------------------------------------
log "Checking prerequisites..."
for cmd in docker git openssl curl; do
    command -v "$cmd" >/dev/null 2>&1 || die "missing: $cmd — did you run scripts/deploy/bootstrap.sh as root?"
done
docker compose version >/dev/null 2>&1 || die "docker compose plugin missing"

# ---- 2. Clone or pull the repo ---------------------------------------------
log "Preparing repository at ${HOME_DIR}..."
if [[ -d "${HOME_DIR}/.git" ]]; then
    log "Repo exists; pulling latest from ${BRANCH}..."
    cd "${HOME_DIR}"
    git fetch --quiet origin
    git checkout --quiet "${BRANCH}"
    git pull --ff-only
else
    log "Cloning ${REPO_URL} (branch ${BRANCH})..."
    git clone --branch "${BRANCH}" --depth 50 "${REPO_URL}" "${HOME_DIR}"
    cd "${HOME_DIR}"
fi

# ---- 3. .env generation (only on first run) -------------------------------
if [[ ! -f .env ]]; then
    log "First run detected — generating .env from .env.docker template..."
    cp .env.docker .env
    chmod 600 .env

    # Replace placeholder passwords with strong random values.
    # sed -i works in-place; we do it twice (once per var).
    ROOT_PW="$(openssl rand -hex 16)"
    USER_PW="$(openssl rand -hex 16)"
    sed -i "s|^MYSQL_ROOT_PASSWORD=.*|MYSQL_ROOT_PASSWORD=${ROOT_PW}|" .env
    sed -i "s|^MYSQL_PASSWORD=.*|MYSQL_PASSWORD=${USER_PW}|" .env

    log "Generated strong random MYSQL_ROOT_PASSWORD and MYSQL_PASSWORD."
    log "File .env is chmod 600. Run 'cat .env' (as deploy) to view."

    printf '\nGenerated credentials are stored in %s/.env (chmod 600).\n' "${HOME_DIR}"
    printf 'Press ENTER to continue, or type "override" to edit .env first: '
    read -r choice
    if [[ "${choice}" == "override" ]]; then
        "${EDITOR:-nano}" .env
    fi
else
    log ".env already exists — leaving untouched."
fi

# ---- 4. Build & start --------------------------------------------------------
log "Building Docker images (first run may take 5-10 minutes)..."
docker compose build

log "Starting the stack..."
docker compose up -d

# ---- 5. Wait for health ------------------------------------------------------
log "Waiting for application health (timeout ${HEALTH_TIMEOUT_SECONDS}s)..."
deadline=$(( $(date +%s) + HEALTH_TIMEOUT_SECONDS ))
while true; do
    if curl -fsS --max-time 5 http://localhost:8080/actuator/health >/dev/null 2>&1; then
        log "Application is healthy."
        break
    fi
    if (( $(date +%s) >= deadline )); then
        warn "Health check timed out. Recent app logs:"
        docker compose logs --tail=200 app >&2 || true
        die "deployment failed health check — see logs above"
    fi
    sleep "${HEALTH_INTERVAL_SECONDS}"
done

# ---- 6. Print summary --------------------------------------------------------
PUBLIC_IP="$(curl -fsS --max-time 5 https://api.ipify.org 2>/dev/null || hostname -I | awk '{print $1}')"
cat <<EOF

============================================================================
Deployment complete!
============================================================================
  Application URL:    http://${PUBLIC_IP}:8080
  Device comms port:  ${PUBLIC_IP}:9000
  Default credentials: admin / admin123   ← CHANGE IMMEDIATELY
  Install path:        ${HOME_DIR}
  Logs:                bash ${HOME_DIR}/scripts/deploy/logs.sh app
  Update later:        bash ${HOME_DIR}/scripts/deploy/update.sh --backup
  Backup:              bash ${HOME_DIR}/scripts/deploy/backup.sh

Verify from your laptop:
  curl http://${PUBLIC_IP}:8080/actuator/health
  → should return: {"status":"UP"}
============================================================================
EOF
