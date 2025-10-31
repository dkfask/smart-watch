package com.example.demo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class GeoFence {
    private Long fenceId;
    private Long userId;
    private String name;
    private BigDecimal centerLatitude;
    private BigDecimal centerLongitude;
    private BigDecimal radius; // meters
    private String triggerType; // enter, exit, both
    private Boolean isActive;
    private LocalDateTime createdAt;

    public Long getFenceId() { return fenceId; }
    public void setFenceId(Long fenceId) { this.fenceId = fenceId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getCenterLatitude() { return centerLatitude; }
    public void setCenterLatitude(BigDecimal centerLatitude) { this.centerLatitude = centerLatitude; }
    public BigDecimal getCenterLongitude() { return centerLongitude; }
    public void setCenterLongitude(BigDecimal centerLongitude) { this.centerLongitude = centerLongitude; }
    public BigDecimal getRadius() { return radius; }
    public void setRadius(BigDecimal radius) { this.radius = radius; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

