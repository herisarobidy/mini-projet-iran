package com.project.service;

import com.project.model.Article;
import com.project.model.Comment;
import com.project.repository.ArticleRepository;
import com.project.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;

    public CommentService(CommentRepository commentRepository,
                          ArticleRepository articleRepository) {
        this.commentRepository = commentRepository;
        this.articleRepository = articleRepository;
    }

    @Transactional
    public Comment create(Long articleId, String author, String content) {
        Article article = articleRepository.findById(articleId).orElseThrow();

        Comment comment = new Comment();
        comment.setArticle(article);
        comment.setAuthor(author);
        comment.setContent(content);
        comment.setCreatedAt(OffsetDateTime.now());
        comment.setApproved(false);

        return commentRepository.save(comment);
    }

    public List<Comment> listApprovedForArticle(Long articleId) {
        return commentRepository.findAllByArticleIdAndApprovedTrueOrderByCreatedAtAsc(articleId);
    }

    public List<Comment> listPending() {
        return commentRepository.findAllByApprovedFalseOrderByCreatedAtDesc();
    }

    public List<Comment> listApproved() {
        return commentRepository.findAllByApprovedTrueOrderByCreatedAtDesc();
    }

    @Transactional
    public void approve(Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        comment.setApproved(true);
        commentRepository.save(comment);
    }

    public void delete(Long commentId) {
        commentRepository.deleteById(commentId);
    }
}
