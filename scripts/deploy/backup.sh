#!/usr/bin/env bash
# ============================================================================
# scripts/deploy/backup.sh — MySQL logical backup with rotation
# ============================================================================
# Run as:    the `deploy` user, typically from cron (see config/crontab.example).
# Purpose:   mysqldump the entire `smart_watch` database to a timestamped
#            gzip file under BACKUP_DIR (default /home/deploy/backups), and
#            prune backups older than RETAIN_DAYS (default 14).
#
# Exit codes: 0 success, non-zero on any failure (cron can react to this).
# ============================================================================
set -euo pipefail

HOME_DIR="${SMART_WATCH_HOME:-/home/deploy/smart-watch}"
BACKUP_DIR="${BACKUP_DIR:-/home/deploy/backups}"
RETAIN_DAYS="${RETAIN_DAYS:-14}"
TS="$(date +%Y%m%d-%H%M%S)"
OUT="${BACKUP_DIR}/smart_watch_${TS}.sql.gz"

log() { printf '\033[1;34m[backup]\033[0m %s\n' "$*"; }
die() { printf '\033[1;31m[backup][fatal]\033[0m %s\n' "$*" >&2; exit 1; }

mkdir -p "${BACKUP_DIR}"

# ---- Load secrets from .env (without echoing them) -------------------------
if [[ ! -f "${HOME_DIR}/.env" ]]; then
    die ".env not found at ${HOME_DIR}/.env — was deploy.sh run?"
fi
set -a
# shellcheck disable=SC1090,SC1091
. "${HOME_DIR}/.env"
set +a

: "${MYSQL_DATABASE:?MYSQL_DATABASE not set in .env}"
: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD not set in .env}"

# ---- 1. Run mysqldump --------------------------------------------------------
log "Dumping database '${MYSQL_DATABASE}' → ${OUT}"
if ! docker compose -f "${HOME_DIR}/docker-compose.yml" \
        exec -T mysql \
        mysqldump --single-transaction --routines --triggers --events \
                  --quick --hex-blob \
                  -uroot -p"${MYSQL_ROOT_PASSWORD}" \
                  "${MYSQL_DATABASE}" \
        | gzip -9 > "${OUT}"; then
    rm -f "${OUT}"
    die "mysqldump failed"
fi

# ---- 2. Verify integrity -----------------------------------------------------
if ! gunzip -t "${OUT}" 2>/dev/null; then
    rm -f "${OUT}"
    die "backup file failed integrity check (gunzip -t)"
fi

# ---- 3. Rotate old backups ---------------------------------------------------
PRUNED=$(find "${BACKUP_DIR}" -maxdepth 1 -name 'smart_watch_*.sql.gz' -mtime +"${RETAIN_DAYS}" -print -delete | wc -l)

# ---- 4. Report ---------------------------------------------------------------
SIZE=$(du -h "${OUT}" | awk '{print $1}')
log "OK: ${OUT} (${SIZE}); pruned ${PRUNED} backup(s) older than ${RETAIN_DAYS} days."
