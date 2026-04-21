package com.smartwatch.monitor.model;

import com.google.gson.annotations.SerializedName;

/**
 * 报警模型类（兼容旧接口），对应后端alarms表
 */
public class Alarm {

    /** 报警ID */
    private Long id;

    /** 设备ID */
    @SerializedName("device_id")
    private Long deviceId;

    /** 病人ID */
    @SerializedName("patient_id")
    private Long patientId;

    /** 报警类型 */
    @SerializedName("alarm_type")
    private String alarmType;

    /** 报警级别：critical、warning、info */
    @SerializedName("alarm_level")
    private String alarmLevel;

    /** 报警详细数据JSON */
    @SerializedName("alarm_data")
    private String alarmData;

    /** 纬度 */
    private Double latitude;

    /** 经度 */
    private Double longitude;

    /** 地址 */
    private String address;

    /** 触发时间 */
    @SerializedName("triggered_time")
    private String triggeredTime;

    /** 状态：pending、handled、false_alarm */
    private String status;

    /** 处理结果 */
    @SerializedName("handle_result")
    private String handleResult;

    /** 处理备注 */
    @SerializedName("handle_remark")
    private String handleRemark;

    /** 处理时间 */
    @SerializedName("handled_time")
    private String handledTime;

    /** 是否已读 */
    @SerializedName("is_read")
    private Boolean isRead;

    /** 创建时间 */
    @SerializedName("created_at")
    private String createdAt;

    /** 更新时间 */
    @SerializedName("updated_at")
    private String updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getAlarmType() { return alarmType; }
    public void setAlarmType(String alarmType) { this.alarmType = alarmType; }
    public String getAlarmLevel() { return alarmLevel; }
    public void setAlarmLevel(String alarmLevel) { this.alarmLevel = alarmLevel; }
    public String getAlarmData() { return alarmData; }
    public void setAlarmData(String alarmData) { this.alarmData = alarmData; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getTriggeredTime() { return triggeredTime; }
    public void setTriggeredTime(String triggeredTime) { this.triggeredTime = triggeredTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getHandleResult() { return handleResult; }
    public void setHandleResult(String handleResult) { this.handleResult = handleResult; }
    public String getHandleRemark() { return handleRemark; }
    public void setHandleRemark(String handleRemark) { this.handleRemark = handleRemark; }
    public String getHandledTime() { return handledTime; }
    public void setHandledTime(String handledTime) { this.handledTime = handledTime; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
