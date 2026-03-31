package com.project.pure.dao;

import com.project.pure.db.Database;
import com.project.pure.util.SlugUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryDao {

    public record CategoryView(Long id, String name, String slug, String description) {
    }

    private final DataSource dataSource;

    public CategoryDao(Database db) {
        this.dataSource = db.dataSource();
    }

    public List<CategoryView> listAll() {
        String sql = "SELECT id, name, slug, description FROM categories ORDER BY id DESC";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<CategoryView> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new CategoryView(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("slug"),
                        rs.getString("description")
                ));
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public CategoryView findById(long id) {
        String sql = "SELECT id, name, slug, description FROM categories WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new CategoryView(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("slug"),
                        rs.getString("description")
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public long countArticlesUsing(long categoryId) {
        String sql = "SELECT COUNT(*) FROM articles WHERE category_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return 0;
                }
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public CategoryView create(String name, String description) {
        String base = SlugUtil.slugify(name);
        String slug = ensureUniqueSlug(base, null);

        String sql = "INSERT INTO categories(name, slug, description) VALUES(?, ?, ?) RETURNING id";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, slug);
            ps.setString(3, description);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                long id = rs.getLong(1);
                return new CategoryView(id, name, slug, description);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(long id, String name, String description) {
        String base = SlugUtil.slugify(name);
        String slug = ensureUniqueSlug(base, id);

        String sql = "UPDATE categories SET name = ?, slug = ?, description = ? WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, slug);
            ps.setString(3, description);
            ps.setLong(4, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(long id) {
        String sql = "DELETE FROM categories WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String ensureUniqueSlug(String baseSlug, Long currentId) {
        if (baseSlug == null || baseSlug.isBlank()) {
            baseSlug = "categorie";
        }

        String candidate = baseSlug;
        int i = 2;
        while (true) {
            CategoryView existing = findBySlug(candidate);
            if (existing == null) {
                return candidate;
            }
            if (currentId != null && existing.id().equals(currentId)) {
                return candidate;
            }
            candidate = baseSlug + "-" + i;
            i++;
        }
    }

    private CategoryView findBySlug(String slug) {
        String sql = "SELECT id, name, slug, description FROM categories WHERE slug = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new CategoryView(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("slug"),
                        rs.getString("description")
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
