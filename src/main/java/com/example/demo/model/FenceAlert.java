package com.example.demo.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "fence_alerts")
public class FenceAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "fence_id", nullable = false)
    private Long fenceId;
    
    @Column(name = "device_id", nullable = false)
    private Long deviceId;
    
    @Column(name = "patient_id")
    private Long patientId;
    
    @Column(name = "alert_type", nullable = false, length = 20)
    private String alertType; // enter, exit
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "triggered_time", nullable = false, columnDefinition = "datetime default current_timestamp")
    private Date triggeredTime;
    
    @Column(name = "is_read", nullable = false, columnDefinition = "boolean default false")
    private Boolean isRead;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime default current_timestamp")
    private Date createdAt;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime default current_timestamp on update current_timestamp")
    private Date updatedAt;
    
    // Default constructor
    public FenceAlert() {
        this.isRead = false;
        this.triggeredTime = new Date();
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFenceId() { return fenceId; }
    public void setFenceId(Long fenceId) { this.fenceId = fenceId; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public Date getTriggeredTime() { return triggeredTime; }
    public void setTriggeredTime(Date triggeredTime) { this.triggeredTime = triggeredTime; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean read) { isRead = read; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

