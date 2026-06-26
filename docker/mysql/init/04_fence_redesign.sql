-- ============================================================================
-- 04_fence_redesign.sql — Geo-Fence redesign data model
-- ============================================================================
-- This file is part of the MySQL init sequence (runs once on a fresh data
-- volume, after 01/02/03_*.sql). It introduces:
--   - fence_geometries     : normalized geometry per fence (single source
--                            of truth for circle/polygon/rectangle math)
--   - fence_events         : state-transition event log (replaces the
--                            dual-write to fence_alerts + alarms + alerts)
--   - fence_device_state   : per-(fence, device) latest inside/outside
--                            state — avoids N+1 lookups during detection
--   - fence_subscriptions  : per-user WebSocket subscription registry
--                            for targeted (not broadcast) push
--   - fence_policies       : per-fence business-rule overrides
--                            (debounce, cooldown, dwell)
--
-- Old tables:
--   - geo_fences (kept, geometry fields de-emphasized)
--   - fence_patients (kept, unchanged)
--   - fence_alerts  -> renamed to fence_alerts_archive (data preserved)
--                       and recreated as a view over fence_events for
--                       legacy read paths.
--
-- Idempotent: every CREATE uses IF NOT EXISTS; every INSERT uses
-- INSERT ... ON DUPLICATE KEY UPDATE / INSERT IGNORE so re-running on a
-- populated DB is safe.
-- ============================================================================

USE smart_watch;

