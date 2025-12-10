package com.example.demo.repository;

import com.example.demo.model.UserDevice;
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

@Repository
public class UserDeviceRepository {
    private final JdbcTemplate jdbc;

    public UserDeviceRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<UserDevice> MAPPER = (rs, n) -> {
        UserDevice ud = new UserDevice();
        ud.setId(rs.getLong("id"));
        ud.setUserId(rs.getLong("user_id"));
        ud.setDeviceId(rs.getLong("device_id"));
        ud.setRelationship(rs.getString("relationship"));
        Timestamp c = rs.getTimestamp("created_at");
        ud.setCreatedAt(c != null ? new Date(c.getTime()) : null);
        return ud;
    };

    public long bind(long userId, long deviceId, String relationship) {
        String sql = "INSERT INTO user_devices(user_id, device_id, relationship) VALUES(?,?,?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setLong(2, deviceId);
            ps.setString(3, relationship);
            return ps;
        }, kh);
        return kh.getKey() == null ? 0L : kh.getKey().longValue();
    }

    public int unbind(long userId, long deviceId) {
        return jdbc.update("DELETE FROM user_devices WHERE user_id=? AND device_id=?", userId, deviceId);
    }

    public List<UserDevice> findByUser(long userId) {
        return jdbc.query("SELECT * FROM user_devices WHERE user_id=? ORDER BY id DESC", MAPPER, userId);
    }

    public List<UserDevice> findByDevice(long deviceId) {
        return jdbc.query("SELECT * FROM user_devices WHERE device_id=? ORDER BY id DESC", MAPPER, deviceId);
    }
}

