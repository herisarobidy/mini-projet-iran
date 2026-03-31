package com.project.pure.dao;

import com.project.pure.db.Database;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDao {

    public record User(Long id, String username, String password, String role) {
    }

    private final DataSource dataSource;

    public UserDao(Database db) {
        this.dataSource = db.dataSource();
    }

    public User findByUsername(String username) {
        String sql = "SELECT id, username, password, role FROM users WHERE username = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Long id = rs.getLong("id");
                String u = rs.getString("username");
                String p = rs.getString("password");
                String r = rs.getString("role");
                return new User(id, u, p, r);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
