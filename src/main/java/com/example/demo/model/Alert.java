package com.example.demo.model;

import jakarta.persistence.*;
import java.util.Date;

/**
 * 统一报警实体类
 * 合并 Alarm 和 FenceAlert，统一管理所有类型的报警信息
 */
@Entity
@Table(name = "alerts")
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 报警来源：alarm、fence、device */
    @Column(name = "source", nullable = false, length = 20)
    private String source;

    /** 来源记录ID（alarms表或fence_alerts表的ID） */
    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "imei", length = 15)
    private String imei;

    @Column(name = "fence_id")
    private Long fenceId;

    // 报警类型：fence_breach, low_battery, sos, fall, heart_rate, device_offline, other
    @Column(name = "alert_type", nullable = false, length = 50)
    private String alertType;

    // 报警级别：critical, warning, info
    @Column(name = "alert_level", nullable = false, length = 20)
    private String alertLevel;

    // JSON格式的报警详细数据
    @Column(name = "alert_data", columnDefinition = "text")
    private String alertData;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "address", length = 255)
    private String address;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "alert_time", nullable = false, columnDefinition = "datetime default current_timestamp")
    private Date alertTime;

    // 报警状态：pending, handled, false_alarm
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "varchar(20) default 'pending'")
    private String status;

    // 是否已读：0-未读，1-已读
    @Column(name = "is_read", nullable = false, columnDefinition = "tinyint default 0")
    private Integer isRead;

    @Column(name = "handle_result", length = 50)
    private String handleResult;

    @Column(name = "handle_remark", columnDefinition = "text")
    private String handleRemark;

    @Column(name = "handled_by")
    private Long handledBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "handled_time")
    private Date handledTime;

    @Transient
    private Device device;

    @Transient
    private Patient patient;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime default current_timestamp")
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime default current_timestamp on update current_timestamp")
    private Date updatedAt;

    /**
     * 默认构造函数，初始化默认值
     */
    public Alert() {
        this.alertTime = new Date();
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.status = "pending";
        this.alertLevel = "warning";
        this.isRead = 0;
    }

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
    public Date getAlertTime() { return alertTime; }
    public void setAlertTime(Date alertTime) { this.alertTime = alertTime; }
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
    public Date getHandledTime() { return handledTime; }
    public void setHandledTime(Date handledTime) { this.handledTime = handledTime; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
}
