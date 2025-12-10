package com.example.demo.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "device_status")
public class DeviceStatus {
    @Id
    @Column(name = "device_id")
    private Long deviceId;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_location_time")
    private Date lastLocationTime;
    
    @Column(name = "last_latitude")
    private Double lastLatitude;
    
    @Column(name = "last_longitude")
    private Double lastLongitude;
    
    @Column(name = "battery_level")
    private Integer batteryLevel;
    
    @Column(name = "is_online", nullable = false, columnDefinition = "boolean default false")
    private Boolean isOnline;
    
    @Column(name = "gsm_signal")
    private Integer gsmSignal;
    
    @Column(name = "satellite_count")
    private Integer satelliteCount;
    
    @Column(name = "arm_status", length = 2)
    private String armStatus;
    
    @Column(name = "work_mode", length = 2)
    private String workMode;
    
    @Column(length = 15)
    private String imei;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime default current_timestamp on update current_timestamp")
    private Date updatedAt;
    
    // Default constructor
    public DeviceStatus() {
        this.isOnline = false;
        this.updatedAt = new Date();
    }

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    
    public Date getLastLocationTime() { return lastLocationTime; }
    public void setLastLocationTime(Date lastLocationTime) { this.lastLocationTime = lastLocationTime; }
    
    public Double getLastLatitude() { return lastLatitude; }
    public void setLastLatitude(Double lastLatitude) { this.lastLatitude = lastLatitude; }
    
    public Double getLastLongitude() { return lastLongitude; }
    public void setLastLongitude(Double lastLongitude) { this.lastLongitude = lastLongitude; }
    
    public Integer getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Integer batteryLevel) { this.batteryLevel = batteryLevel; }
    
    public Boolean getIsOnline() { return isOnline; }
    public void setIsOnline(Boolean isOnline) { this.isOnline = isOnline; }
    
    public Integer getGsmSignal() { return gsmSignal; }
    public void setGsmSignal(Integer gsmSignal) { this.gsmSignal = gsmSignal; }
    
    public Integer getSatelliteCount() { return satelliteCount; }
    public void setSatelliteCount(Integer satelliteCount) { this.satelliteCount = satelliteCount; }
    
    public String getArmStatus() { return armStatus; }
    public void setArmStatus(String armStatus) { this.armStatus = armStatus; }
    
    public String getWorkMode() { return workMode; }
    public void setWorkMode(String workMode) { this.workMode = workMode; }
    
    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }
    
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
