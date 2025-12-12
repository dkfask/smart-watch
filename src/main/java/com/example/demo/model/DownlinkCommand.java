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
    @JoinColumn(name = "device_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Device device;

    @Column(length = 15)
    private String imei;

    @Column(length = 16)
    private String protocol;

    @Column(length = 32)
    private String seq;

    @Lob
    private String payload;

    @Column(length = 32, columnDefinition = "varchar(32) default 'pending'")
    private String status;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime default current_timestamp")
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "sent_at")
    private Date sentAt;

    @Lob
    private String response;

    @Column(name = "retry_count", nullable = false, columnDefinition = "int default 0")
    private Integer retryCount = 0;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime default current_timestamp on update current_timestamp")
    private Date updatedAt;

    @Column(name = "command_type", length = 50, nullable = false)
    private String commandType;

    @Column(name = "command_content", nullable = false)
    @Lob
    private String commandContent;

    @Column(name = "priority", length = 20, columnDefinition = "varchar(20) default 'normal'", nullable = false)
    private String priority = "normal";

    @Column(name = "sent_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date sentTime;

    @Column(name = "delivered_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date deliveredTime;

    @Column(name = "failed_reason")
    @Lob
    private String failedReason;

    @Column(name = "created_by")
    private Long createdBy;

    public DownlinkCommand() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.retryCount = 0;
        this.status = "pending";
        this.priority = "normal";
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

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getCommandType() { return commandType; }
    public void setCommandType(String commandType) { this.commandType = commandType; }

    public String getCommandContent() { return commandContent; }
    public void setCommandContent(String commandContent) { this.commandContent = commandContent; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Date getSentTime() { return sentTime; }
    public void setSentTime(Date sentTime) { this.sentTime = sentTime; }

    public Date getDeliveredTime() { return deliveredTime; }
    public void setDeliveredTime(Date deliveredTime) { this.deliveredTime = deliveredTime; }

    public String getFailedReason() { return failedReason; }
    public void setFailedReason(String failedReason) { this.failedReason = failedReason; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}

