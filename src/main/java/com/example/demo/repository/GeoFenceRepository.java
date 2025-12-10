package com.example.demo.repository;

import com.example.demo.model.GeoFence;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public class GeoFenceRepository {
    private final JdbcTemplate jdbc;

    public GeoFenceRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<GeoFence> MAPPER = (rs, n) -> {
        GeoFence f = new GeoFence();
        f.setId(rs.getLong("id"));
        f.setName(rs.getString("name"));
        f.setType(rs.getString("type"));
        // 使用getObject处理可能为null的double值
        f.setCenterLat(rs.getObject("center_lat", Double.class));
        f.setCenterLng(rs.getObject("center_lng", Double.class));
        // 使用getObject处理可能为null的int值
        f.setRadius(rs.getObject("radius", Integer.class));
        f.setCoordinates(rs.getString("coordinates"));
        f.setStatus(rs.getString("status"));
        f.setDescription(rs.getString("description"));
        // 使用getObject处理可能为null的Long值
        f.setCreatedBy(rs.getObject("created_by", Long.class));
        Timestamp c = rs.getTimestamp("created_at");
        f.setCreatedAt(c != null ? new Date(c.getTime()) : null);
        Timestamp u = rs.getTimestamp("updated_at");
        f.setUpdatedAt(u != null ? new Date(u.getTime()) : null);
        return f;
    };

    public long create(GeoFence f) {
        String sql = "INSERT INTO geo_fences(name, type, center_lat, center_lng, radius, coordinates, status, description, created_by, patient_id) VALUES(?,?,?,?,?,?,?,?,?,?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, f.getName());
            ps.setString(2, f.getType());
            ps.setObject(3, f.getCenterLat());
            ps.setObject(4, f.getCenterLng());
            ps.setObject(5, f.getRadius());
            ps.setString(6, f.getCoordinates());
            ps.setString(7, f.getStatus());
            ps.setString(8, f.getDescription());
            ps.setObject(9, f.getCreatedBy());
            ps.setObject(10, f.getPatientId());
            return ps;
        }, kh);
        return kh.getKey() == null ? 0L : kh.getKey().longValue();
    }

    public int update(GeoFence f) {
        return jdbc.update("UPDATE geo_fences SET name=?, type=?, center_lat=?, center_lng=?, radius=?, coordinates=?, status=?, description=?, created_by=?, patient_id=? WHERE id=?",
                f.getName(), f.getType(), f.getCenterLat(), f.getCenterLng(), f.getRadius(), f.getCoordinates(), f.getStatus(), f.getDescription(), f.getCreatedBy(), f.getPatientId(), f.getId());
    }

    public Optional<GeoFence> findById(long id) {
        List<GeoFence> list = jdbc.query("SELECT * FROM geo_fences WHERE id=?", MAPPER, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<GeoFence> listByUser(long userId) {
        return jdbc.query("SELECT * FROM geo_fences WHERE created_by=? ORDER BY id DESC", MAPPER, userId);
    }

    public List<GeoFence> listActiveByUser(long userId) {
        return jdbc.query("SELECT * FROM geo_fences WHERE created_by=? AND status='active' ORDER BY id DESC", MAPPER, userId);
    }

    public List<GeoFence> listAll() {
        return jdbc.query("SELECT * FROM geo_fences ORDER BY id DESC", MAPPER);
    }

    public List<GeoFence> listActive() {
        return jdbc.query("SELECT * FROM geo_fences WHERE status='active' ORDER BY id DESC", MAPPER);
    }

    public int delete(long fenceId) { 
        return jdbc.update("DELETE FROM geo_fences WHERE id=?", fenceId); 
    }

    public int deactivate(long fenceId) {
        return jdbc.update("UPDATE geo_fences SET status='inactive' WHERE id=?", fenceId);
    }
}

