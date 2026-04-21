package com.smartwatch.monitor.model;

import com.google.gson.annotations.SerializedName;

/**
 * 电子围栏模型类，对应后端geo_fences表
 */
public class GeoFence {

    /** 围栏ID */
    private Long id;

    /** 围栏名称 */
    private String name;

    /** 围栏类型：circle-圆形，polygon-多边形，rectangle-矩形 */
    private String type;

    /** 中心纬度 */
    @SerializedName("center_lat")
    private Double centerLat;

    /** 中心经度 */
    @SerializedName("center_lng")
    private Double centerLng;

    /** 半径（米，仅用于圆形围栏） */
    private Integer radius;

    /** 围栏坐标，JSON格式 */
    private String coordinates;

    /** 状态：active-活跃，inactive-停用 */
    private String status;

    /** 描述 */
    private String description;

    /** 创建者ID */
    @SerializedName("created_by")
    private Long createdBy;

    /** 关联的病人ID */
    @SerializedName("patient_id")
    private Long patientId;

    /** 是否支持多病人关联 */
    @SerializedName("is_multi_patient")
    private Boolean isMultiPatient;

    /** 创建时间 */
    @SerializedName("created_at")
    private String createdAt;

    /** 更新时间 */
    @SerializedName("updated_at")
    private String updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Double getCenterLat() { return centerLat; }
    public void setCenterLat(Double centerLat) { this.centerLat = centerLat; }
    public Double getCenterLng() { return centerLng; }
    public void setCenterLng(Double centerLng) { this.centerLng = centerLng; }
    public Integer getRadius() { return radius; }
    public void setRadius(Integer radius) { this.radius = radius; }
    public String getCoordinates() { return coordinates; }
    public void setCoordinates(String coordinates) { this.coordinates = coordinates; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Boolean getIsMultiPatient() { return isMultiPatient; }
    public void setIsMultiPatient(Boolean isMultiPatient) { this.isMultiPatient = isMultiPatient; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
