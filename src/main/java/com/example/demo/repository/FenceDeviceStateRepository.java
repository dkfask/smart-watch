package com.example.demo.repository;

import com.example.demo.model.FenceDeviceState;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Repository
public class FenceDeviceStateRepository {

    private final JdbcTemplate jdbc;

    public FenceDeviceStateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<FenceDeviceState> MAPPER = (rs, n) -> {
        FenceDeviceState s = new FenceDeviceState();
        s.setFenceId(rs.getLong("fence_id"));
        s.setDeviceId(rs.getLong("device_id"));
        s.setIsInside(rs.getBoolean("is_inside"));
        Timestamp c = rs.getTimestamp("last_changed_at");
        s.setLastChangedAt(c != null ? new Date(c.getTime()) : null);
        Timestamp in = rs.getTimestamp("inside_since");
        s.setInsideSince(in != null ? new Date(in.getTime()) : null);
        s.setLastLat((Double) rs.getObject("last_lat"));
        s.setLastLng((Double) rs.getObject("last_lng"));
        s.setOutsideCount((Integer) rs.getObject("outside_count"));
        return s;
    };

    public FenceDeviceState findOne(long fenceId, long deviceId) {
        List<FenceDeviceState> list = jdbc.query(
            "SELECT * FROM fence_device_state WHERE fence_id=? AND device_id=?",
            MAPPER, fenceId, deviceId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<FenceDeviceState> findByDevice(long deviceId) {
        return jdbc.query(
            "SELECT * FROM fence_device_state WHERE device_id=?",
            MAPPER, deviceId);
    }

    public List<FenceDeviceState> findInsideByFence(long fenceId) {
        return jdbc.query(
            "SELECT * FROM fence_device_state WHERE fence_id=? AND is_inside=1",
            MAPPER, fenceId);
    }

    /** Upsert — insert if absent, otherwise update. */
    public int upsert(FenceDeviceState s) {
        return jdbc.update(
            "INSERT INTO fence_device_state "
          + "(fence_id, device_id, is_inside, last_changed_at, inside_since, "
          + " last_lat, last_lng, outside_count) "
          + "VALUES (?,?,?,?,?,?,?,?) "
          + "ON DUPLICATE KEY UPDATE "
          + "  is_inside=VALUES(is_inside), "
          + "  last_changed_at=VALUES(last_changed_at), "
          + "  inside_since=VALUES(inside_since), "
          + "  last_lat=VALUES(last_lat), "
          + "  last_lng=VALUES(last_lng), "
          + "  outside_count=VALUES(outside_count)",
            s.getFenceId(), s.getDeviceId(),
            s.getIsInside() == null ? false : s.getIsInside(),
            s.getLastChangedAt() == null ? null : new Timestamp(s.getLastChangedAt().getTime()),
            s.getInsideSince() == null ? null : new Timestamp(s.getInsideSince().getTime()),
            s.getLastLat(), s.getLastLng(),
            s.getOutsideCount() == null ? 0 : s.getOutsideCount());
    }

    public int deleteOne(long fenceId, long deviceId) {
        return jdbc.update(
            "DELETE FROM fence_device_state WHERE fence_id=? AND device_id=?",
            fenceId, deviceId);
    }
}
