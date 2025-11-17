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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(length = 15)
    private String imei;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "recv_time")
    private Date recvTime;

    @Column(name = "data_type", length = 32)
    private String dataType;

    @Lob
    private String value;

    @Lob
    private String extra;

    public HealthRecord() { this.recvTime = new Date(); }

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }

    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }

    public Date getRecvTime() { return recvTime; }
    public void setRecvTime(Date recvTime) { this.recvTime = recvTime; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getExtra() { return extra; }
    public void setExtra(String extra) { this.extra = extra; }
}

