package com.example.demo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DeviceStatus {
    private Long deviceId;
    private LocalDateTime lastLocationTime;
    private BigDecimal lastLatitude;
    private BigDecimal lastLongitude;
    private Integer batteryLevel;
    private Boolean isOnline;
    private LocalDateTime updatedAt;

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public LocalDateTime getLastLocationTime() { return lastLocationTime; }
    public void setLastLocationTime(LocalDateTime lastLocationTime) { this.lastLocationTime = lastLocationTime; }
    public BigDecimal getLastLatitude() { return lastLatitude; }
    public void setLastLatitude(BigDecimal lastLatitude) { this.lastLatitude = lastLatitude; }
    public BigDecimal getLastLongitude() { return lastLongitude; }
    public void setLastLongitude(BigDecimal lastLongitude) { this.lastLongitude = lastLongitude; }
    public Integer getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Integer batteryLevel) { this.batteryLevel = batteryLevel; }
    public Boolean getIsOnline() { return isOnline; }
    public void setIsOnline(Boolean online) { isOnline = online; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

