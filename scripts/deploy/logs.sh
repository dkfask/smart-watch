#!/usr/bin/env bash
# ============================================================================
# scripts/deploy/logs.sh — Convenience wrapper around `docker compose logs`
# ============================================================================
# Run as:    the `deploy` user, whenever you want to see logs.
# Purpose:   Tails recent log lines for one (or all) compose services.
#
# Usage:
#   bash logs.sh           # all services, last 200 lines, follow
#   bash logs.sh app       # only the app service
#   bash logs.sh mysql     # only MySQL
#
# Tip: when you Ctrl-C out of this, try:
#   docker compose logs --since=10m app
# to see only the last 10 minutes.
# ============================================================================
set -euo pipefail

HOME_DIR="${SMART_WATCH_HOME:-/home/deploy/smart-watch}"
SERVICE="${1:-}"

cd "${HOME_DIR}"

trap 'printf "\n(use: docker compose logs --since=10m app to see only recent lines)\n"' INT TERM

if [[ -z "${SERVICE}" ]]; then
    exec docker compose logs --tail=200 -f
else
    exec docker compose logs --tail=200 -f "${SERVICE}"
fi
