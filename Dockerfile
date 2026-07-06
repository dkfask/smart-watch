# ============================================================================
# Smart Watch Monitoring System — container image
# ============================================================================
# Three-stage build:
#   1. frontend-builder : node:20-alpine, builds the Vue SPA via Vite.
#   2. backend-builder  : eclipse-temurin:21-jdk, builds the Spring Boot JAR
#                         with the SPA already in src/main/resources/static.
#   3. runtime          : eclipse-temurin:21-jre-alpine, runs the JAR as a
#                         non-root user with tini as PID 1.
#
# Build:    docker build -t smart-watch:latest .
# Run:      docker compose up -d  (see docker-compose.yml)
# ============================================================================

# ----------------------------------------------------------------------------
# Stage 1 — Build the Vue 3 SPA
# ----------------------------------------------------------------------------
FROM node:20-alpine AS frontend-builder

WORKDIR /build

# Use the Taobao npm mirror (registry.npmjs.org is blocked from many
# mainland-China networks). Pinned at build time so future lockfile
# resolution is reproducible.
RUN npm config set registry https://registry.npmmirror.com

# Copy manifests first so this layer is cached when only sources change.
COPY frontend/package.json frontend/package-lock.json* ./frontend/

# Use `npm ci` for reproducible, lockfile-driven installs.
# `--no-audit --no-fund` keeps the log quiet inside CI.
RUN cd frontend && npm ci --no-audit --no-fund

# Now bring in the rest of the frontend sources and build.
# vite.config.js sets build.outDir=../src/main/resources/static, so the
# built assets land at /build/src/main/resources/static.
COPY frontend/ ./frontend/
RUN cd frontend && npm run build

# Sanity check: the dist directory must contain at least index.html.
RUN test -f /build/src/main/resources/static/index.html \
    || (echo "ERROR: frontend build did not produce index.html" && exit 1)

# ----------------------------------------------------------------------------
# Stage 2 — Build the Spring Boot JAR (Gradle wrapper)
# ----------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS backend-builder

WORKDIR /build

# Copy the Gradle wrapper and build descriptors first — this layer is the
# most expensive to invalidate, so we keep it as stable as possible.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x ./gradlew && ./gradlew --version > /dev/null

# Copy Java sources and the just-built SPA, then assemble the bootJar.
# `-x test` skips JaCoCo coverage; `skipFrontend=true` prevents the Gradle
# npmBuild task from running (we already produced the static assets).
COPY src ./src
COPY --from=frontend-builder /build/src/main/resources/static ./src/main/resources/static
RUN ./gradlew --no-daemon clean bootJar -x test -PskipFrontend=true

# Verify the JAR exists and is non-trivial in size.
RUN test -f build/libs/demo2.jar \
    && test $(stat -c%s build/libs/demo2.jar) -gt 1000000 \
    || (echo "ERROR: demo2.jar missing or suspiciously small" && exit 1)

# ----------------------------------------------------------------------------
# Stage 3 — Runtime image
# ----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# tini: clean PID 1, signal forwarding, zombie reaping.
# bash/curl/tzdata: used by docker-entrypoint.sh and the healthcheck.
# tzdata: gives us /usr/share/zoneinfo/Asia/Shanghai.
RUN apk add --no-cache bash curl tini tzdata

# Create the non-root runtime user up-front so file ownership is correct.
RUN addgroup -S app && adduser -S app -G app

# JVM defaults — applied to every `java` invocation via JAVA_TOOL_OPTIONS.
# -XX:+UseContainerSupport + MaxRAMPercentage honour cgroup memory limits.
# -XX:+ExitOnOutOfMemoryError causes fast container restarts on OOM.
# -Djava.security.egd=file:/dev/./urandom avoids /dev/random blocking on Alpine.
ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom -Duser.timezone=Asia/Shanghai" \
    SERVER_PORT=8080

WORKDIR /app

# Application artefact and entrypoint script.
COPY --from=backend-builder /build/build/libs/demo2.jar /app/demo2.jar
COPY scripts/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

# Bake the MySQL init scripts into the runtime image so the official MySQL
# image picks them up via /docker-entrypoint-initdb.d/ on first volume init.
# (We do NOT bind-mount them because MySQL only runs them on a fresh data
# volume; bind-mounting would surprise users who expect re-runs.)
COPY docker/mysql/init/ /docker-entrypoint-initdb.d/

# Prepare the data directory and grant ownership to the runtime user.
RUN mkdir -p /app/mpband_data/raw /app/mpband_data/devices \
    && chmod +x /usr/local/bin/docker-entrypoint.sh \
    && chown -R app:app /app

# Netty device port (9000) and TCP echo port (9090) are part of the app's
# runtime contract. Spring Security CORS is a non-issue because the SPA is
# served from the same origin at 8080.
EXPOSE 8080 9000 9090

# Healthcheck uses Spring Boot Actuator (/actuator/health). The endpoint is
# exposed via the `docker` Spring profile (see application-docker.properties).
HEALTHCHECK --interval=15s --timeout=5s --start-period=120s --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

# tini forwards SIGTERM/SIGINT to the JVM so Hikari, the scheduler, and
# Netty can shut down cleanly instead of being killed mid-flush.
USER app
ENTRYPOINT ["/sbin/tini", "--", "/usr/local/bin/docker-entrypoint.sh"]
