package com.example.demo.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "alarms")
public class Alarm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "device_id", nullable = false)
    private Long deviceId;
    
    @Column(name = "patient_id")
    private Long patientId;
    
    // 关联对象，用于前端显示设备和病人信息
    @Transient
    private Device device;
    
    @Transient
    private Patient patient;
    
    @Column(name = "alarm_type", nullable = false, length = 50)
    private String alarmType; // fence_breach, low_battery, sos, fall, heart_rate, etc.
    
    @Column(name = "alarm_level", nullable = false, length = 20)
    private String alarmLevel; // critical, warning, info
    
    @Column(name = "alarm_data", columnDefinition = "text")
    private String alarmData; // JSON格式的报警详细数据
    
    @Column(name = "latitude")
    private Double latitude;
    
    @Column(name = "longitude")
    private Double longitude;
    
    @Column(name = "address")
    private String address;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "triggered_time", nullable = false, columnDefinition = "datetime default current_timestamp")
    private Date triggeredTime;
    
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "varchar(20) default 'pending'")
    private String status; // pending, handled, false_alarm
    
    @Column(name = "handle_result", length = 50)
    private String handleResult;
    
    @Column(name = "handle_remark", columnDefinition = "text")
    private String handleRemark;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "handled_time")
    private Date handledTime;
    
    @Column(name = "is_read", nullable = false, columnDefinition = "boolean default false")
    private Boolean isRead;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime default current_timestamp")
    private Date createdAt;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime default current_timestamp on update current_timestamp")
    private Date updatedAt;
    
    // Default constructor
    public Alarm() {
        this.isRead = false;
        this.triggeredTime = new Date();
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.status = "pending";
        this.alarmLevel = "warning";
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
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
    public Date getTriggeredTime() { return triggeredTime; }
    public void setTriggeredTime(Date triggeredTime) { this.triggeredTime = triggeredTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getHandleResult() { return handleResult; }
    public void setHandleResult(String handleResult) { this.handleResult = handleResult; }
    public String getHandleRemark() { return handleRemark; }
    public void setHandleRemark(String handleRemark) { this.handleRemark = handleRemark; }
    public Date getHandledTime() { return handledTime; }
    public void setHandledTime(Date handledTime) { this.handledTime = handledTime; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean read) { isRead = read; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}