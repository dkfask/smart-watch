package com.example.demo.model;

import jakarta.persistence.*;
import java.util.Date;

/**
 * A single state-transition event for a device within a fence.
 *
 * <p>Replaces the dual-write to {@code fence_alerts} / {@code alarms} /
 * {@code alerts} tables. Every transition (enter, exit, dwell) is one row
 * here. The legacy {@code fence_alerts} table is exposed as a view over
 * this one for backward compatibility.
 *
 * <p>Ack lifecycle:
 * <pre>NEW → ACKED → RESOLVED   (operator dealt with the alert)
 *      ↘ FALSE_POSITIVE       (operator marked as noise)</pre>
 */
@Entity
@Table(name = "fence_events")
public class FenceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fence_id", nullable = false)
    private GeoFence fence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 10)
    private EventType eventType;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "occurred_at", nullable = false,
            columnDefinition = "datetime(3) default current_timestamp(3)")
    private Date occurredAt;

    /** For DWELL events: how long the device has been inside, in seconds. */
    @Column(name = "dwell_seconds")
    private Integer dwellSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "ack_status", nullable = false, length = 20,
            columnDefinition = "enum('NEW','ACKED','RESOLVED','FALSE_POSITIVE') default 'NEW'")
    private AckStatus ackStatus = AckStatus.NEW;

    @Column(name = "ack_user_id")
    private Long ackUserId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ack_at")
    private Date ackAt;

    @Column(name = "ack_remark", length = 500)
    private String ackRemark;

    public enum EventType { ENTER, EXIT, DWELL }
    public enum AckStatus  { NEW, ACKED, RESOLVED, FALSE_POSITIVE }

    public FenceEvent() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public GeoFence getFence() { return fence; }
    public void setFence(GeoFence fence) { this.fence = fence; }
    /** Helper for callers that only have the FK id (most JDBC code). */
    public Long getFenceId() { return fence == null ? null : fence.getId(); }
    public void setFenceId(Long fenceId) {
        if (fenceId == null) { this.fence = null; return; }
        if (this.fence == null) this.fence = new GeoFence();
        this.fence.setId(fenceId);
    }
    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }
    public Long getDeviceId() { return device == null ? null : device.getId(); }
    public void setDeviceId(Long deviceId) {
        if (deviceId == null) { this.device = null; return; }
        if (this.device == null) this.device = new Device();
        this.device.setId(deviceId);
    }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public Long getPatientId() { return patient == null ? null : patient.getId(); }
    public void setPatientId(Long patientId) {
        if (patientId == null) { this.patient = null; return; }
        if (this.patient == null) this.patient = new Patient();
        this.patient.setId(patientId);
    }
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Date getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Date occurredAt) { this.occurredAt = occurredAt; }
    public Integer getDwellSeconds() { return dwellSeconds; }
    public void setDwellSeconds(Integer dwellSeconds) { this.dwellSeconds = dwellSeconds; }
    public AckStatus getAckStatus() { return ackStatus; }
    public void setAckStatus(AckStatus ackStatus) { this.ackStatus = ackStatus; }
    public Long getAckUserId() { return ackUserId; }
    public void setAckUserId(Long ackUserId) { this.ackUserId = ackUserId; }
    public Date getAckAt() { return ackAt; }
    public void setAckAt(Date ackAt) { this.ackAt = ackAt; }
    public String getAckRemark() { return ackRemark; }
    public void setAckRemark(String ackRemark) { this.ackRemark = ackRemark; }
}
