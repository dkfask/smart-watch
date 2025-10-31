package com.example.demo.repository;

import com.example.demo.model.FenceAlert;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class FenceAlertRepository {
    private final JdbcTemplate jdbc;

    public FenceAlertRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<FenceAlert> MAPPER = (rs, n) -> {
        FenceAlert a = new FenceAlert();
        a.setAlertId(rs.getLong("alert_id"));
        a.setFenceId(rs.getLong("fence_id"));
        a.setDeviceId(rs.getLong("device_id"));
        a.setAlertType(rs.getString("alert_type"));
        var t = rs.getTimestamp("triggered_time");
        a.setTriggeredTime(t != null ? t.toLocalDateTime() : null);
        Object rd = rs.getObject("is_read");
        a.setIsRead(rd == null ? null : rs.getBoolean("is_read"));
        return a;
    };

    public long create(long fenceId, long deviceId, String type) {
        String sql = "INSERT INTO fence_alerts(fence_id, device_id, alert_type) VALUES(?,?,?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, fenceId);
            ps.setLong(2, deviceId);
            ps.setString(3, type);
            return ps;
        }, kh);
        return kh.getKey() == null ? 0L : kh.getKey().longValue();
    }

    public List<FenceAlert> listByDevice(long deviceId, int limit, int offset) {
        return jdbc.query("SELECT * FROM fence_alerts WHERE device_id=? ORDER BY triggered_time DESC LIMIT ? OFFSET ?", MAPPER, deviceId, limit, offset);
    }

    public int markRead(long alertId, boolean read) {
        return jdbc.update("UPDATE fence_alerts SET is_read=?, triggered_time=triggered_time WHERE alert_id=?", read, alertId);
    }
}

