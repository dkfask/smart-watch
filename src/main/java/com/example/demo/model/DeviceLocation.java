package com.example.demo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DeviceLocation {
    private Long locationId;
    private Long deviceId;
    private LocalDateTime time;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer accuracy;
    private BigDecimal altitude;
    private Integer batteryLevel;
    private String source; // gps, wifi, cell, bluetooth

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public LocalDateTime getTime() { return time; }
    public void setTime(LocalDateTime time) { this.time = time; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public Integer getAccuracy() { return accuracy; }
    public void setAccuracy(Integer accuracy) { this.accuracy = accuracy; }
    public BigDecimal getAltitude() { return altitude; }
    public void setAltitude(BigDecimal altitude) { this.altitude = altitude; }
    public Integer getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Integer batteryLevel) { this.batteryLevel = batteryLevel; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}

