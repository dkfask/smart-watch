package com.example.demo.service;

import com.example.demo.model.FenceEvent;
import com.example.demo.repository.FenceSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pushes fence events to WebSocket subscribers in a targeted (non-broadcast)
 * fashion. In stage 2 this is wired into {@code FenceEngine} but the
 * push is a no-op until stage 3 plugs in the actual
 * {@code WebSocketHandler}.
 *
 * <p>Subscription model — the table {@code fence_subscriptions} stores
 * rows keyed on {@code (user_id, ws_session_id, scope_type, scope_id)}.
 * A push for a {@code (fenceId, patientId)} event reaches every session
 * that subscribed to:
 *
 * <ul>
 *   <li>the specific fence ({@code FENCE} scope)</li>
 *   <li>the patient's events ({@code PATIENT} scope)</li>
 *   <li>everything ({@code ALL} scope)</li>
 * </ul>
 *
 * <p>Stage 3 wires {@link #setWebSocketHandler} so that subscribers
 * actually receive the payload.
 */
@Component
public class WebSocketFencePusher {
    private static final Logger log = LoggerFactory.getLogger(WebSocketFencePusher.class);

    private final FenceSubscriptionRepository subscriptionRepo;
    private volatile WebSocketSink sink;

    public WebSocketFencePusher(FenceSubscriptionRepository subscriptionRepo) {
        this.subscriptionRepo = subscriptionRepo;
    }

    /**
     * Look up subscriber sessions for this event and (when wired) push
     * the payload. Silently logs and drops on failure — never propagates.
     */
    public void push(long fenceId, long patientId, FenceEvent event) {
        try {
            List<String> sessions = subscriptionRepo
                    .findWsSessionsForFenceEvent(fenceId, patientId);
            if (sessions.isEmpty()) return;
            if (sink == null) {
                log.debug("Pusher has no sink wired (stage 3); "
                        + "would deliver {} event to {} sessions",
                        event.getEventType(), sessions.size());
                return;
            }
            sink.sendToSessions(sessions, "fence_event", event);
        } catch (Exception ex) {
            log.error("WebSocket fence_event push failed: {}", ex.toString());
        }
    }

    /**
     * Set by stage 3 once {@code WebSocketHandler} is refactored to
     * accept per-session targeted sends. Until then the pusher is a no-op.
     */
    public void setWebSocketHandler(WebSocketSink sink) {
        this.sink = sink;
    }

    /** Minimal interface — implemented by WebSocketHandler in stage 3. */
    public interface WebSocketSink {
        void sendToSessions(List<String> sessionIds, String action, Object payload);
    }
}
