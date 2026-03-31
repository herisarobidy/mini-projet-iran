package com.project.pure.dao;

import com.project.pure.db.Database;
import com.project.pure.util.SlugUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

public class ArticleDao {

    private static final DateTimeFormatter TS_WITH_OPTIONAL_FRACTION = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .toFormatter();

    public record ArticleSummary(Long id, String title, String slug, String image, String author, OffsetDateTime createdAt) {
    }

    public record SitemapUrl(String slug, OffsetDateTime lastModified) {
    }

    public List<ArticleAdminRow> listAll() {
        String sql = "SELECT id, title, slug, published FROM articles ORDER BY id DESC";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<ArticleAdminRow> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new ArticleAdminRow(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("slug"),
                        rs.getBoolean("published")
                ));
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ArticleDetail findById(long id) {
        String sql = "SELECT id, title, slug, content, image, author, created_at, published, category_id " +
                "FROM articles WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Long rid = rs.getLong("id");
                String title = rs.getString("title");
                String slug = rs.getString("slug");
                String content = rs.getString("content");
                String image = rs.getString("image");
                String author = rs.getString("author");
                OffsetDateTime createdAt = readOffsetDateTime(rs, "created_at");
                boolean published = rs.getBoolean("published");
                long categoryId = rs.getLong("category_id");
                return new ArticleDetail(rid, title, slug, content, image, author, createdAt, published, categoryId);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public long create(String title,
                       String content,
                       String author,
                       long categoryId,
                       boolean published,
                       String image) {

        String base = SlugUtil.slugify(title);
        String slug = ensureUniqueSlug(base, null);

        String sql = "INSERT INTO articles(title, slug, content, image, author, category_id, published) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, slug);
            ps.setString(3, content);
            ps.setString(4, image);
            ps.setString(5, author);
            ps.setLong(6, categoryId);
            ps.setBoolean(7, published);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(long id,
                       String title,
                       String content,
                       String author,
                       long categoryId,
                       boolean published,
                       String image) {

        String base = SlugUtil.slugify(title);
        String slug = ensureUniqueSlug(base, id);

        String sql = "UPDATE articles SET title=?, slug=?, content=?, image=?, author=?, category_id=?, published=? WHERE id=?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, slug);
            ps.setString(3, content);
            ps.setString(4, image);
            ps.setString(5, author);
            ps.setLong(6, categoryId);
            ps.setBoolean(7, published);
            ps.setLong(8, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(long id) {
        String sql = "DELETE FROM articles WHERE id = ?";
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
            baseSlug = "article";
        }

        String candidate = baseSlug;
        int i = 2;
        while (true) {
            ArticleDetail existing = findBySlug(candidate);
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

    public record ArticleDetail(Long id, String title, String slug, String content, String image, String author, OffsetDateTime createdAt, boolean published, Long categoryId) {
    }

    public record ArticleAdminRow(Long id, String title, String slug, boolean published) {
    }

    private final DataSource dataSource;

    public ArticleDao(Database db) {
        this.dataSource = db.dataSource();
    }

    public long countPublished() {
        String sql = "SELECT COUNT(*) FROM articles WHERE published = TRUE";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return 0;
            }
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<SitemapUrl> listPublishedForSitemap(int limit) {
        int safeLimit = Math.max(1, limit);
        String sql = "SELECT slug, created_at FROM articles WHERE published = TRUE ORDER BY created_at DESC LIMIT ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, safeLimit);
            try (ResultSet rs = ps.executeQuery()) {
                List<SitemapUrl> out = new ArrayList<>();
                while (rs.next()) {
                    String slug = rs.getString("slug");
                    OffsetDateTime createdAt = readOffsetDateTime(rs, "created_at");
                    out.add(new SitemapUrl(slug, createdAt));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ArticleSummary> listPublishedPage(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);
        int offset = safePage * safeSize;

        String sql = "SELECT id, title, slug, image, author, created_at " +
                "FROM articles " +
                "WHERE published = TRUE " +
                "ORDER BY created_at DESC " +
                "LIMIT ? OFFSET ?";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, safeSize);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                List<ArticleSummary> out = new ArrayList<>();
                while (rs.next()) {
                    Long id = rs.getLong("id");
                    String title = rs.getString("title");
                    String slug = rs.getString("slug");
                    String image = rs.getString("image");
                    String author = rs.getString("author");
                    OffsetDateTime createdAt = readOffsetDateTime(rs, "created_at");
                    out.add(new ArticleSummary(id, title, slug, image, author, createdAt));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ArticleDetail findBySlug(String slug) {
        String sql = "SELECT id, title, slug, content, image, author, created_at, published, category_id " +
                "FROM articles WHERE slug = ?";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, slug);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                Long id = rs.getLong("id");
                String title = rs.getString("title");
                String s = rs.getString("slug");
                String content = rs.getString("content");
                String image = rs.getString("image");
                String author = rs.getString("author");
                OffsetDateTime createdAt = readOffsetDateTime(rs, "created_at");
                boolean published = rs.getBoolean("published");
                long categoryId = rs.getLong("category_id");

                return new ArticleDetail(id, title, s, content, image, author, createdAt, published, categoryId);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static OffsetDateTime readOffsetDateTime(ResultSet rs, String column) throws SQLException {
        try {
            OffsetDateTime odt = rs.getObject(column, OffsetDateTime.class);
            if (odt != null) {
                return odt;
            }
        } catch (SQLException ignored) {
        }

        Object raw = rs.getObject(column);
        if (raw == null) {
            return null;
        }

        if (raw instanceof OffsetDateTime odt) {
            return odt;
        }

        if (raw instanceof LocalDateTime ldt) {
            ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(ldt);
            return ldt.atOffset(offset);
        }

        String s = raw.toString().trim();
        if (s.isEmpty()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(s);
        } catch (Exception ignored) {
        }

        LocalDateTime ldt = LocalDateTime.parse(s, TS_WITH_OPTIONAL_FRACTION);
        ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(ldt);
        return ldt.atOffset(offset);
    }
}
