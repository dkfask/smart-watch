package com.example.demo.model;

import jakarta.persistence.*;
import java.util.Date;

/**
 * 围栏报警实体类
 * 用于记录设备越出围栏的报警信息
 */
@Entity
@Table(name = "fence_alerts")
public class FenceAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "imei", length = 15)
    private String imei;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fence_id", nullable = false)
    private GeoFence fence;
    
    @Column(name = "alert_type", nullable = false, length = 20)
    private String alertType = "fence_breach"; // fence_breach: 围栏越界
    
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "varchar(20) default 'pending'")
    private String status = "pending"; // pending: 待处理, handled: 已处理, false_alarm: 误报
    
    @Column(name = "latitude", nullable = false)
    private Double latitude;
    
    @Column(name = "longitude", nullable = false)
    private Double longitude;
    
    // 地址字段不参与数据库映射，标记为Transient
    @Transient
    private String address;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "alert_time", nullable = false, columnDefinition = "datetime default current_timestamp")
    private Date alertTime;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "handled_time")
    private Date handledTime;
    
    @Column(name = "handled_by")
    private Long handledBy;
    
    // remark字段不参与数据库映射，标记为Transient
    @Transient
    private String remark;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime default current_timestamp")
    private Date createdAt;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime default current_timestamp on update current_timestamp")
    private Date updatedAt;
    
    @Column(name = "is_read", nullable = false, columnDefinition = "tinyint default 0")
    private Integer isRead;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "triggered_time", nullable = false, columnDefinition = "datetime default current_timestamp")
    private Date triggeredTime;
    
    // Default constructor
    public FenceAlert() {
        this.alertTime = new Date();
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.isRead = 0;
        this.triggeredTime = new Date();
    }
    
    // Parameterized constructor for quick creation
    public FenceAlert(Device device, Patient patient, GeoFence fence, Double latitude, Double longitude) {
        this.device = device;
        this.patient = patient;
        this.fence = fence;
        this.latitude = latitude;
        this.longitude = longitude;
        this.alertType = "fence_breach";
        this.status = "pending";
        this.alertTime = new Date();
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.isRead = 0;
        this.triggeredTime = new Date();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }
    public GeoFence getFence() { return fence; }
    public void setFence(GeoFence fence) { this.fence = fence; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Date getAlertTime() { return alertTime; }
    public void setAlertTime(Date alertTime) { this.alertTime = alertTime; }
    public Date getHandledTime() { return handledTime; }
    public void setHandledTime(Date handledTime) { this.handledTime = handledTime; }
    public Long getHandledBy() { return handledBy; }
    public void setHandledBy(Long handledBy) { this.handledBy = handledBy; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }
    public Date getTriggeredTime() { return triggeredTime; }
    public void setTriggeredTime(Date triggeredTime) { this.triggeredTime = triggeredTime; }
}