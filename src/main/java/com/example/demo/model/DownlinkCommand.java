package com.example.demo.model;

import jakarta.persistence.*;
import java.util.Date;

/**
 * DownlinkCommand - 记录由平台下发的下行命令（BPxx）以及状态/响应
 */
@Entity
@Table(name = "downlink_commands")
public class DownlinkCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(length = 15)
    private String imei;

    @Column(length = 16)
    private String protocol;

    @Column(length = 32)
    private String seq;

    @Lob
    private String payload;

    @Column(length = 32)
    private String status;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "sent_at")
    private Date sentAt;

    @Lob
    private String response;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    public DownlinkCommand() {
        this.createdAt = new Date();
    }

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }

    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getSeq() { return seq; }
    public void setSeq(String seq) { this.seq = seq; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getSentAt() { return sentAt; }
    public void setSentAt(Date sentAt) { this.sentAt = sentAt; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
}

