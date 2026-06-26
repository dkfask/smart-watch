package com.example.demo.repository;

import com.example.demo.model.FenceEvent;
import com.example.demo.model.FenceEvent.AckStatus;
import com.example.demo.model.FenceEvent.EventType;
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
public class FenceEventRepository {

    private final JdbcTemplate jdbc;

    public FenceEventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<FenceEvent> MAPPER = (rs, n) -> {
        FenceEvent e = new FenceEvent();
        e.setId(rs.getLong("id"));
        e.setFenceId(rs.getLong("fence_id"));
        e.setDeviceId(rs.getLong("device_id"));
        e.setPatientId(rs.getLong("patient_id"));
        String et = rs.getString("event_type");
        if (et != null) { try { e.setEventType(EventType.valueOf(et)); } catch (IllegalArgumentException ignored) {} }
        e.setLatitude((Double) rs.getObject("latitude"));
        e.setLongitude((Double) rs.getObject("longitude"));
        Timestamp t = rs.getTimestamp("occurred_at");
        e.setOccurredAt(t != null ? new Date(t.getTime()) : null);
        e.setDwellSeconds((Integer) rs.getObject("dwell_seconds"));
        String ack = rs.getString("ack_status");
        if (ack != null) { try { e.setAckStatus(AckStatus.valueOf(ack)); } catch (IllegalArgumentException ignored) {} }
        e.setAckUserId((Long) rs.getObject("ack_user_id"));
        Timestamp at = rs.getTimestamp("ack_at");
        e.setAckAt(at != null ? new Date(at.getTime()) : null);
        e.setAckRemark(rs.getString("ack_remark"));
        return e;
    };

    public long insert(FenceEvent e) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO fence_events "
              + "(fence_id, device_id, patient_id, event_type, latitude, longitude, "
              + " occurred_at, dwell_seconds, ack_status, ack_user_id, ack_at, ack_remark) "
              + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, e.getFenceId());
            ps.setLong(2, e.getDeviceId());
            ps.setLong(3, e.getPatientId());
            ps.setString(4, e.getEventType() == null ? null : e.getEventType().name());
            ps.setObject(5, e.getLatitude());
            ps.setObject(6, e.getLongitude());
            ps.setTimestamp(7, e.getOccurredAt() == null ? null
                : new Timestamp(e.getOccurredAt().getTime()));
            ps.setObject(8, e.getDwellSeconds());
            ps.setString(9, e.getAckStatus() == null ? "NEW" : e.getAckStatus().name());
            ps.setObject(10, e.getAckUserId());
            ps.setTimestamp(11, e.getAckAt() == null ? null
                : new Timestamp(e.getAckAt().getTime()));
            ps.setString(12, e.getAckRemark());
            return ps;
        }, kh);
        return kh.getKey() == null ? 0L : kh.getKey().longValue();
    }

    public List<FenceEvent> findByFence(long fenceId, int limit) {
        return jdbc.query(
            "SELECT * FROM fence_events WHERE fence_id=? "
          + "ORDER BY occurred_at DESC LIMIT ?",
            MAPPER, fenceId, limit);
    }

    public List<FenceEvent> findActiveByPatient(long patientId, int limit) {
        return jdbc.query(
            "SELECT * FROM fence_events WHERE patient_id=? AND ack_status='NEW' "
          + "ORDER BY occurred_at DESC LIMIT ?",
            MAPPER, patientId, limit);
    }

    public List<FenceEvent> findActiveAll(int limit) {
        return jdbc.query(
            "SELECT * FROM fence_events WHERE ack_status='NEW' "
          + "ORDER BY occurred_at DESC LIMIT ?",
            MAPPER, limit);
    }

    public FenceEvent findLatestByFenceDevice(long fenceId, long deviceId) {
        List<FenceEvent> list = jdbc.query(
            "SELECT * FROM fence_events WHERE fence_id=? AND device_id=? "
          + "ORDER BY occurred_at DESC LIMIT 1",
            MAPPER, fenceId, deviceId);
        return list.isEmpty() ? null : list.get(0);
    }

    public int updateAck(long id, AckStatus status, Long userId,
                         Date ackAt, String remark) {
        return jdbc.update(
            "UPDATE fence_events SET ack_status=?, ack_user_id=?, ack_at=?, ack_remark=? "
          + "WHERE id=?",
            status == null ? null : status.name(), userId,
            ackAt == null ? null : new Timestamp(ackAt.getTime()),
            remark, id);
    }

    public List<FenceEvent> findRecentByDevice(long deviceId, int limit) {
        return jdbc.query(
            "SELECT * FROM fence_events WHERE device_id=? "
          + "ORDER BY occurred_at DESC LIMIT ?",
            MAPPER, deviceId, limit);
    }
}
