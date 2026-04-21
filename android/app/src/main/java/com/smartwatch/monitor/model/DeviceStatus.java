package com.smartwatch.monitor.model;

import com.google.gson.annotations.SerializedName;

/**
 * 设备状态模型类，对应后端device_status表
 */
public class DeviceStatus {

    /** 设备ID */
    @SerializedName("device_id")
    private Long deviceId;

    /** 最后定位时间 */
    @SerializedName("last_location_time")
    private String lastLocationTime;

    /** 最后纬度 */
    @SerializedName("last_latitude")
    private Double lastLatitude;

    /** 最后经度 */
    @SerializedName("last_longitude")
    private Double lastLongitude;

    /** 电池电量 */
    @SerializedName("battery_level")
    private Integer batteryLevel;

    /** 是否在线 */
    @SerializedName("is_online")
    private Boolean isOnline;

    /** GSM信号强度 */
    @SerializedName("gsm_signal")
    private Integer gsmSignal;

    /** 卫星数量 */
    @SerializedName("satellite_count")
    private Integer satelliteCount;

    /** 布防状态 */
    @SerializedName("arm_status")
    private String armStatus;

    /** 工作模式 */
    @SerializedName("work_mode")
    private String workMode;

    /** 设备IMEI号 */
    private String imei;

    /** 更新时间 */
    @SerializedName("updated_at")
    private String updatedAt;

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public String getLastLocationTime() { return lastLocationTime; }
    public void setLastLocationTime(String lastLocationTime) { this.lastLocationTime = lastLocationTime; }
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
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
