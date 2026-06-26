package com.example.demo.model;

import jakarta.persistence.*;

/**
 * Per-fence business-rule overrides.
 *
 * <p>Special row {@code fence_id = 0} holds the deployment-wide default
 * (seeded in {@code 04_fence_redesign.sql}). {@code FenceEngine} reads
 * the per-fence row first and falls back to fence_id=0.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code enterAlert}    — emit ENTER events to subscribers</li>
 *   <li>{@code exitAlert}     — emit EXIT events (breach alerts)</li>
 *   <li>{@code dwellAlert}    — emit DWELL events after {@code dwellSeconds}</li>
 *   <li>{@code debounceCount} — require this many consecutive
 *       inside/outside reports before committing a state transition
 *       (mitigates GPS jitter)</li>
 *   <li>{@code cooldownSec}   — minimum gap between successive events
 *       of the same type for the same (fence, device)</li>
 * </ul>
 */
@Entity
@Table(name = "fence_policies")
public class FencePolicy {

    @Id
    @Column(name = "fence_id")
    private Long fenceId;

    @Column(name = "enter_alert", nullable = false,
            columnDefinition = "tinyint(1) default 1")
    private Boolean enterAlert = true;

    @Column(name = "exit_alert", nullable = false,
            columnDefinition = "tinyint(1) default 1")
    private Boolean exitAlert = true;

    @Column(name = "dwell_alert", nullable = false,
            columnDefinition = "tinyint(1) default 0")
    private Boolean dwellAlert = false;

    @Column(name = "dwell_seconds", nullable = false,
            columnDefinition = "int default 300")
    private Integer dwellSeconds = 300;

    @Column(name = "debounce_count", nullable = false,
            columnDefinition = "int default 3")
    private Integer debounceCount = 3;

    @Column(name = "cooldown_sec", nullable = false,
            columnDefinition = "int default 300")
    private Integer cooldownSec = 300;

    public FencePolicy() {}

    public Long getFenceId() { return fenceId; }
    public void setFenceId(Long fenceId) { this.fenceId = fenceId; }
    public Boolean getEnterAlert() { return enterAlert; }
    public void setEnterAlert(Boolean v) { this.enterAlert = v; }
    public Boolean getExitAlert() { return exitAlert; }
    public void setExitAlert(Boolean v) { this.exitAlert = v; }
    public Boolean getDwellAlert() { return dwellAlert; }
    public void setDwellAlert(Boolean v) { this.dwellAlert = v; }
    public Integer getDwellSeconds() { return dwellSeconds; }
    public void setDwellSeconds(Integer v) { this.dwellSeconds = v; }
    public Integer getDebounceCount() { return debounceCount; }
    public void setDebounceCount(Integer v) { this.debounceCount = v; }
    public Integer getCooldownSec() { return cooldownSec; }
    public void setCooldownSec(Integer v) { this.cooldownSec = v; }

    /** Sentinel fence id for the global default row. */
    public static final long GLOBAL_DEFAULT_FENCE_ID = 0L;
}