-- ----------------------------------------------------------------------------
-- 1. fence_geometries — single source of truth for fence geometry
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fence_geometries (
    fence_id        BIGINT NOT NULL PRIMARY KEY,
    geometry_type   ENUM('CIRCLE','POLYGON','RECTANGLE') NOT NULL,
    center_lat      DECIMAL(10,7) NULL,
    center_lng      DECIMAL(10,7) NULL,
    radius_m        INT NULL,
    polygon_coords  JSON NULL,
    rect_sw_lat     DECIMAL(10,7) NULL,
    rect_sw_lng     DECIMAL(10,7) NULL,
    rect_ne_lat     DECIMAL(10,7) NULL,
    rect_ne_lng     DECIMAL(10,7) NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                     ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_fence_geom FOREIGN KEY (fence_id)
        REFERENCES geo_fences(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 2. fence_events — state-transition event log (new source of truth)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fence_events (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    fence_id        BIGINT NOT NULL,
    device_id       BIGINT NOT NULL,
    patient_id      BIGINT NOT NULL,
    event_type      ENUM('ENTER','EXIT','DWELL') NOT NULL,
    latitude        DECIMAL(10,7) NOT NULL,
    longitude       DECIMAL(10,7) NOT NULL,
    occurred_at     DATETIME(3) NOT NULL,
    dwell_seconds   INT NULL,
    ack_status      ENUM('NEW','ACKED','RESOLVED','FALSE_POSITIVE')
                     NOT NULL DEFAULT 'NEW',
    ack_user_id     BIGINT NULL,
    ack_at          DATETIME NULL,
    ack_remark      VARCHAR(500) NULL,
    KEY idx_fe_fence_time (fence_id, occurred_at DESC),
    KEY idx_fe_device_time (device_id, occurred_at DESC),
    KEY idx_fe_patient_time (patient_id, occurred_at DESC),
    KEY idx_fe_unack (ack_status, occurred_at),
    CONSTRAINT fk_fe_fence  FOREIGN KEY (fence_id)
        REFERENCES geo_fences(id) ON DELETE CASCADE,
    CONSTRAINT fk_fe_device FOREIGN KEY (device_id)
        REFERENCES device(id) ON DELETE CASCADE,
    CONSTRAINT fk_fe_patient FOREIGN KEY (patient_id)
        REFERENCES patients(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 3. fence_device_state — current per-(fence, device) state
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fence_device_state (
    fence_id        BIGINT NOT NULL,
    device_id       BIGINT NOT NULL,
    is_inside       TINYINT(1) NOT NULL DEFAULT 0,
    last_changed_at DATETIME(3) NOT NULL,
    inside_since    DATETIME(3) NULL,
    last_lat        DECIMAL(10,7) NULL,
    last_lng        DECIMAL(10,7) NULL,
    outside_count   INT NOT NULL DEFAULT 0,        -- for GPS debounce
    PRIMARY KEY (fence_id, device_id),
    KEY idx_fds_device (device_id),
    CONSTRAINT fk_fds_fence  FOREIGN KEY (fence_id)
        REFERENCES geo_fences(id) ON DELETE CASCADE,
    CONSTRAINT fk_fds_device FOREIGN KEY (device_id)
        REFERENCES device(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 4. fence_subscriptions — WebSocket topic registry
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fence_subscriptions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    scope_type      ENUM('FENCE','PATIENT','DEVICE','ALL') NOT NULL,
    scope_id        BIGINT NULL,
    ws_session_id   VARCHAR(64) NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sub_user_session (user_id, ws_session_id, scope_type, scope_id),
    KEY idx_sub_session (ws_session_id),
    KEY idx_sub_scope (scope_type, scope_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 5. fence_policies — per-fence business-rule overrides
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fence_policies (
    fence_id        BIGINT PRIMARY KEY,
    enter_alert     TINYINT(1) NOT NULL DEFAULT 1,
    exit_alert      TINYINT(1) NOT NULL DEFAULT 1,
    dwell_alert     TINYINT(1) NOT NULL DEFAULT 0,
    dwell_seconds   INT NOT NULL DEFAULT 300,
    debounce_count  INT NOT NULL DEFAULT 3,
    cooldown_sec    INT NOT NULL DEFAULT 300,
    CONSTRAINT fk_fp_fence FOREIGN KEY (fence_id)
        REFERENCES geo_fences(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Global default row (id=0; treated as fallback when a fence has no row).
INSERT IGNORE INTO fence_policies
    (fence_id, enter_alert, exit_alert, dwell_alert, dwell_seconds, debounce_count, cooldown_sec)
VALUES (0, 1, 1, 0, 300, 3, 300);

-- ----------------------------------------------------------------------------
-- 6. Migrate existing geometry data → fence_geometries
-- ----------------------------------------------------------------------------
-- Circle: copy center + radius; coordinates JSON ignored.
INSERT INTO fence_geometries (fence_id, geometry_type, center_lat, center_lng, radius_m)
SELECT id, 'CIRCLE', center_lat, center_lng, radius
FROM geo_fences
WHERE type = 'circle'
ON DUPLICATE KEY UPDATE
    geometry_type = VALUES(geometry_type),
    center_lat    = VALUES(center_lat),
    center_lng    = VALUES(center_lng),
    radius_m      = VALUES(radius_m);

-- Polygon: store coordinates JSON as-is.
INSERT INTO fence_geometries (fence_id, geometry_type, polygon_coords)
SELECT id, 'POLYGON', CAST(coordinates AS JSON)
FROM geo_fences
WHERE type = 'polygon'
  AND coordinates IS NOT NULL
ON DUPLICATE KEY UPDATE
    geometry_type  = VALUES(geometry_type),
    polygon_coords = VALUES(polygon_coords);

-- Rectangle: derive bbox from the 4-vertex JSON. Coordinates are
-- [[sw_lat, sw_lng], [se_lat, se_lng], [ne_lat, ne_lng], [nw_lat, nw_lng]].
INSERT INTO fence_geometries
    (fence_id, geometry_type, rect_sw_lat, rect_sw_lng, rect_ne_lat, rect_ne_lng)
SELECT
    id,
    'RECTANGLE',
    LEAST(
        JSON_EXTRACT(coordinates, '$[0][0]'),
        JSON_EXTRACT(coordinates, '$[1][0]'),
        JSON_EXTRACT(coordinates, '$[2][0]'),
        JSON_EXTRACT(coordinates, '$[3][0]')
    ),
    LEAST(
        JSON_EXTRACT(coordinates, '$[0][1]'),
        JSON_EXTRACT(coordinates, '$[1][1]'),
        JSON_EXTRACT(coordinates, '$[2][1]'),
        JSON_EXTRACT(coordinates, '$[3][1]')
    ),
    GREATEST(
        JSON_EXTRACT(coordinates, '$[0][0]'),
        JSON_EXTRACT(coordinates, '$[1][0]'),
        JSON_EXTRACT(coordinates, '$[2][0]'),
        JSON_EXTRACT(coordinates, '$[3][0]')
    ),
    GREATEST(
        JSON_EXTRACT(coordinates, '$[0][1]'),
        JSON_EXTRACT(coordinates, '$[1][1]'),
        JSON_EXTRACT(coordinates, '$[2][1]'),
        JSON_EXTRACT(coordinates, '$[3][1]')
    )
FROM geo_fences
WHERE type = 'rectangle'
  AND coordinates IS NOT NULL
ON DUPLICATE KEY UPDATE
    geometry_type = VALUES(geometry_type),
    rect_sw_lat   = VALUES(rect_sw_lat),
    rect_sw_lng   = VALUES(rect_sw_lng),
    rect_ne_lat   = VALUES(rect_ne_lat),
    rect_ne_lng   = VALUES(rect_ne_lng);

-- ----------------------------------------------------------------------------
-- 7. Per-fence default policy rows
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO fence_policies (fence_id) SELECT id FROM geo_fences;

-- ----------------------------------------------------------------------------
-- 8. Preserve historical alerts as archive; expose as legacy view
-- ----------------------------------------------------------------------------
-- Move old fence_alerts to archive (only if not already moved).
SET @has_fence_alerts := (
    SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = 'smart_watch' AND table_name = 'fence_alerts'
);
SET @sql_archive := IF(@has_fence_alerts > 0,
    'RENAME TABLE fence_alerts TO fence_alerts_archive',
    'SELECT 1');
PREPARE stmt_archive FROM @sql_archive;
EXECUTE stmt_archive;
DEALLOCATE PREPARE stmt_archive;

-- Recreate fence_alerts as a forward-compatible view over fence_events
-- (legacy fields map from new fields; only EXIT/DWELL become alerts).
DROP VIEW IF EXISTS fence_alerts_v;
CREATE OR REPLACE SQL SECURITY INVOKER VIEW fence_alerts AS
SELECT
    fe.id,
    fe.device_id,
    fe.patient_id,
    fe.fence_id,
    'fence_breach' AS alert_type,
    CASE fe.ack_status
        WHEN 'NEW'             THEN 'pending'
        WHEN 'ACKED'           THEN 'handled'
        WHEN 'RESOLVED'        THEN 'handled'
        WHEN 'FALSE_POSITIVE'  THEN 'false_alarm'
    END AS status,
    fe.latitude,
    fe.longitude,
    NULL AS address,
    fe.occurred_at AS alert_time,
    fe.ack_at AS handled_time,
    fe.ack_user_id AS handled_by,
    fe.ack_remark AS remark,
    fe.occurred_at AS created_at
FROM fence_events fe
WHERE fe.event_type IN ('EXIT','DWELL');

-- ----------------------------------------------------------------------------
-- 9. Backfill existing fence_alerts_archive rows into fence_events
-- ----------------------------------------------------------------------------
-- Only on first run: if archive exists but fence_events has no EXIT rows
-- for archived alerts, copy them over so the new table has the full
-- history.
INSERT IGNORE INTO fence_events
    (fence_id, device_id, patient_id, event_type, latitude, longitude,
     occurred_at, ack_status)
SELECT
    fence_id, device_id, patient_id,
    'EXIT', latitude, longitude, alert_time,
    CASE status
        WHEN 'pending'      THEN 'NEW'
        WHEN 'handled'      THEN 'ACKED'
        WHEN 'false_alarm'  THEN 'FALSE_POSITIVE'
        ELSE 'NEW'
    END
FROM fence_alerts_archive;

-- ============================================================================
-- End of 04_fence_redesign.sql
-- ============================================================================
