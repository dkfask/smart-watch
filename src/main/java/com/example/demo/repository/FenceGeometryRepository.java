package com.example.demo.repository;

import com.example.demo.model.FenceGeometry;
import com.example.demo.model.FenceGeometry.GeometryType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FenceGeometryRepository {

    private final JdbcTemplate jdbc;

    public FenceGeometryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<FenceGeometry> MAPPER = (rs, n) -> {
        FenceGeometry g = new FenceGeometry();
        g.setFenceId(rs.getLong("fence_id"));
        String t = rs.getString("geometry_type");
        if (t != null) {
            try { g.setGeometryType(GeometryType.valueOf(t)); }
            catch (IllegalArgumentException ignored) {}
        }
        g.setCenterLat((Double) rs.getObject("center_lat"));
        g.setCenterLng((Double) rs.getObject("center_lng"));
        g.setRadiusM((Integer) rs.getObject("radius_m"));
        g.setPolygonCoords(rs.getString("polygon_coords"));
        g.setRectSwLat((Double) rs.getObject("rect_sw_lat"));
        g.setRectSwLng((Double) rs.getObject("rect_sw_lng"));
        g.setRectNeLat((Double) rs.getObject("rect_ne_lat"));
        g.setRectNeLng((Double) rs.getObject("rect_ne_lng"));
        return g;
    };

    public FenceGeometry findByFenceId(long fenceId) {
        List<FenceGeometry> list = jdbc.query(
            "SELECT * FROM fence_geometries WHERE fence_id=?", MAPPER, fenceId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<FenceGeometry> findByFenceIds(List<Long> fenceIds) {
        if (fenceIds == null || fenceIds.isEmpty()) return List.of();
        String placeholders = String.join(",", fenceIds.stream().map(i -> "?").toList());
        return jdbc.query(
            "SELECT * FROM fence_geometries WHERE fence_id IN (" + placeholders + ")",
            MAPPER, fenceIds.toArray());
    }

    public List<FenceGeometry> findAllActive() {
        // geo_fences.status='active' gates the join.
        return jdbc.query(
            "SELECT g.* FROM fence_geometries g JOIN geo_fences f ON f.id=g.fence_id "
          + "WHERE f.status='active'",
            MAPPER);
    }

    public int upsert(FenceGeometry g) {
        return jdbc.update(
            "INSERT INTO fence_geometries "
          + "(fence_id, geometry_type, center_lat, center_lng, radius_m, "
          + " polygon_coords, rect_sw_lat, rect_sw_lng, rect_ne_lat, rect_ne_lng) "
          + "VALUES (?,?,?,?,?,?,?,?,?,?) "
          + "ON DUPLICATE KEY UPDATE "
          + "  geometry_type=VALUES(geometry_type), "
          + "  center_lat=VALUES(center_lat), "
          + "  center_lng=VALUES(center_lng), "
          + "  radius_m=VALUES(radius_m), "
          + "  polygon_coords=VALUES(polygon_coords), "
          + "  rect_sw_lat=VALUES(rect_sw_lat), "
          + "  rect_sw_lng=VALUES(rect_sw_lng), "
          + "  rect_ne_lat=VALUES(rect_ne_lat), "
          + "  rect_ne_lng=VALUES(rect_ne_lng)",
            g.getFenceId(),
            g.getGeometryType() == null ? null : g.getGeometryType().name(),
            g.getCenterLat(), g.getCenterLng(), g.getRadiusM(),
            g.getPolygonCoords(),
            g.getRectSwLat(), g.getRectSwLng(),
            g.getRectNeLat(), g.getRectNeLng());
    }

    public int deleteByFenceId(long fenceId) {
        return jdbc.update("DELETE FROM fence_geometries WHERE fence_id=?", fenceId);
    }
}
