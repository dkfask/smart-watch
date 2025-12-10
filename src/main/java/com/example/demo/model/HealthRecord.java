package com.example.demo.model;

import jakarta.persistence.*;
import java.util.Date;

/**
 * HealthRecord - 存储健康类数据（APJK、APTP 等）
 */
@Entity
@Table(name = "health_records")
public class HealthRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(length = 15)
    private String imei;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "recv_time", nullable = false)
    private Date recvTime;

    @Column(name = "data_type", length = 32, nullable = false)
    private String dataType;

    @Column(nullable = false)
    private String value;

    @Lob
    @Column(name = "raw_data")
    private String rawData;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime default current_timestamp")
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime default current_timestamp on update current_timestamp")
    private Date updatedAt;

    public HealthRecord() {
        this.recvTime = new Date();
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }

    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }

    public Date getRecvTime() { return recvTime; }
    public void setRecvTime(Date recvTime) { this.recvTime = recvTime; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getRawData() { return rawData; }
    public void setRawData(String rawData) { this.rawData = rawData; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

