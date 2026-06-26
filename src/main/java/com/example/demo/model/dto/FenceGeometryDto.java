package com.example.demo.model.dto;

import com.example.demo.model.FenceGeometry;
import com.example.demo.model.FenceGeometry.GeometryType;

import java.util.List;

/**
 * Geometry payload for fence create/update responses.
 *
 * <p>One of {@code circle}, {@code polygon}, or {@code rectangle} is
 * meaningful per DTO; the server validates the matching field group.
 *
 * <p>Stored as a String {@code coordinates} for backwards compatibility
 * with the legacy {@code geo_fences.coordinates} JSON column, AND as
 * normalised fields for the new {@code fence_geometries} table.
 */
public class FenceGeometryDto {

    /** CIRCLE / POLYGON / RECTANGLE */
    private String type;

    // CIRCLE
    private Double centerLat;
    private Double centerLng;
    private Integer radiusM;

    // POLYGON: list of [lat, lng] pairs
    private List<double[]> polygon;

    // RECTANGLE: south-west and north-east corners
    private Double rectSwLat;
    private Double rectSwLng;
    private Double rectNeLat;
    private Double rectNeLng;

    public FenceGeometryDto() {}

    public static FenceGeometryDto fromEntity(FenceGeometry g) {
        if (g == null) return null;
        FenceGeometryDto d = new FenceGeometryDto();
        d.type = g.getGeometryType() == null ? null : g.getGeometryType().name().toLowerCase();
        d.centerLat = g.getCenterLat();
        d.centerLng = g.getCenterLng();
        d.radiusM = g.getRadiusM();
        d.rectSwLat = g.getRectSwLat();
        d.rectSwLng = g.getRectSwLng();
        d.rectNeLat = g.getRectNeLat();
        d.rectNeLng = g.getRectNeLng();
        // polygon_coords is a JSON array string; the controller may parse
        // it into List<double[]> for the response when needed.
        return d;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Double getCenterLat() { return centerLat; }
    public void setCenterLat(Double v) { this.centerLat = v; }
    public Double getCenterLng() { return centerLng; }
    public void setCenterLng(Double v) { this.centerLng = v; }
    public Integer getRadiusM() { return radiusM; }
    public void setRadiusM(Integer v) { this.radiusM = v; }
    public List<double[]> getPolygon() { return polygon; }
    public void setPolygon(List<double[]> v) { this.polygon = v; }
    public Double getRectSwLat() { return rectSwLat; }
    public void setRectSwLat(Double v) { this.rectSwLat = v; }
    public Double getRectSwLng() { return rectSwLng; }
    public void setRectSwLng(Double v) { this.rectSwLng = v; }
    public Double getRectNeLat() { return rectNeLat; }
    public void setRectNeLat(Double v) { this.rectNeLat = v; }
    public Double getRectNeLng() { return rectNeLng; }
    public void setRectNeLng(Double v) { this.rectNeLng = v; }
}
