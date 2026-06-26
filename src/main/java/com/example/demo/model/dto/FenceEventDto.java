package com.example.demo.model.dto;

import com.example.demo.model.FenceEvent;

import java.util.Date;

/**
 * API payload for {@link FenceEvent}. Used by:
 *
 * <ul>
 *   <li>{@code GET /api/fences/{id}/events} — historical events</li>
 *   <li>{@code GET /api/fences/active-events} — un-ack'd events</li>
 *   <li>WebSocket push payload (type = {@code fence_event})</li>
 * </ul>
 */
public class FenceEventDto {
    private Long id;
    private Long fenceId;
    private String fenceName;
    private Long deviceId;
    private Long patientId;
    private String eventType;          // ENTER / EXIT / DWELL
    private Double latitude;
    private Double longitude;
    private Date occurredAt;
    private Integer dwellSeconds;
    private String ackStatus;          // NEW / ACKED / RESOLVED / FALSE_POSITIVE
    private Long ackUserId;
    private Date ackAt;
    private String ackRemark;

    public FenceEventDto() {}

    public static FenceEventDto fromEntity(FenceEvent e) {
        if (e == null) return null;
        FenceEventDto d = new FenceEventDto();
        d.id = e.getId();
        d.fenceId = e.getFence() != null ? e.getFence().getId() : null;
        d.fenceName = e.getFence() != null ? e.getFence().getName() : null;
        d.deviceId = e.getDevice() != null ? e.getDevice().getId() : null;
        d.patientId = e.getPatient() != null ? e.getPatient().getId() : null;
        d.eventType = e.getEventType() == null ? null : e.getEventType().name();
        d.latitude = e.getLatitude();
        d.longitude = e.getLongitude();
        d.occurredAt = e.getOccurredAt();
        d.dwellSeconds = e.getDwellSeconds();
        d.ackStatus = e.getAckStatus() == null ? null : e.getAckStatus().name();
        d.ackUserId = e.getAckUserId();
        d.ackAt = e.getAckAt();
        d.ackRemark = e.getAckRemark();
        return d;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFenceId() { return fenceId; }
    public void setFenceId(Long fenceId) { this.fenceId = fenceId; }
    public String getFenceName() { return fenceName; }
    public void setFenceName(String fenceName) { this.fenceName = fenceName; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Date getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Date occurredAt) { this.occurredAt = occurredAt; }
    public Integer getDwellSeconds() { return dwellSeconds; }
    public void setDwellSeconds(Integer dwellSeconds) { this.dwellSeconds = dwellSeconds; }
    public String getAckStatus() { return ackStatus; }
    public void setAckStatus(String ackStatus) { this.ackStatus = ackStatus; }
    public Long getAckUserId() { return ackUserId; }
    public void setAckUserId(Long ackUserId) { this.ackUserId = ackUserId; }
    public Date getAckAt() { return ackAt; }
    public void setAckAt(Date ackAt) { this.ackAt = ackAt; }
    public String getAckRemark() { return ackRemark; }
    public void setAckRemark(String ackRemark) { this.ackRemark = ackRemark; }
}
