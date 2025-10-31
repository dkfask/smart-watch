package com.example.demo.repository;

import com.example.demo.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<User> MAPPER = (rs, n) -> {
        User u = new User();
        u.setUserId(rs.getLong("user_id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPassword_hash(rs.getString("password_hash"));
        u.setPhone_number(rs.getString("phone_number"));
        u.setAvatar_url(rs.getString("avatar_url"));
        Timestamp c = rs.getTimestamp("created_at");
        Timestamp up = rs.getTimestamp("updated_at");
        u.setCreated_at(c != null ? c.toLocalDateTime() : null);
        u.setUpdated_at(up != null ? up.toLocalDateTime() : null);
        return u;
    };

    public long create(User u) {
        String sql = "INSERT INTO users(username,email,password_hash,phone_number,avatar_url) VALUES(?,?,?,?,?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPassword_hash());
            ps.setString(4, u.getPhone_number());
            ps.setString(5, u.getAvatar_url());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? 0L : key.longValue();
    }

    public Optional<User> findById(long id) {
        List<User> list = jdbc.query("SELECT * FROM users WHERE user_id=?", MAPPER, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<User> findByUsername(String username) {
        List<User> list = jdbc.query("SELECT * FROM users WHERE username=?", MAPPER, username);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<User> list(int limit, int offset) {
        return jdbc.query("SELECT * FROM users ORDER BY user_id DESC LIMIT ? OFFSET ?", MAPPER, limit, offset);
    }

    public int update(User u) {
        return jdbc.update("UPDATE users SET email=?, password_hash=?, phone_number=?, avatar_url=? WHERE user_id=?",
                u.getEmail(), u.getPassword_hash(), u.getPhone_number(), u.getAvatar_url(), u.getUserId());
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM users WHERE user_id=?", id);
    }
}

