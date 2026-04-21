package com.smartwatch.monitor.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

/**
 * 报警模型类，对应后端alerts表，同时作为Room数据库实体
 */
@Entity(tableName = "alerts")
public class Alert {

    /** 报警ID */
    @PrimaryKey
    private Long id;

    /** 报警来源：alarm、fence、device */
    @ColumnInfo(name = "source")
    private String source;

    /** 来源记录ID */
    @ColumnInfo(name = "source_id")
    @SerializedName("source_id")
    private Long sourceId;

    /** 设备ID */
    @ColumnInfo(name = "device_id")
    @SerializedName("device_id")
    private Long deviceId;

    /** 病人ID */
    @ColumnInfo(name = "patient_id")
    @SerializedName("patient_id")
    private Long patientId;

    /** 设备IMEI号 */
    @ColumnInfo(name = "imei")
    private String imei;

    /** 围栏ID */
    @ColumnInfo(name = "fence_id")
    @SerializedName("fence_id")
    private Long fenceId;

    /** 报警类型：fence_breach、low_battery、sos、fall、heart_rate等 */
    @ColumnInfo(name = "alert_type")
    @SerializedName("alert_type")
    private String alertType;

    /** 报警级别：critical、warning、info */
    @ColumnInfo(name = "alert_level")
    @SerializedName("alert_level")
    private String alertLevel;

    /** 报警详细数据JSON */
    @ColumnInfo(name = "alert_data")
    @SerializedName("alert_data")
    private String alertData;

    /** 纬度 */
    @ColumnInfo(name = "latitude")
    private Double latitude;

    /** 经度 */
    @ColumnInfo(name = "longitude")
    private Double longitude;

    /** 地址 */
    @ColumnInfo(name = "address")
    private String address;

    /** 报警时间 */
    @ColumnInfo(name = "alert_time")
    @SerializedName("alert_time")
    private String alertTime;

    /** 状态：pending-待处理，handled-已处理，false_alarm-误报 */
    @ColumnInfo(name = "status")
    private String status;

    /** 是否已读：0-未读，1-已读 */
    @ColumnInfo(name = "is_read")
    @SerializedName("is_read")
    private Integer isRead;

    /** 处理结果 */
    @ColumnInfo(name = "handle_result")
    @SerializedName("handle_result")
    private String handleResult;

    /** 处理备注 */
    @ColumnInfo(name = "handle_remark")
    @SerializedName("handle_remark")
    private String handleRemark;

    /** 处理人ID */
    @ColumnInfo(name = "handled_by")
    @SerializedName("handled_by")
    private Long handledBy;

    /** 处理时间 */
    @ColumnInfo(name = "handled_time")
    @SerializedName("handled_time")
    private String handledTime;

    /** 创建时间 */
    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    private String createdAt;

    /** 更新时间 */
    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    private String updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }
    public Long getFenceId() { return fenceId; }
    public void setFenceId(Long fenceId) { this.fenceId = fenceId; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public String getAlertLevel() { return alertLevel; }
    public void setAlertLevel(String alertLevel) { this.alertLevel = alertLevel; }
    public String getAlertData() { return alertData; }
    public void setAlertData(String alertData) { this.alertData = alertData; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getAlertTime() { return alertTime; }
    public void setAlertTime(String alertTime) { this.alertTime = alertTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }
    public String getHandleResult() { return handleResult; }
    public void setHandleResult(String handleResult) { this.handleResult = handleResult; }
    public String getHandleRemark() { return handleRemark; }
    public void setHandleRemark(String handleRemark) { this.handleRemark = handleRemark; }
    public Long getHandledBy() { return handledBy; }
    public void setHandledBy(Long handledBy) { this.handledBy = handledBy; }
    public String getHandledTime() { return handledTime; }
    public void setHandledTime(String handledTime) { this.handledTime = handledTime; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
