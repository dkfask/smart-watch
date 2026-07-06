#!/usr/bin/env bash
# ============================================================================
# scripts/deploy/healthcheck.sh — HTTP health probe for cron
# ============================================================================
# Run as:    the `deploy` user, from cron (see config/crontab.example).
# Purpose:   Probe /actuator/health every 5 minutes; log failures only.
#
# Exit codes: 0 healthy, 1 unhealthy.
# Alerting (webhook / email / Slack) is the operator's responsibility — this
# script intentionally does NOT call home. Wire it up to your monitoring
# stack using the exit code and the LOG_DIR/healthcheck.log file.
# ============================================================================
set -euo pipefail

LOG_DIR="${LOG_DIR:-/home/deploy/logs}"
URL="http://localhost:${APP_HTTP_PORT:-8080}/actuator/health"

mkdir -p "${LOG_DIR}"

if curl -fsS --max-time 10 "${URL}" >/dev/null 2>&1; then
    exit 0
fi

# Failure path: log a single line, then exit non-zero so cron captures it.
printf '%s FAIL %s\n' "$(date -Iseconds)" "${URL}" >> "${LOG_DIR}/healthcheck.log"
exit 1
