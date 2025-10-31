package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Service
public class MySqlService {
    private static final Logger log = LoggerFactory.getLogger(MySqlService.class);

    private final DataSource dataSource;

    public MySqlService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 测试数据库连接是否可用（执行 SELECT 1）
     */
    public boolean testConnection() {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1")) {
            if (rs.next()) {
                int v = rs.getInt(1);
                boolean ok = v == 1;
                log.info("DB testConnection => {}", ok);
                return ok;
            }
            return false;
        } catch (Exception e) {
            log.warn("Database test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 运行任意查询并返回结果列表（每行为 Map）
     */
    public List<Map<String, Object>> query(String sql, Object... args) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    ps.setObject(i + 1, args[i]);
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        row.put(md.getColumnLabel(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
            }

            return rows;
        } catch (SQLException e) {
            log.warn("Query failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
