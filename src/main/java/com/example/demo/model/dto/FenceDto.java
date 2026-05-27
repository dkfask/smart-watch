package com.example.demo.model.dto;

import com.example.demo.model.GeoFence;

import java.util.Date;
import java.util.List;

/**
 * 围栏详情DTO，替代GeoFenceController中手动构建的Map<String, Object>
 */
public class FenceDto {
    private Long id;
    private String name;
    private String type;
    private Double centerLat;
    private Double centerLng;
    private Integer radius;
    private String coordinates;
    private String status;
    private String description;
    private Long createdBy;
    private Long patientId;
    private Boolean isMultiPatient;
    private Date createdAt;
    private Date updatedAt;
    private List<Long> patientIds;

    /**
     * 从GeoFence实体构建FenceDto（不含patientIds）
     * @param fence 围栏实体
     * @return FenceDto
     */
    public static FenceDto fromFence(GeoFence fence) {
        FenceDto dto = new FenceDto();
        dto.setId(fence.getId());
        dto.setName(fence.getName());
        dto.setType(fence.getType());
        dto.setCenterLat(fence.getCenterLat());
        dto.setCenterLng(fence.getCenterLng());
        dto.setRadius(fence.getRadius());
        dto.setCoordinates(fence.getCoordinates());
        dto.setStatus(fence.getStatus());
        dto.setDescription(fence.getDescription());
        dto.setCreatedBy(fence.getCreatedBy());
        dto.setPatientId(fence.getPatientId());
        dto.setIsMultiPatient(fence.getIsMultiPatient());
        dto.setCreatedAt(fence.getCreatedAt());
        dto.setUpdatedAt(fence.getUpdatedAt());
        return dto;
    }

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
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public List<Long> getPatientIds() { return patientIds; }
    public void setPatientIds(List<Long> patientIds) { this.patientIds = patientIds; }
}
