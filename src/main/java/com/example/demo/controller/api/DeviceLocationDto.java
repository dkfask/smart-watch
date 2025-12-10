package com.example.demo.controller.api;

import java.time.LocalDateTime;

public class DeviceLocationDto {
    private Long deviceId;
    private String imei;
    private Double latitude;
    private Double longitude;
    private String address;
    private LocalDateTime time;
    private String source;
    private Integer batteryLevel;
    private Integer accuracy;

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }

    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public LocalDateTime getTime() { return time; }
    public void setTime(LocalDateTime time) { this.time = time; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Integer getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Integer batteryLevel) { this.batteryLevel = batteryLevel; }

    public Integer getAccuracy() { return accuracy; }
    public void setAccuracy(Integer accuracy) { this.accuracy = accuracy; }
}

