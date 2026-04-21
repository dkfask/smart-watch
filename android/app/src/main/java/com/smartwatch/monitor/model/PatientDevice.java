package com.smartwatch.monitor.model;

import com.google.gson.annotations.SerializedName;

/**
 * 病人-设备关联模型类，对应后端patient_devices表
 */
public class PatientDevice {

    /** 关联ID */
    private Long id;

    /** 病人ID */
    @SerializedName("patient_id")
    private Long patientId;

    /** 设备ID */
    @SerializedName("device_id")
    private Long deviceId;

    /** 绑定时间 */
    @SerializedName("bind_time")
    private String bindTime;

    /** 关系：wearing-佩戴，monitoring-监测 */
    private String relationship;

    /** 是否激活 */
    @SerializedName("isActive")
    private Boolean isActive;

    /** 创建时间 */
    @SerializedName("created_at")
    private String createdAt;

    /** 更新时间 */
    @SerializedName("updated_at")
    private String updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public String getBindTime() { return bindTime; }
    public void setBindTime(String bindTime) { this.bindTime = bindTime; }
    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
