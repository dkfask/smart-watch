package com.smartwatch.monitor.model;

import com.google.gson.annotations.SerializedName;

/**
 * 设备信息DTO，包含设备基本信息、在线状态和关联病人信息
 */
public class DeviceInfoDto {

    /** 设备ID */
    private Long id;

    /** 设备IMEI号 */
    private String imei;

    /** 移动国家代码 */
    private String mcc;

    /** 移动网络代码 */
    private String mnc;

    /** 接入点名称 */
    private String apn;

    /** SIM卡ICCID */
    private String iccid;

    /** 国际移动用户识别码 */
    private String imsi;

    /** 是否在线 */
    @SerializedName("isOnline")
    private Boolean isOnline;

    /** 最后定位时间 */
    @SerializedName("lastLocationTime")
    private String lastLocationTime;

    /** 最后纬度 */
    @SerializedName("lastLatitude")
    private Double lastLatitude;

    /** 最后经度 */
    @SerializedName("lastLongitude")
    private Double lastLongitude;

    /** 电池电量 */
    @SerializedName("batteryLevel")
    private Integer batteryLevel;

    /** 创建时间 */
    @SerializedName("createdAt")
    private String createdAt;

    /** 更新时间 */
    @SerializedName("updatedAt")
    private String updatedAt;

    /** 关联病人信息 */
    private PatientInfo patient;

    /** 关联的设备实体（用于Room缓存） */
    private Device device;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }
    public String getMcc() { return mcc; }
    public void setMcc(String mcc) { this.mcc = mcc; }
    public String getMnc() { return mnc; }
    public void setMnc(String mnc) { this.mnc = mnc; }
    public String getApn() { return apn; }
    public void setApn(String apn) { this.apn = apn; }
    public String getIccid() { return iccid; }
    public void setIccid(String iccid) { this.iccid = iccid; }
    public String getImsi() { return imsi; }
    public void setImsi(String imsi) { this.imsi = imsi; }
    public Boolean getIsOnline() { return isOnline; }
    public void setIsOnline(Boolean isOnline) { this.isOnline = isOnline; }
    public String getLastLocationTime() { return lastLocationTime; }
    public void setLastLocationTime(String lastLocationTime) { this.lastLocationTime = lastLocationTime; }
    public Double getLastLatitude() { return lastLatitude; }
    public void setLastLatitude(Double lastLatitude) { this.lastLatitude = lastLatitude; }
    public Double getLastLongitude() { return lastLongitude; }
    public void setLastLongitude(Double lastLongitude) { this.lastLongitude = lastLongitude; }
    public Integer getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Integer batteryLevel) { this.batteryLevel = batteryLevel; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public PatientInfo getPatient() { return patient; }
    public void setPatient(PatientInfo patient) { this.patient = patient; }
    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }

    /**
     * 病人简要信息（嵌套在DeviceInfoDto中）
     */
    public static class PatientInfo {
        private Long id;
        private String name;
        private String gender;
        private Integer age;
        private String ward;
        private String bed;
        private String phone;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public String getWard() { return ward; }
        public void setWard(String ward) { this.ward = ward; }
        public String getBed() { return bed; }
        public void setBed(String bed) { this.bed = bed; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }
}
