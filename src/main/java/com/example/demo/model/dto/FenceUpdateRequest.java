package com.example.demo.model.dto;

import java.util.List;

/**
 * 围栏更新请求DTO，替代GeoFenceController中的Map<String, Object>请求体
 */
public class FenceUpdateRequest {
    private String name;
    private String type;
    private Integer radius;
    private String coordinates;
    private String status;
    private String description;
    private Long createdBy;
    private Long patientId;
    private Boolean isMultiPatient;
    private Double centerLat;
    private Double centerLng;
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
