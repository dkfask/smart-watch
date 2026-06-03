package com.example.demo.repository;

import com.example.demo.model.DeviceStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public class DeviceStatusRepository {
    private final JdbcTemplate jdbc;

    public DeviceStatusRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<DeviceStatus> MAPPER = (rs, n) -> {
        DeviceStatus s = new DeviceStatus();
        s.setDeviceId(rs.getLong("device_id"));
        var t = rs.getTimestamp("last_location_time");
        s.setLastLocationTime(t != null ? new Date(t.getTime()) : null);
        s.setLastLatitude(rs.getBigDecimal("last_latitude") != null ? rs.getBigDecimal("last_latitude").doubleValue() : null);
        s.setLastLongitude(rs.getBigDecimal("last_longitude") != null ? rs.getBigDecimal("last_longitude").doubleValue() : null);
        Object bat = rs.getObject("battery_level");
        s.setBatteryLevel(bat == null ? null : rs.getInt("battery_level"));
        Object on = rs.getObject("is_online");
        s.setIsOnline(on == null ? null : rs.getBoolean("is_online"));
        var up = rs.getTimestamp("updated_at");
        s.setUpdatedAt(up != null ? new Date(up.getTime()) : null);
        // 新增：读取 imei 字段以便回显
        s.setImei(rs.getString("imei"));
        return s;
    };

    public int upsert(DeviceStatus s) {
        // PRIMARY KEY(device_id)
        return jdbc.update("INSERT INTO device_status(device_id,last_location_time,last_latitude,last_longitude,battery_level,is_online,updated_at,imei) " +
                        "VALUES(?,?,?,?,?,?,CURRENT_TIMESTAMP,?) " +
                        "ON DUPLICATE KEY UPDATE last_location_time=COALESCE(VALUES(last_location_time), last_location_time), last_latitude=COALESCE(VALUES(last_latitude), last_latitude), last_longitude=COALESCE(VALUES(last_longitude), last_longitude), battery_level=COALESCE(VALUES(battery_level), battery_level), is_online=COALESCE(VALUES(is_online), is_online), updated_at=CURRENT_TIMESTAMP, imei=COALESCE(VALUES(imei), imei)",
                s.getDeviceId(),
                s.getLastLocationTime() == null ? null : new Timestamp(s.getLastLocationTime().getTime()),
                s.getLastLatitude(), s.getLastLongitude(), s.getBatteryLevel(), s.getIsOnline(), s.getImei());
    }

    public Optional<DeviceStatus> findById(long deviceId) {
        List<DeviceStatus> list = jdbc.query("SELECT * FROM device_status WHERE device_id= ?", MAPPER, deviceId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public int setOnline(long deviceId, boolean online) {
        return jdbc.update("UPDATE device_status SET is_online=?, updated_at=CURRENT_TIMESTAMP WHERE device_id=?", online, deviceId);
    }

    public int touch(long deviceId) {
        return jdbc.update("UPDATE device_status SET updated_at=CURRENT_TIMESTAMP WHERE device_id=?", deviceId);
    }

    public List<DeviceStatus> findAll() {
        return jdbc.query("SELECT * FROM device_status", MAPPER);
    }
    
    /**
     * 根据设备ID列表查询设备状态
     * @param deviceIds 设备ID列表
     * @return 设备状态列表
     */
    public List<DeviceStatus> findAllById(List<Long> deviceIds) {
        if (deviceIds.isEmpty()) {
            return List.of();
        }
        // 构建IN查询语句
        String placeholders = String.join(",", deviceIds.stream().map(id -> "?").toArray(String[]::new));
        String sql = "SELECT * FROM device_status WHERE device_id IN (" + placeholders + ")";
        return jdbc.query(sql, MAPPER, deviceIds.toArray());
    }
}
