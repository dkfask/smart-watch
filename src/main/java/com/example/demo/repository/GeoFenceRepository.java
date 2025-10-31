package com.example.demo.repository;

import com.example.demo.model.GeoFence;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class GeoFenceRepository {
    private final JdbcTemplate jdbc;

    public GeoFenceRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<GeoFence> MAPPER = (rs, n) -> {
        GeoFence f = new GeoFence();
        f.setFenceId(rs.getLong("fence_id"));
        f.setUserId(rs.getLong("user_id"));
        f.setName(rs.getString("name"));
        f.setCenterLatitude(rs.getBigDecimal("center_latitude"));
        f.setCenterLongitude(rs.getBigDecimal("center_longitude"));
        f.setRadius(rs.getBigDecimal("radius"));
        f.setTriggerType(rs.getString("trigger_type"));
        Object act = rs.getObject("is_active");
        f.setIsActive(act == null ? null : rs.getBoolean("is_active"));
        Timestamp c = rs.getTimestamp("created_at");
        f.setCreatedAt(c != null ? c.toLocalDateTime() : null);
        return f;
    };

    public long create(GeoFence f) {
        String sql = "INSERT INTO geo_fences(user_id,name,center_latitude,center_longitude,radius,trigger_type,is_active) VALUES(?,?,?,?,?,?,?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, f.getUserId());
            ps.setString(2, f.getName());
            ps.setBigDecimal(3, f.getCenterLatitude());
            ps.setBigDecimal(4, f.getCenterLongitude());
            ps.setBigDecimal(5, f.getRadius());
            ps.setString(6, f.getTriggerType());
            if (f.getIsActive() == null) ps.setObject(7, null); else ps.setBoolean(7, f.getIsActive());
            return ps;
        }, kh);
        return kh.getKey() == null ? 0L : kh.getKey().longValue();
    }

    public int update(GeoFence f) {
        return jdbc.update("UPDATE geo_fences SET name=?, center_latitude=?, center_longitude=?, radius=?, trigger_type=?, is_active=? WHERE fence_id=?",
                f.getName(), f.getCenterLatitude(), f.getCenterLongitude(), f.getRadius(), f.getTriggerType(), f.getIsActive(), f.getFenceId());
    }

    public Optional<GeoFence> findById(long id) {
        List<GeoFence> list = jdbc.query("SELECT * FROM geo_fences WHERE fence_id=?", MAPPER, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<GeoFence> listByUser(long userId) {
        return jdbc.query("SELECT * FROM geo_fences WHERE user_id=? ORDER BY fence_id DESC", MAPPER, userId);
    }

    public List<GeoFence> listActiveByUser(long userId) {
        return jdbc.query("SELECT * FROM geo_fences WHERE user_id=? AND is_active=TRUE ORDER BY fence_id DESC", MAPPER, userId);
    }

    public int delete(long fenceId) { return jdbc.update("DELETE FROM geo_fences WHERE fence_id=?", fenceId); }
}

