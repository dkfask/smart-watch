package com.example.demo.model.dto;

import com.example.demo.model.Device;
import com.example.demo.model.DeviceStatus;
import com.example.demo.model.Patient;

import java.util.Date;

/**
 * 设备信息DTO，替代DeviceController中手动构建的Map<String, Object>
 */
public class DeviceInfoDto {
    private Long id;
    private String imei;
    private String mcc;
    private String mnc;
    private String apn;
    private String iccid;
    private String imsi;
    private Boolean isOnline;
    private Date lastLocationTime;
    private Double lastLatitude;
    private Double lastLongitude;
    private Integer batteryLevel;
    private Date createdAt;
    private Date updatedAt;
    private PatientInfo patient;

    /**
     * 从Device实体构建基础DeviceInfoDto（不含状态和病人信息）
     * @param device 设备实体
     * @return DeviceInfoDto
     */
    public static DeviceInfoDto fromDevice(Device device) {
        DeviceInfoDto dto = new DeviceInfoDto();
        dto.setId(device.getId());
        dto.setImei(device.getImei());
        dto.setMcc(device.getMcc());
        dto.setMnc(device.getMnc());
        dto.setApn(device.getApn());
        dto.setIccid(device.getIccid());
        dto.setImsi(device.getImsi());
        dto.setCreatedAt(device.getCreatedAt());
        dto.setUpdatedAt(device.getUpdatedAt());
        return dto;
    }

    /**
     * 填充设备在线状态信息
     * @param status 设备状态实体
     * @param realtimeOnline 实时在线状态
     */
    public void fillStatus(DeviceStatus status, boolean realtimeOnline) {
        this.isOnline = realtimeOnline;
        if (status != null) {
            this.lastLocationTime = status.getLastLocationTime();
            this.lastLatitude = status.getLastLatitude();
            this.lastLongitude = status.getLastLongitude();
            this.batteryLevel = status.getBatteryLevel();
        }
    }

    /**
     * 填充关联病人信息
     * @param patient 病人实体
     */
    public void fillPatient(Patient patient) {
        if (patient != null) {
            this.patient = PatientInfo.fromPatient(patient);
        }
    }

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
    public Date getLastLocationTime() { return lastLocationTime; }
    public void setLastLocationTime(Date lastLocationTime) { this.lastLocationTime = lastLocationTime; }
    public Double getLastLatitude() { return lastLatitude; }
    public void setLastLatitude(Double lastLatitude) { this.lastLatitude = lastLatitude; }
    public Double getLastLongitude() { return lastLongitude; }
    public void setLastLongitude(Double lastLongitude) { this.lastLongitude = lastLongitude; }
    public Integer getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Integer batteryLevel) { this.batteryLevel = batteryLevel; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public PatientInfo getPatient() { return patient; }
    public void setPatient(PatientInfo patient) { this.patient = patient; }

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

        /**
         * 从Patient实体构建PatientInfo
         * @param patient 病人实体
         * @return PatientInfo
         */
        public static PatientInfo fromPatient(Patient patient) {
            PatientInfo info = new PatientInfo();
            info.setId(patient.getId());
            info.setName(patient.getName());
            info.setGender(patient.getGender());
            info.setAge(patient.getAge());
            info.setWard(patient.getWard());
            info.setBed(patient.getBed());
            info.setPhone(patient.getPhone());
            return info;
        }

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
