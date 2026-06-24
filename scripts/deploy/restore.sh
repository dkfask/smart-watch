#!/usr/bin/env bash
# ============================================================================
# scripts/deploy/restore.sh — Restore MySQL from a backup file
# ============================================================================
# Run as:    the `deploy` user, when you need to roll back the database.
# Purpose:   Stop the app, drop & recreate the database, import the dump,
#            restart the app, verify health.
#
# Usage:     bash restore.sh /path/to/smart_watch_YYYYMMDD-HHMMSS.sql.gz [--yes]
#
# The --yes flag skips the interactive confirmation (for scripted recovery).
# ============================================================================
set -euo pipefail

HOME_DIR="${SMART_WATCH_HOME:-/home/deploy/smart-watch}"
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-180}"

SKIP_CONFIRM=0
BACKUP_FILE=""
for arg in "$@"; do
    case "$arg" in
        --yes|-y) SKIP_CONFIRM=1 ;;
        *.sql.gz) BACKUP_FILE="$arg" ;;
        -h|--help)
            sed -n '2,18p' "$0"
            exit 0
            ;;
        *)
            printf 'unknown arg: %s\n' "$arg" >&2; exit 2 ;;
    esac
done

[[ -n "${BACKUP_FILE}" ]] || { sed -n '2,18p' "$0"; exit 2; }
[[ -f "${BACKUP_FILE}" ]] || { echo "file not found: ${BACKUP_FILE}" >&2; exit 2; }

log() { printf '\033[1;34m[restore]\033[0m %s\n' "$*"; }
die() { printf '\033[1;31m[restore][fatal]\033[0m %s\n' "$*" >&2; exit 1; }

# ---- 1. Verify backup file --------------------------------------------------
log "Verifying backup file integrity..."
gunzip -t "${BACKUP_FILE}" || die "backup file is corrupt (gunzip -t failed)"

# ---- 2. Confirm with the user -----------------------------------------------
if (( ! SKIP_CONFIRM )); then
    printf '\nThis will REPLACE the current database with the contents of:\n  %s\n\n' "${BACKUP_FILE}"
    printf 'Type "yes" to continue: '
    read -r answer
    [[ "${answer}" == "yes" ]] || die "aborted by user"
fi

# ---- 3. Load secrets --------------------------------------------------------
[[ -f "${HOME_DIR}/.env" ]] || die ".env not found at ${HOME_DIR}/.env"
set -a
# shellcheck disable=SC1090,SC1091
. "${HOME_DIR}/.env"
set +a

: "${MYSQL_DATABASE:?MYSQL_DATABASE not set in .env}"
: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD not set in .env}"

# ---- 4. Stop the app (so no writes race the restore) ------------------------
log "Stopping app container..."
docker compose -f "${HOME_DIR}/docker-compose.yml" stop app

# ---- 5. Drop & recreate database, then import -------------------------------
log "Recreating database '${MYSQL_DATABASE}'..."
docker compose -f "${HOME_DIR}/docker-compose.yml" exec -T mysql \
    mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" \
    -e "DROP DATABASE IF EXISTS \`${MYSQL_DATABASE}\`;
        CREATE DATABASE \`${MYSQL_DATABASE}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

log "Importing dump (this may take a few minutes)..."
if ! zcat "${BACKUP_FILE}" | docker compose -f "${HOME_DIR}/docker-compose.yml" exec -T mysql \
        mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}"; then
    die "import failed — database is now in an inconsistent state. Investigate before restarting app."
fi

# ---- 6. Restart app ---------------------------------------------------------
log "Starting app container..."
docker compose -f "${HOME_DIR}/docker-compose.yml" start app

# ---- 7. Wait for health ----------------------------------------------------
log "Waiting for application health (timeout ${HEALTH_TIMEOUT_SECONDS}s)..."
deadline=$(( $(date +%s) + HEALTH_TIMEOUT_SECONDS ))
while true; do
    if curl -fsS --max-time 5 http://localhost:8080/actuator/health >/dev/null 2>&1; then
        log "Application is healthy. Restore complete."
        exit 0
    fi
    if (( $(date +%s) >= deadline )); then
        die "app failed to come back healthy — check: docker compose logs app"
    fi
    sleep 5
done
