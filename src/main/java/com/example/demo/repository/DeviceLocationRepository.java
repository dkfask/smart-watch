package com.example.demo.repository;

import com.example.demo.model.DeviceLocation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class DeviceLocationRepository {
    private final JdbcTemplate jdbc;

    public DeviceLocationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<DeviceLocation> MAPPER = (rs, n) -> {
        DeviceLocation dl = new DeviceLocation();
        dl.setLocationId(rs.getLong("location_id"));
        dl.setDeviceId(rs.getLong("device_id"));
        Timestamp t = rs.getTimestamp("time");
        dl.setTime(t != null ? t.toLocalDateTime() : null);
        dl.setLatitude(rs.getBigDecimal("latitude"));
        dl.setLongitude(rs.getBigDecimal("longitude"));
        Object acc = rs.getObject("accuracy");
        dl.setAccuracy(acc == null ? null : rs.getInt("accuracy"));
        dl.setAltitude(rs.getBigDecimal("altitude"));
        Object bat = rs.getObject("battery_level");
        dl.setBatteryLevel(bat == null ? null : rs.getInt("battery_level"));
        dl.setSource(rs.getString("source"));
        return dl;
    };

    public long insert(DeviceLocation dl) {
        String sql = "INSERT INTO device_locations(device_id, time, latitude, longitude, accuracy, altitude, battery_level, source) VALUES(?,?,?,?,?,?,?,?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, dl.getDeviceId());
            LocalDateTime time = dl.getTime() == null ? LocalDateTime.now() : dl.getTime();
            ps.setTimestamp(2, Timestamp.valueOf(time));
            ps.setBigDecimal(3, dl.getLatitude());
            ps.setBigDecimal(4, dl.getLongitude());
            if (dl.getAccuracy() == null) ps.setObject(5, null); else ps.setInt(5, dl.getAccuracy());
            ps.setBigDecimal(6, dl.getAltitude());
            if (dl.getBatteryLevel() == null) ps.setObject(7, null); else ps.setInt(7, dl.getBatteryLevel());
            ps.setString(8, dl.getSource());
            return ps;
        }, kh);
        return kh.getKey() == null ? 0L : kh.getKey().longValue();
    }

    public List<DeviceLocation> listRecent(long deviceId, int limit, int offset) {
        return jdbc.query("SELECT * FROM device_locations WHERE device_id=? ORDER BY time DESC LIMIT ? OFFSET ?", MAPPER, deviceId, limit, offset);
    }

    public List<DeviceLocation> listByRange(long deviceId, LocalDateTime start, LocalDateTime end, int limit, int offset) {
        return jdbc.query("SELECT * FROM device_locations WHERE device_id=? AND time BETWEEN ? AND ? ORDER BY time DESC LIMIT ? OFFSET ?",
                MAPPER, deviceId, Timestamp.valueOf(start), Timestamp.valueOf(end), limit, offset);
    }
}

