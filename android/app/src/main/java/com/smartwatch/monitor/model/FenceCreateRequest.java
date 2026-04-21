package com.smartwatch.monitor.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * 围栏创建请求DTO
 */
public class FenceCreateRequest {

    /** 围栏名称 */
    private String name;

    /** 围栏类型 */
    private String type;

    /** 半径（米） */
    private Integer radius;

    /** 围栏坐标JSON */
    private String coordinates;

    /** 状态 */
    private String status;

    /** 描述 */
    private String description;

    /** 创建者ID */
    @SerializedName("createdBy")
    private Long createdBy;

    /** 关联病人ID */
    @SerializedName("patientId")
    private Long patientId;

    /** 是否多病人关联 */
    @SerializedName("isMultiPatient")
    private Boolean isMultiPatient;

    /** 中心纬度 */
    @SerializedName("centerLat")
    private Double centerLat;

    /** 中心经度 */
    @SerializedName("centerLng")
    private Double centerLng;

    /** 关联病人ID列表 */
    @SerializedName("patientIds")
    private List<Long> patientIds;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
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
    public Double getCenterLat() { return centerLat; }
    public void setCenterLat(Double centerLat) { this.centerLat = centerLat; }
    public Double getCenterLng() { return centerLng; }
    public void setCenterLng(Double centerLng) { this.centerLng = centerLng; }
    public List<Long> getPatientIds() { return patientIds; }
    public void setPatientIds(List<Long> patientIds) { this.patientIds = patientIds; }
}
