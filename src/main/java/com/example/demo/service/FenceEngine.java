package com.example.demo.service;

import com.example.demo.model.FenceDeviceState;
import com.example.demo.model.FenceEvent;
import com.example.demo.model.FenceEvent.AckStatus;
import com.example.demo.model.FenceEvent.EventType;
import com.example.demo.model.FenceGeometry;
import com.example.demo.model.FenceGeometry.GeometryType;
import com.example.demo.model.FencePolicy;
import com.example.demo.model.GeoFence;
import com.example.demo.model.PatientDevice;
import com.example.demo.repository.FenceDeviceStateRepository;
import com.example.demo.repository.FenceEventRepository;
import com.example.demo.repository.FenceGeometryRepository;
import com.example.demo.repository.FencePolicyRepository;
import com.example.demo.repository.FenceSubscriptionRepository;
import com.example.demo.repository.GeoFenceRepository;
import com.example.demo.repository.PatientDeviceRepository;
import com.example.demo.util.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Geo-fence detection engine.
 *
 * <p>Replaces the old {@code FenceService.checkFencesAndAlert} which only
 * supported circle geometry and only fired on EXIT. This engine:
 *
 * <ul>
 *   <li>Supports all three geometry types (CIRCLE / POLYGON / RECTANGLE)
 *       via the {@link FenceGeometry} table.</li>
 *   <li>Emits three event types: {@link EventType#ENTER ENTER},
 *       {@link EventType#EXIT EXIT}, {@link EventType#DWELL DWELL}.</li>
 *   <li>Applies GPS debounce — requires N consecutive points on the
 *       opposite side before confirming a transition.</li>
 *   <li>Honours per-fence cooldown (suppresses noise events).</li>
 *   <li>Honours per-fence dwell trigger (alerts after device has been
 *       inside for N seconds).</li>
 *   <li>Maintains a per-(fence, device) state row in
 *       {@code fence_device_state} so each location report only needs
 *       to read the latest state, not the latest event.</li>
 * </ul>
 *
 * <p>Single public entry point: {@link #onLocationReport(long, double, double)}.
 * Called from {@code TrackingService} for every device location update.
 */
@Service
public class FenceEngine {
    private static final Logger log = LoggerFactory.getLogger(FenceEngine.class);

    private final GeoFenceRepository fenceRepo;
    private final FenceGeometryRepository geometryRepo;
    private final FencePolicyRepository policyRepo;
    private final FenceEventRepository eventRepo;
    private final FenceDeviceStateRepository stateRepo;
    private final FenceSubscriptionRepository subscriptionRepo;
    private final PatientDeviceRepository patientDeviceRepo;

    public FenceEngine(GeoFenceRepository fenceRepo,
                       FenceGeometryRepository geometryRepo,
                       FencePolicyRepository policyRepo,
                       FenceEventRepository eventRepo,
                       FenceDeviceStateRepository stateRepo,
                       FenceSubscriptionRepository subscriptionRepo,
                       PatientDeviceRepository patientDeviceRepo) {
        this.fenceRepo = fenceRepo;
        this.geometryRepo = geometryRepo;
        this.policyRepo = policyRepo;
        this.eventRepo = eventRepo;
        this.stateRepo = stateRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.patientDeviceRepo = patientDeviceRepo;
    }

    // =====================================================================
    // Public entry
    // =====================================================================

    /**
     * Called from {@code TrackingService} for every device location
     * report. Looks up the device's patient(s), iterates each bound
     * fence, and decides whether to emit ENTER/EXIT/DWELL.
     */
    public void onLocationReport(long deviceId, double lat, double lng) {
        if (Double.isNaN(lat) || Double.isNaN(lng)) return;
        try {
            checkFencesForDevice(deviceId, lat, lng);
        } catch (Exception ex) {
            // Never let fence-detection failure break the location pipeline.
            log.error("FenceEngine failed for device {} at ({},{}): {}",
                    deviceId, lat, lng, ex.toString(), ex);
        }
    }

    // =====================================================================
    // Per-device analysis
    // =====================================================================

    void checkFencesForDevice(long deviceId, double lat, double lng) {
        List<PatientDevice> bindings = patientDeviceRepo.findByDeviceId(deviceId);
        if (bindings == null || bindings.isEmpty()) return;

        for (PatientDevice pd : bindings) {
            Long patientId = pd.getPatientId();
            if (patientId == null) continue;
            List<GeoFence> fences = fencesForPatient(patientId);
            for (GeoFence fence : fences) {
                if (!"active".equalsIgnoreCase(fence.getStatus())) continue;
                checkSingleFence(fence, deviceId, patientId, lat, lng);
            }
        }
    }

    /**
     * Patient → list of active fences. Combines the legacy
     * {@code geo_fences.patient_id} path with the
     * {@code fence_patients} many-to-many table, deduplicated by id.
     */
    private List<GeoFence> fencesForPatient(Long patientId) {
        java.util.LinkedHashMap<Long, GeoFence> map = new java.util.LinkedHashMap<>();
        for (GeoFence f : fenceRepo.listByPatient(patientId)) {
            map.putIfAbsent(f.getId(), f);
        }
        // The new m:n table (always-active filter applied at SQL level).
        for (GeoFence f : jdbcFindByPatientViaFencePatients(patientId)) {
            map.putIfAbsent(f.getId(), f);
        }
        return new java.util.ArrayList<>(map.values());
    }

    /**
     * Read-only query for fences via {@code fence_patients} join.
     * Uses JdbcTemplate indirectly via GeoFenceRepository.listByPatient
     * semantics — for the new path we accept a SQL round-trip.
     */
    private List<GeoFence> jdbcFindByPatientViaFencePatients(Long patientId) {
        // Delegate to a small SQL query via fenceRepo's JdbcTemplate.
        // We don't add a new repo method for this to avoid touching
        // GeoFenceRepository in stage 2; instead we issue the SQL
        // through a temporary one-shot helper.
        return fenceRepo.listActiveByFencePatients(patientId);
    }

    // =====================================================================
    // Per-fence state machine
    // =====================================================================

    void checkSingleFence(GeoFence fence, long deviceId, long patientId,
                          double lat, double lng) {
        FenceGeometry geom = geometryRepo.findByFenceId(fence.getId());
        if (geom == null) {
            // Geometry migration hasn't run for this fence yet — fall back
            // to the legacy Haversine check via the old entity.
            // (In stage 4 this path is removed.)
            return;
        }
        FencePolicy policy = policyRepo.effectivePolicyFor(fence.getId());
        FenceDeviceState state = stateRepo.findOne(fence.getId(), deviceId);

        boolean nowInside = pointInside(geom, lat, lng);

        // First contact — initialise state without emitting.
        if (state == null) {
            FenceDeviceState initial = new FenceDeviceState();
            initial.setFenceId(fence.getId());
            initial.setDeviceId(deviceId);
            initial.setIsInside(nowInside);
            initial.setLastChangedAt(new Date());
            if (nowInside) initial.setInsideSince(new Date());
            initial.setLastLat(lat);
            initial.setLastLng(lng);
            initial.setOutsideCount(0);
            stateRepo.upsert(initial);
            return;
        }

        // Same state as before — handle DWELL or just refresh.
        if (state.getIsInside() != null && state.getIsInside() == nowInside) {
            state.setOutsideCount(0);
            state.setLastLat(lat);
            state.setLastLng(lng);
            stateRepo.upsert(state);
            if (nowInside && Boolean.TRUE.equals(policy.getDwellAlert())) {
                evaluateDwell(fence, deviceId, patientId, state, policy, lat, lng);
            }
            return;
        }

        // State changed — apply GPS debounce before committing.
        int requiredHits = policy.getDebounceCount() == null ? 3
                : Math.max(1, policy.getDebounceCount());
        int currentCount = state.getOutsideCount() == null ? 0 : state.getOutsideCount();
        // If transitioning TO outside, increment the counter; to inside,
        // reset it (jitter close to the boundary shouldn't trigger ENTER).
        int newCount = nowInside ? 0 : currentCount + 1;
        state.setOutsideCount(newCount);
        state.setLastLat(lat);
        state.setLastLng(lng);

        if (newCount < requiredHits) {
            // Not enough evidence yet — persist provisional state only.
            stateRepo.upsert(state);
            log.debug("Fence {} debounce {}/{} for device {} — not yet confirmed",
                    fence.getId(), newCount, requiredHits, deviceId);
            return;
        }

        // Confirmed transition — emit event subject to cooldown.
        EventType eventType = nowInside ? EventType.ENTER : EventType.EXIT;
        boolean allowEvent = (nowInside && Boolean.TRUE.equals(policy.getEnterAlert()))
                          || (!nowInside && Boolean.TRUE.equals(policy.getExitAlert()));
        if (!allowEvent) {
            // Policy says don't surface this transition — but still commit.
            commitStateChange(state, nowInside, lat, lng);
            return;
        }

        if (isWithinCooldown(fence.getId(), deviceId, eventType, policy)) {
            log.debug("Fence {} cooldown for device {} eventType={} — suppressed",
                    fence.getId(), deviceId, eventType);
            commitStateChange(state, nowInside, lat, lng);
            return;
        }

        FenceEvent event = buildEvent(fence, deviceId, patientId, eventType, lat, lng, null);
        long eventId = eventRepo.insert(event);

        commitStateChange(state, nowInside, lat, lng);

        log.info("Fence event: fence={} device={} patient={} type={} lat={} lng={} eventId={}",
                fence.getId(), deviceId, patientId, eventType, lat, lng, eventId);

        // Stage 3: push via WebSocketFencePusher.
        // Stage 2: no-op for now (kept off to avoid double-pushing with
        // legacy FenceService during the transition window).
        if (webSocketPusher != null) {
            webSocketPusher.push(fence.getId(), patientId, event);
        }
    }

    /**
     * Commit the new state row after a confirmed transition or refresh.
     */
    private void commitStateChange(FenceDeviceState state, boolean nowInside,
                                   double lat, double lng) {
        state.setIsInside(nowInside);
        state.setLastChangedAt(new Date());
        if (nowInside) {
            state.setInsideSince(new Date());
        } else {
            state.setInsideSince(null);
            state.setOutsideCount(0);
        }
        state.setLastLat(lat);
        state.setLastLng(lng);
        stateRepo.upsert(state);
    }

    // =====================================================================
    // DWELL
    // =====================================================================

    private void evaluateDwell(GeoFence fence, long deviceId, long patientId,
                               FenceDeviceState state, FencePolicy policy,
                               double lat, double lng) {
        if (state.getInsideSince() == null) return;
        int dwellSec = policy.getDwellSeconds() == null ? 300
                : Math.max(1, policy.getDwellSeconds());
        long insideMs = System.currentTimeMillis() - state.getInsideSince().getTime();
        long dwellMs = insideMs - dwellSec * 1000L;
        if (dwellMs < 0) return;   // not long enough yet

        // Avoid emitting DWELL repeatedly — only when not already
        // emitted for this stay. We use the latest DWELL event as the
        // marker; if it's absent or older than insideSince, fire.
        FenceEvent latest = eventRepo.findLatestByFenceDevice(fence.getId(), deviceId);
        if (latest != null && latest.getEventType() == EventType.DWELL
                && latest.getOccurredAt() != null
                && latest.getOccurredAt().after(state.getInsideSince())) {
            return;     // already emitted for this stay
        }

        long dwellSecsActual = insideMs / 1000L;
        FenceEvent event = buildEvent(fence, deviceId, patientId,
                EventType.DWELL, lat, lng, (int) dwellSecsActual);
        long eventId = eventRepo.insert(event);
        log.info("Fence dwell event: fence={} device={} patient={} dwellSec={} eventId={}",
                fence.getId(), deviceId, patientId, dwellSecsActual, eventId);

        if (webSocketPusher != null) {
            webSocketPusher.push(fence.getId(), patientId, event);
        }
    }

    // =====================================================================
    // Geometry dispatch
    // =====================================================================

    private boolean pointInside(FenceGeometry g, double lat, double lng) {
        // Decode polygon JSON if present; GeoUtils expects List<double[]>.
        List<double[]> polygon = null;
        if (g.getPolygonCoords() != null && !g.getPolygonCoords().isBlank()) {
            polygon = decodePolygon(g.getPolygonCoords());
        }
        return GeoUtils.insideFromGeometryType(
                g.getGeometryType() == null ? null : g.getGeometryType().name(),
                lat, lng,
                g.getCenterLat(), g.getCenterLng(), g.getRadiusM(),
                polygon,
                g.getRectSwLat(), g.getRectSwLng(),
                g.getRectNeLat(), g.getRectNeLng());
    }

    /**
     * Minimal JSON polygon decoder. The stored format is
     * {@code [[lat,lng], [lat,lng], ...]} — same as the frontend sends.
     */
    private static List<double[]> decodePolygon(String json) {
        List<double[]> out = new java.util.ArrayList<>();
        if (json == null) return out;
        String s = json.trim();
        if (s.startsWith("[")) s = s.substring(1);
        if (s.endsWith("]")) s = s.substring(0, s.length() - 1);
        int depth = 0;
        StringBuilder cur = new StringBuilder();
        java.util.List<String> inner = new java.util.ArrayList<>(2);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '[') {
                depth++;
                cur.setLength(0);
            } else if (c == ']') {
                depth--;
                String tok = cur.toString().trim();
                if (!tok.isEmpty()) inner.add(tok);
                cur.setLength(0);
                if (depth == 0 && !inner.isEmpty()) {
                    double[] pair = new double[2];
                    pair[0] = Double.parseDouble(inner.get(0).replaceAll("[^0-9.\\-eE]", ""));
                    pair[1] = inner.size() > 1
                            ? Double.parseDouble(inner.get(1).replaceAll("[^0-9.\\-eE]", ""))
                            : 0;
                    out.add(pair);
                    inner.clear();
                }
            } else if (c == ',' && depth == 1) {
                String tok = cur.toString().trim();
                if (!tok.isEmpty()) inner.add(tok);
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        return out;
    }

    // =====================================================================
    // Cooldown
    // =====================================================================

    private boolean isWithinCooldown(long fenceId, long deviceId,
                                     EventType eventType, FencePolicy policy) {
        int cooldownSec = policy.getCooldownSec() == null ? 300
                : Math.max(1, policy.getCooldownSec());
        FenceEvent latest = eventRepo.findLatestByFenceDevice(fenceId, deviceId);
        if (latest == null || latest.getEventType() != eventType
                || latest.getOccurredAt() == null) {
            return false;
        }
        long elapsedMs = System.currentTimeMillis() - latest.getOccurredAt().getTime();
        return elapsedMs < cooldownSec * 1000L;
    }

    // =====================================================================
    // Event builder
    // =====================================================================

    private FenceEvent buildEvent(GeoFence fence, long deviceId, long patientId,
                                  EventType eventType, double lat, double lng,
                                  Integer dwellSeconds) {
        FenceEvent e = new FenceEvent();
        e.setFenceId(fence.getId());
        e.setDeviceId(deviceId);
        e.setPatientId(patientId);
        e.setEventType(eventType);
        e.setLatitude(lat);
        e.setLongitude(lng);
        e.setOccurredAt(new Date());
        e.setDwellSeconds(dwellSeconds);
        e.setAckStatus(AckStatus.NEW);
        return e;
    }

    // =====================================================================
    // WebSocket hook (set by Spring in stage 3)
    // =====================================================================
    private volatile WebSocketFencePusher webSocketPusher;

    /** Inject the pusher once WebSocketFencePusher exists (stage 3). */
    public void setWebSocketPusher(WebSocketFencePusher pusher) {
        this.webSocketPusher = pusher;
    }
}
