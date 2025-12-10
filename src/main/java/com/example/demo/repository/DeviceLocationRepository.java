package com.example.demo.repository;

import com.example.demo.model.DeviceLocation;
import com.example.demo.service.AmapLocationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class DeviceLocationRepository {
    private final JdbcTemplate jdbc;
    private final AmapLocationService amapLocationService;

    public DeviceLocationRepository(JdbcTemplate jdbc, AmapLocationService amapLocationService) { 
        this.jdbc = jdbc; 
        this.amapLocationService = amapLocationService;
    }

    // 使用 location_records 的列名：id, device_id, imei, recv_time, gps_raw, extra_raw, latitude, longitude, speed, direction, address, source
    private static final RowMapper<DeviceLocation> MAPPER = (rs, n) -> {
        DeviceLocation dl = new DeviceLocation();
        dl.setLocationId(rs.getLong("id"));
        dl.setDeviceId(rs.getLong("device_id"));
        dl.setImei(rs.getString("imei"));
        Timestamp t = rs.getTimestamp("recv_time");
        dl.setTime(t != null ? t.toLocalDateTime() : null);
        dl.setLatitude(rs.getBigDecimal("latitude"));
        dl.setLongitude(rs.getBigDecimal("longitude"));
        // location_records 不包含 accuracy/altitude/battery_level 字段，保持模型字段为 null
        dl.setAccuracy(null);
        dl.setAltitude(null);
        dl.setBatteryLevel(null);
        dl.setSource(rs.getString("source"));
        return dl;
    };

    public long insert(DeviceLocation dl) {
        // 获取地址信息
        final String address;
        if (dl.getLatitude() != null && dl.getLongitude() != null) {
            address = amapLocationService.regeoAddress(dl.getLatitude().doubleValue(), dl.getLongitude().doubleValue());
        } else {
            address = null;
        }
        
        String sql = "INSERT INTO location_records(device_id, imei, recv_time, gps_raw, extra_raw, latitude, longitude, speed, direction, address, source) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, dl.getDeviceId());
            ps.setString(2, dl.getImei());
            LocalDateTime time = dl.getTime() == null ? LocalDateTime.now() : dl.getTime();
            ps.setTimestamp(3, Timestamp.valueOf(time));
            ps.setObject(4, null); // gps_raw
            ps.setObject(5, null); // extra_raw
            ps.setBigDecimal(6, dl.getLatitude());
            ps.setBigDecimal(7, dl.getLongitude());
            ps.setObject(8, null); // speed
            ps.setObject(9, null); // direction
            ps.setString(10, address); // address
            ps.setString(11, dl.getSource());
            return ps;
        }, kh);
        return kh.getKey() == null ? 0L : kh.getKey().longValue();
    }

    public List<DeviceLocation> listRecent(long deviceId, int limit, int offset) {
        return jdbc.query("SELECT * FROM location_records WHERE device_id=? ORDER BY recv_time DESC LIMIT ? OFFSET ?", MAPPER, deviceId, limit, offset);
    }

    public List<DeviceLocation> listByRange(long deviceId, LocalDateTime start, LocalDateTime end, int limit, int offset) {
        return jdbc.query("SELECT * FROM location_records WHERE device_id=? AND recv_time BETWEEN ? AND ? ORDER BY recv_time DESC LIMIT ? OFFSET ?",
                MAPPER, deviceId, Timestamp.valueOf(start), Timestamp.valueOf(end), limit, offset);
    }
}
