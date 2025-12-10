package com.example.demo.model;

import jakarta.persistence.*;
import java.util.Date;

/**
 * HeartbeatRecord - 存储设备上报的心跳/状态数据（对应 AP03）
 */
@Entity
@Table(name = "heartbeat_records")
public class HeartbeatRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(length = 15)
    private String imei;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "recv_time", nullable = false)
    private Date recvTime;

    @Column(name = "status_block", length = 255)
    private String statusBlock;

    @Column(length = 64)
    private String counter;

    @Column(name = "roll_count", length = 64)
    private String rollCount;

    @Column(name = "work_mode", length = 64)
    private String workMode;

    @Column(name = "interval_seconds")
    private Integer intervalSeconds;

    @Lob
    @Column(name = "raw_payload")
    private String rawPayload;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime default current_timestamp")
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime default current_timestamp on update current_timestamp")
    private Date updatedAt;

    public HeartbeatRecord() {
        this.recvTime = new Date();
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }

    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }

    public Date getRecvTime() { return recvTime; }
    public void setRecvTime(Date recvTime) { this.recvTime = recvTime; }

    public String getStatusBlock() { return statusBlock; }
    public void setStatusBlock(String statusBlock) { this.statusBlock = statusBlock; }

    public String getCounter() { return counter; }
    public void setCounter(String counter) { this.counter = counter; }

    public String getRollCount() { return rollCount; }
    public void setRollCount(String rollCount) { this.rollCount = rollCount; }

    public String getWorkMode() { return workMode; }
    public void setWorkMode(String workMode) { this.workMode = workMode; }

    public Integer getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(Integer intervalSeconds) { this.intervalSeconds = intervalSeconds; }

    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

