package com.smartwatch.monitor.model;

import com.google.gson.annotations.SerializedName;

/**
 * 设备位置模型类，对应后端设备定位数据
 */
public class DeviceLocation {

    /** 位置记录ID */
    @SerializedName("locationId")
    private Long locationId;

    /** 设备ID */
    @SerializedName("deviceId")
    private Long deviceId;

    /** 定位时间 */
    private String time;

    /** 纬度 */
    private Double latitude;

    /** 经度 */
    private Double longitude;

    /** 精度（米） */
    private Integer accuracy;

    /** 海拔 */
    private Double altitude;

    /** 电池电量 */
    @SerializedName("batteryLevel")
    private Integer batteryLevel;

    /** 定位来源：gps、wifi、cell、bluetooth */
    private String source;

    /** 设备IMEI号 */
    private String imei;

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Integer getAccuracy() { return accuracy; }
    public void setAccuracy(Integer accuracy) { this.accuracy = accuracy; }
    public Double getAltitude() { return altitude; }
    public void setAltitude(Double altitude) { this.altitude = altitude; }
    public Integer getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Integer batteryLevel) { this.batteryLevel = batteryLevel; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }
}
