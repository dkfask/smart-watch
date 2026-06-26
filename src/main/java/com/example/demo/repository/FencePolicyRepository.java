package com.example.demo.repository;

import com.example.demo.model.FencePolicy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FencePolicyRepository {

    private final JdbcTemplate jdbc;

    public FencePolicyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<FencePolicy> MAPPER = (rs, n) -> {
        FencePolicy p = new FencePolicy();
        p.setFenceId(rs.getLong("fence_id"));
        p.setEnterAlert(rs.getBoolean("enter_alert"));
        p.setExitAlert(rs.getBoolean("exit_alert"));
        p.setDwellAlert(rs.getBoolean("dwell_alert"));
        p.setDwellSeconds(rs.getInt("dwell_seconds"));
        p.setDebounceCount(rs.getInt("debounce_count"));
        p.setCooldownSec(rs.getInt("cooldown_sec"));
        return p;
    };

    public FencePolicy findByFenceId(long fenceId) {
        List<FencePolicy> list = jdbc.query(
            "SELECT * FROM fence_policies WHERE fence_id=?",
            MAPPER, fenceId);
        return list.isEmpty() ? null : list.get(0);
    }

    public FencePolicy findGlobalDefault() {
        return findByFenceId(FencePolicy.GLOBAL_DEFAULT_FENCE_ID);
    }

    /**
     * Effective policy for a fence: prefer per-fence row, fall back to global default.
     * Always returns a non-null object.
     */
    public FencePolicy effectivePolicyFor(long fenceId) {
        FencePolicy p = findByFenceId(fenceId);
        if (p != null) return p;
        return findGlobalDefault();
    }

    public int upsert(FencePolicy p) {
        return jdbc.update(
            "INSERT INTO fence_policies "
          + "(fence_id, enter_alert, exit_alert, dwell_alert, "
          + " dwell_seconds, debounce_count, cooldown_sec) "
          + "VALUES (?,?,?,?,?,?,?) "
          + "ON DUPLICATE KEY UPDATE "
          + "  enter_alert=VALUES(enter_alert), "
          + "  exit_alert=VALUES(exit_alert), "
          + "  dwell_alert=VALUES(dwell_alert), "
          + "  dwell_seconds=VALUES(dwell_seconds), "
          + "  debounce_count=VALUES(debounce_count), "
          + "  cooldown_sec=VALUES(cooldown_sec)",
            p.getFenceId(),
            p.getEnterAlert(), p.getExitAlert(), p.getDwellAlert(),
            p.getDwellSeconds(), p.getDebounceCount(), p.getCooldownSec());
    }

    public int deleteByFenceId(long fenceId) {
        return jdbc.update("DELETE FROM fence_policies WHERE fence_id=?", fenceId);
    }
}
