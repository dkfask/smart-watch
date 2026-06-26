package com.example.demo.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Cached latest state of a single (fence, device) pair. Updated by
 * {@code FenceEngine} after every transition. Used to:
 *
 * <ul>
 *   <li>Avoid re-querying the latest event each location report.</li>
 *   <li>Drive the live-status dashboard (who is inside which fence).</li>
 *   <li>Provide the {@code outside_count} running tally for GPS
 *       debounce so we don't false-fire on jitter.</li>
 * </ul>
 *
 * Composite primary key — no surrogate {@code id} column.
 */
@Entity
@Table(name = "fence_device_state")
@IdClass(FenceDeviceState.PK.class)
public class FenceDeviceState {

    @Id
    @Column(name = "fence_id", nullable = false)
    private Long fenceId;

    @Id
    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "is_inside", nullable = false,
            columnDefinition = "tinyint(1) default 0")
    private Boolean isInside = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_changed_at", nullable = false,
            columnDefinition = "datetime(3) default current_timestamp(3)")
    private Date lastChangedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "inside_since")
    private Date insideSince;

    @Column(name = "last_lat")   private Double lastLat;
    @Column(name = "last_lng")   private Double lastLng;

    @Column(name = "outside_count", nullable = false,
            columnDefinition = "int default 0")
    private Integer outsideCount = 0;

    public FenceDeviceState() {}

    public Long getFenceId() { return fenceId; }
    public void setFenceId(Long fenceId) { this.fenceId = fenceId; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public Boolean getIsInside() { return isInside; }
    public void setIsInside(Boolean isInside) { this.isInside = isInside; }
    public Date getLastChangedAt() { return lastChangedAt; }
    public void setLastChangedAt(Date d) { this.lastChangedAt = d; }
    public Date getInsideSince() { return insideSince; }
    public void setInsideSince(Date d) { this.insideSince = d; }
    public Double getLastLat() { return lastLat; }
    public void setLastLat(Double lastLat) { this.lastLat = lastLat; }
    public Double getLastLng() { return lastLng; }
    public void setLastLng(Double lastLng) { this.lastLng = lastLng; }
    public Integer getOutsideCount() { return outsideCount; }
    public void setOutsideCount(Integer outsideCount) { this.outsideCount = outsideCount; }

    /** Composite primary key. */
    public static class PK implements Serializable {
        private Long fenceId;
        private Long deviceId;

        public PK() {}
        public PK(Long fenceId, Long deviceId) {
            this.fenceId = fenceId;
            this.deviceId = deviceId;
        }
        public Long getFenceId() { return fenceId; }
        public void setFenceId(Long v) { this.fenceId = v; }
        public Long getDeviceId() { return deviceId; }
        public void setDeviceId(Long v) { this.deviceId = v; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(fenceId, pk.fenceId)
                && Objects.equals(deviceId, pk.deviceId);
        }
        @Override public int hashCode() { return Objects.hash(fenceId, deviceId); }
    }
}
