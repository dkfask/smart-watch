package com.example.demo.model;

import java.time.LocalDateTime;

public class FenceAlert {
    private Long alertId;
    private Long fenceId;
    private Long deviceId;
    private String alertType; // enter, exit
    private LocalDateTime triggeredTime;
    private Boolean isRead;

    public Long getAlertId() { return alertId; }
    public void setAlertId(Long alertId) { this.alertId = alertId; }
    public Long getFenceId() { return fenceId; }
    public void setFenceId(Long fenceId) { this.fenceId = fenceId; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public LocalDateTime getTriggeredTime() { return triggeredTime; }
    public void setTriggeredTime(LocalDateTime triggeredTime) { this.triggeredTime = triggeredTime; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean read) { isRead = read; }
}

