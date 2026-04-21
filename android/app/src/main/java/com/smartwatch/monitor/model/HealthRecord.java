package com.smartwatch.monitor.model;

import com.google.gson.annotations.SerializedName;

/**
 * 健康记录模型类，对应后端health_records表
 */
public class HealthRecord {

    /** 记录ID */
    private Long id;

    /** 病人ID */
    @SerializedName("patient_id")
    private Long patientId;

    /** 设备ID */
    @SerializedName("device_id")
    private Long deviceId;

    /** 设备IMEI号 */
    private String imei;

    /** 接收时间 */
    @SerializedName("recv_time")
    private String recvTime;

    /** 数据类型：temperature、heart_rate、blood_pressure、spo2、blood_oxygen */
    @SerializedName("data_type")
    private String dataType;

    /** 数据值 */
    private String value;

    /** 原始数据 */
    @SerializedName("raw_data")
    private String rawData;

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
    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }
    public String getRecvTime() { return recvTime; }
    public void setRecvTime(String recvTime) { this.recvTime = recvTime; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getRawData() { return rawData; }
    public void setRawData(String rawData) { this.rawData = rawData; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
