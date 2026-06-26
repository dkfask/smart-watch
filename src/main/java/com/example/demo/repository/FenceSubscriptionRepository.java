package com.example.demo.repository;

import com.example.demo.model.FenceSubscription;
import com.example.demo.model.FenceSubscription.ScopeType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class FenceSubscriptionRepository {

    private final JdbcTemplate jdbc;

    public FenceSubscriptionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<FenceSubscription> MAPPER = (rs, n) -> {
        FenceSubscription s = new FenceSubscription();
        s.setId(rs.getLong("id"));
        s.setUserId(rs.getLong("user_id"));
        String sc = rs.getString("scope_type");
        if (sc != null) { try { s.setScopeType(ScopeType.valueOf(sc)); } catch (IllegalArgumentException ignored) {} }
        s.setScopeId((Long) rs.getObject("scope_id"));
        s.setWsSessionId(rs.getString("ws_session_id"));
        return s;
    };

    public long insert(FenceSubscription s) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT IGNORE INTO fence_subscriptions "
              + "(user_id, scope_type, scope_id, ws_session_id) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, s.getUserId());
            ps.setString(2, s.getScopeType() == null ? null : s.getScopeType().name());
            ps.setObject(3, s.getScopeId());
            ps.setString(4, s.getWsSessionId());
            return ps;
        }, kh);
        return kh.getKey() == null ? 0L : kh.getKey().longValue();
    }

    public int delete(long userId, ScopeType scope, Long scopeId, String wsSessionId) {
        return jdbc.update(
            "DELETE FROM fence_subscriptions "
          + "WHERE user_id=? AND scope_type=? AND "
          + "      ((scope_id <=> ?) OR (scope_id IS NULL AND ? IS NULL)) "
          + "      AND ws_session_id=?",
            userId, scope == null ? null : scope.name(),
            scopeId, scopeId, wsSessionId);
    }

    public int deleteBySession(String wsSessionId) {
        return jdbc.update(
            "DELETE FROM fence_subscriptions WHERE ws_session_id=?",
            wsSessionId);
    }

    /**
     * Find sessions that should receive a fence event for the given
     * (fenceId, patientId). Sessions match if they subscribed to the fence,
     * the patient, or ALL events.
     */
    public List<String> findWsSessionsForFenceEvent(long fenceId, long patientId) {
        return jdbc.query(
            "SELECT DISTINCT ws_session_id FROM fence_subscriptions "
          + "WHERE (scope_type='FENCE'   AND scope_id=?) "
          + "   OR (scope_type='PATIENT' AND scope_id=?) "
          + "   OR (scope_type='ALL')",
            (rs, n) -> rs.getString(1),
            fenceId, patientId);
    }

    public List<FenceSubscription> findBySession(String wsSessionId) {
        return jdbc.query(
            "SELECT * FROM fence_subscriptions WHERE ws_session_id=?",
            MAPPER, wsSessionId);
    }
}
