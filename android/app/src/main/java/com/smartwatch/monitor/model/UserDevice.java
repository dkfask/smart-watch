package com.smartwatch.monitor.model;

import com.google.gson.annotations.SerializedName;

/**
 * 用户-设备关联模型类，对应后端user_devices表
 */
public class UserDevice {

    /** 关联ID */
    private Long id;

    /** 用户ID */
    @SerializedName("user_id")
    private Long userId;

    /** 设备ID */
    @SerializedName("device_id")
    private Long deviceId;

    /** 关系：owner-所有者，viewer-查看者，guardian-监护人 */
    private String relationship;

    /** 创建时间 */
    @SerializedName("created_at")
    private String createdAt;

    /** 更新时间 */
    @SerializedName("updated_at")
    private String updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
