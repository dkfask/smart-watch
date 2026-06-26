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
        f.setCenterLat(rs.getObject("center_lat", Double.class));
        f.setCenterLng(rs.getObject("center_lng", Double.class));
        f.setRadius(rs.getObject("radius", Integer.class));
        f.setCoordinates(rs.getString("coordinates"));
        f.setStatus(rs.getString("status"));
        f.setDescription(rs.getString("description"));
        f.setCreatedBy(rs.getObject("created_by", Long.class));
        f.setPatientId(rs.getObject("patient_id", Long.class));
        f.setIsMultiPatient(rs.getObject("is_multi_patient", Boolean.class));
        Timestamp c = rs.getTimestamp("created_at");
        f.setCreatedAt(c != null ? new Date(c.getTime()) : null);
        Timestamp u = rs.getTimestamp("updated_at");
        f.setUpdatedAt(u != null ? new Date(u.getTime()) : null);
        return f;
    };

    public long create(GeoFence f) {
        String sql = "INSERT INTO geo_fences(name, type, center_lat, center_lng, radius, coordinates, status, description, created_by, patient_id, is_multi_patient) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
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
            ps.setObject(11, f.getIsMultiPatient());
            return ps;
        }, kh);
        return kh.getKey() == null ? 0L : kh.getKey().longValue();
    }

    public int update(GeoFence f) {
        return jdbc.update("UPDATE geo_fences SET name=?, type=?, center_lat=?, center_lng=?, radius=?, coordinates=?, status=?, description=?, created_by=?, patient_id=?, is_multi_patient=? WHERE id=?",
                f.getName(), f.getType(), f.getCenterLat(), f.getCenterLng(), f.getRadius(), f.getCoordinates(), f.getStatus(), f.getDescription(), f.getCreatedBy(), f.getPatientId(), f.getIsMultiPatient(), f.getId());
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
    
    /**
     * 根据病人ID获取关联的围栏列表
     * @param patientId 病人ID
     * @return 围栏列表
     */
    public List<GeoFence> listByPatient(Long patientId) {
        return jdbc.query(
            "SELECT * FROM geo_fences WHERE patient_id = ? AND status='active' ORDER BY id DESC",
            MAPPER,
            patientId
        );
    }

    /**
     * Look up active fences via the {@code fence_patients} many-to-many
     * join table. Used by FenceEngine to find fences assigned through
     * the newer m:n relationship in addition to the legacy single-FK
     * path on {@code geo_fences.patient_id}.
     */
    public List<GeoFence> listActiveByFencePatients(Long patientId) {
        return jdbc.query(
            "SELECT gf.* FROM geo_fences gf "
          + "INNER JOIN fence_patients fp ON gf.id = fp.fence_id "
          + "WHERE fp.patient_id = ? AND gf.status = 'active' "
          + "ORDER BY gf.id DESC",
            MAPPER, patientId);
    }
}

