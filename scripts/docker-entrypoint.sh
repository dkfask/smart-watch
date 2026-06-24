#!/usr/bin/env bash
# ============================================================================
# docker-entrypoint.sh — Smart Watch container entrypoint
# ============================================================================
# Responsibilities:
#   1. Wait for MySQL to accept TCP connections (compose `depends_on:
#      service_healthy` is best-effort; this is the belt-and-braces fallback).
#   2. Ensure the mpband data subdirectories exist (raw/, devices/).
#   3. exec the Spring Boot JAR under the JVM defaults baked into the image.
#
# Required environment (injected by docker-compose.yml):
#   DB_URL           e.g. jdbc:mysql://mysql:3306/smart_watch?...
#   DB_USERNAME
#   DB_PASSWORD
#   APP_MPBAND_SAVEDIR  default /app/mpband_data
# ============================================================================
set -euo pipefail

# ---- 1. Validate required env ------------------------------------------------
: "${DB_URL:?DB_URL not set}"
: "${DB_USERNAME:?DB_USERNAME not set}"
: "${DB_PASSWORD:?DB_PASSWORD not set}"

# ---- 2. Parse host/port out of the JDBC URL ---------------------------------
# Examples handled:
#   jdbc:mysql://mysql:3306/smart_watch?...
#   jdbc:mysql://mysql:3306/smart_watch
HOST=$(echo "$DB_URL" | sed -E 's#jdbc:mysql://([^:/]+).*#\1#')
PORT=$(echo "$DB_URL" | sed -E 's#jdbc:mysql://[^:/]+:([0-9]+).*#\1#')
PORT="${PORT:-3306}"

# ---- 3. Wait for MySQL (up to ~2 minutes) -----------------------------------
echo "[entrypoint] Waiting for MySQL at ${HOST}:${PORT}..."
for i in $(seq 1 60); do
  if (echo > "/dev/tcp/${HOST}/${PORT}") >/dev/null 2>&1; then
    echo "[entrypoint] MySQL is reachable after ${i} attempt(s)."
    break
  fi
  if [ "$i" -eq 60 ]; then
    echo "[entrypoint] ERROR: MySQL did not become reachable in time." >&2
    exit 1
  fi
  sleep 2
done

# ---- 4. Prepare the mpband data directory -----------------------------------
SAVE_DIR="${APP_MPBAND_SAVEDIR:-/app/mpband_data}"
mkdir -p "${SAVE_DIR}/raw" "${SAVE_DIR}/devices"
echo "[entrypoint] mpband data directory: ${SAVE_DIR}"

# ---- 5. Hand off to the JVM -------------------------------------------------
# JAVA_TOOL_OPTIONS is honoured automatically; JAVA_OPTS lets operators
# append extra flags at runtime if needed (set in docker-compose env).
echo "[entrypoint] Starting Spring Boot application..."
exec java ${JAVA_OPTS:-} -jar /app/demo2.jar \
    --spring.profiles.active="${SPRING_PROFILES_ACTIVE:-docker}"
