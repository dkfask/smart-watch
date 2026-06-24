#!/usr/bin/env bash
# ============================================================================
# scripts/deploy/update.sh — Repeatable application update
# ============================================================================
# Run as:    the `deploy` user, whenever you want to deploy a new commit.
# Purpose:   git pull → (optional DB backup) → rebuild app image → restart app.
#
# Flags:
#   --backup    Take a DB backup before updating (uses backup.sh).
#   --prune     Prune dangling Docker images after a successful update.
#
# Expected downtime: ~30 s while Spring Boot restarts and Netty rebinds.
# Devices reconnect automatically once the new process binds port 9000.
# ============================================================================
set -euo pipefail

HOME_DIR="${SMART_WATCH_HOME:-/home/deploy/smart-watch}"
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-180}"
HEALTH_INTERVAL_SECONDS=5

DO_BACKUP=0
DO_PRUNE=0
for arg in "$@"; do
    case "$arg" in
        --backup) DO_BACKUP=1 ;;
        --prune)  DO_PRUNE=1 ;;
        -h|--help)
            sed -n '2,15p' "$0"
            exit 0
            ;;
        *)
            printf 'unknown flag: %s\n' "$arg" >&2; exit 2 ;;
    esac
done

log()  { printf '\033[1;34m[update]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[update][warn]\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31m[update][fatal]\033[0m %s\n' "$*" >&2; exit 1; }

cd "${HOME_DIR}"

# ---- 1. Pull latest code ----------------------------------------------------
log "Pulling latest from origin..."
git fetch --quiet origin
git pull --ff-only

# ---- 2. Optional backup ------------------------------------------------------
if (( DO_BACKUP )); then
    log "Taking database backup before update..."
    bash "${HOME_DIR}/scripts/deploy/backup.sh" || die "backup failed — update aborted"
fi

# ---- 3. Rebuild & restart ----------------------------------------------------
log "Rebuilding app image..."
docker compose build app

log "Restarting app (expect ~30 s downtime; Netty will rebind on 9000)..."
docker compose up -d app

# ---- 4. Wait for health ------------------------------------------------------
log "Waiting for application health (timeout ${HEALTH_TIMEOUT_SECONDS}s)..."
deadline=$(( $(date +%s) + HEALTH_TIMEOUT_SECONDS ))
while true; do
    if curl -fsS --max-time 5 http://localhost:8080/actuator/health >/dev/null 2>&1; then
        break
    fi
    if (( $(date +%s) >= deadline )); then
        warn "Health check timed out. Recent app logs:"
        docker compose logs --tail=200 app >&2 || true
        die "update failed health check — see logs above"
    fi
    sleep "${HEALTH_INTERVAL_SECONDS}"
done

# ---- 5. Optional prune -------------------------------------------------------
if (( DO_PRUNE )); then
    log "Pruning dangling Docker images..."
    docker image prune -f
fi

# ---- 6. Summary --------------------------------------------------------------
SHORT_SHA="$(git rev-parse --short HEAD)"
log "Update complete. Last commit: ${SHORT_SHA}"
log "Run 'bash ${HOME_DIR}/scripts/deploy/logs.sh app' to watch live logs."
