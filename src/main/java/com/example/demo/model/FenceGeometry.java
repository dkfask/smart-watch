package com.example.demo.model;

import jakarta.persistence.*;
import java.util.Date;

/**
 * Normalised geometry storage for a {@link GeoFence}. One row per fence.
 *
 * <p>This table is the single source of truth for fence geometry math.
 * The {@code geo_fences} table keeps denormalised legacy columns
 * (center_lat, center_lng, radius, coordinates JSON) for backward
 * compatibility with existing read paths and the frontend, but all
 * new geometry math should read from this table.
 *
 * <p>Discriminator: {@link #geometryType} picks which field group is
 * populated:
 * <ul>
 *   <li>{@code CIRCLE}    — center_lat, center_lng, radius_m</li>
 *   <li>{@code POLYGON}   — polygon_coords (JSON array of [lat,lng])</li>
 *   <li>{@code RECTANGLE} — rect_sw_lat/lng + rect_ne_lat/lng</li>
 * </ul>
 */
@Entity
@Table(name = "fence_geometries")
public class FenceGeometry {

    @Id
    @Column(name = "fence_id")
    private Long fenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "geometry_type", nullable = false, length = 20)
    private GeometryType geometryType;

    @Column(name = "center_lat")                  private Double centerLat;
    @Column(name = "center_lng")                  private Double centerLng;
    @Column(name = "radius_m")                    private Integer radiusM;

    /** JSON array of [lat,lng] pairs (POLYGON only). Stored as String for portability. */
    @Column(name = "polygon_coords", columnDefinition = "json")
    private String polygonCoords;

    @Column(name = "rect_sw_lat")                 private Double rectSwLat;
    @Column(name = "rect_sw_lng")                 private Double rectSwLng;
    @Column(name = "rect_ne_lat")                 private Double rectNeLat;
    @Column(name = "rect_ne_lng")                 private Double rectNeLng;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at",
            nullable = false,
            insertable = false,
            updatable = false,
            columnDefinition = "datetime default current_timestamp on update current_timestamp")
    private Date updatedAt;

    public enum GeometryType { CIRCLE, POLYGON, RECTANGLE }

    public FenceGeometry() {}

    public Long getFenceId() { return fenceId; }
    public void setFenceId(Long fenceId) { this.fenceId = fenceId; }
    public GeometryType getGeometryType() { return geometryType; }
    public void setGeometryType(GeometryType geometryType) { this.geometryType = geometryType; }
    public Double getCenterLat() { return centerLat; }
    public void setCenterLat(Double v) { this.centerLat = v; }
    public Double getCenterLng() { return centerLng; }
    public void setCenterLng(Double v) { this.centerLng = v; }
    public Integer getRadiusM() { return radiusM; }
    public void setRadiusM(Integer v) { this.radiusM = v; }
    public String getPolygonCoords() { return polygonCoords; }
    public void setPolygonCoords(String v) { this.polygonCoords = v; }
    public Double getRectSwLat() { return rectSwLat; }
    public void setRectSwLat(Double v) { this.rectSwLat = v; }
    public Double getRectSwLng() { return rectSwLng; }
    public void setRectSwLng(Double v) { this.rectSwLng = v; }
    public Double getRectNeLat() { return rectNeLat; }
    public void setRectNeLat(Double v) { this.rectNeLat = v; }
    public Double getRectNeLng() { return rectNeLng; }
    public void setRectNeLng(Double v) { this.rectNeLng = v; }
    public Date getUpdatedAt() { return updatedAt; }
}
