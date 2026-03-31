package com.project.pure.dao;

import com.project.pure.db.Database;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommentDao {

    public record CommentView(Long id, Long articleId, String author, String content, OffsetDateTime createdAt) {
    }

    public record ArticleRef(String title, String slug) {
    }

    public record ModerationComment(Long id, ArticleRef article, String author, String content, OffsetDateTime createdAt) {
    }

    private final DataSource dataSource;

    public CommentDao(Database db) {
        this.dataSource = db.dataSource();
    }

    public List<CommentView> listApprovedForArticle(long articleId) {
        String sql = "SELECT id, article_id, author, content, created_at " +
                "FROM comments " +
                "WHERE article_id = ? AND approved = TRUE " +
                "ORDER BY created_at ASC";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, articleId);

            try (ResultSet rs = ps.executeQuery()) {
                List<CommentView> out = new ArrayList<>();
                while (rs.next()) {
                    Long id = rs.getLong("id");
                    Long aid = rs.getLong("article_id");
                    String author = rs.getString("author");
                    String content = rs.getString("content");
                    OffsetDateTime createdAt = null;
                    try {
                        createdAt = rs.getObject("created_at", OffsetDateTime.class);
                    } catch (SQLException ignored) {
                    }
                    if (createdAt == null) {
                        Object raw = rs.getObject("created_at");
                        if (raw instanceof OffsetDateTime odt) {
                            createdAt = odt;
                        }
                    }
                    out.add(new CommentView(id, aid, author, content, createdAt));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void create(long articleId, String author, String content) {
        String sql = "INSERT INTO comments(article_id, author, content, approved) VALUES(?, ?, ?, FALSE)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, articleId);
            ps.setString(2, author);
            ps.setString(3, content);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public java.util.List<ModerationComment> listPending() {
        return listForModeration(false);
    }

    public java.util.List<ModerationComment> listApproved() {
        return listForModeration(true);
    }

    private java.util.List<ModerationComment> listForModeration(boolean approved) {
        String sql = "SELECT c.id, c.author, c.content, c.created_at, a.title AS article_title, a.slug AS article_slug " +
                "FROM comments c " +
                "JOIN articles a ON a.id = c.article_id " +
                "WHERE c.approved = ? " +
                "ORDER BY c.created_at DESC";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, approved);

            try (ResultSet rs = ps.executeQuery()) {
                java.util.List<ModerationComment> out = new java.util.ArrayList<>();
                while (rs.next()) {
                    Long id = rs.getLong("id");
                    String author = rs.getString("author");
                    String content = rs.getString("content");
                    OffsetDateTime createdAt = null;
                    try {
                        createdAt = rs.getObject("created_at", OffsetDateTime.class);
                    } catch (SQLException ignored) {
                    }
                    String at = rs.getString("article_title");
                    String as = rs.getString("article_slug");
                    out.add(new ModerationComment(id, new ArticleRef(at, as), author, content, createdAt));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void approve(long id) {
        String sql = "UPDATE comments SET approved = TRUE WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(long id) {
        String sql = "DELETE FROM comments WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
