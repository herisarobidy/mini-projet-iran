package com.project.repository;

import com.project.model.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"article"})
    List<Comment> findAllByApprovedFalseOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"article"})
    List<Comment> findAllByApprovedTrueOrderByCreatedAtDesc();

    List<Comment> findAllByArticleIdAndApprovedTrueOrderByCreatedAtAsc(Long articleId);
}
