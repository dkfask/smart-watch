package com.example.demo.model;

import jakarta.persistence.*;
import java.util.Date;

/**
 * WebSocket subscription entry — one row per (user, ws_session, scope).
 * Used by {@code WebSocketFencePusher} to deliver fence events to only
 * those sessions that asked for them (not broadcast).
 *
 * <p>Scopes:
 * <ul>
 *   <li>{@code FENCE}   — events for a specific fence; {@code scopeId} = fence_id</li>
 *   <li>{@code PATIENT} — events for a specific patient; {@code scopeId} = patient_id</li>
 *   <li>{@code DEVICE}  — events for a specific device; {@code scopeId} = device_id</li>
 *   <li>{@code ALL}     — every event; {@code scopeId} is null</li>
 * </ul>
 */
@Entity
@Table(name = "fence_subscriptions",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_sub_user_session",
           columnNames = {"user_id", "ws_session_id", "scope_type", "scope_id"}))
public class FenceSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 10)
    private ScopeType scopeType;

    @Column(name = "scope_id")
    private Long scopeId;

    @Column(name = "ws_session_id", nullable = false, length = 64)
    private String wsSessionId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "datetime default current_timestamp")
    private Date createdAt = new Date();

    public enum ScopeType { FENCE, PATIENT, DEVICE, ALL }

    public FenceSubscription() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public ScopeType getScopeType() { return scopeType; }
    public void setScopeType(ScopeType scopeType) { this.scopeType = scopeType; }
    public Long getScopeId() { return scopeId; }
    public void setScopeId(Long scopeId) { this.scopeId = scopeId; }
    public String getWsSessionId() { return wsSessionId; }
    public void setWsSessionId(String wsSessionId) { this.wsSessionId = wsSessionId; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
