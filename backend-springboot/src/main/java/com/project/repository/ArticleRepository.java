package com.project.repository;

import com.project.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    @EntityGraph(attributePaths = "category")
    Optional<Article> findBySlug(String slug);

    boolean existsBySlug(String slug);

    long countByCategoryId(Long categoryId);

    List<Article> findAllByPublishedTrueOrderByCreatedAtDesc();

    @Override
    @EntityGraph(attributePaths = "category")
    Optional<Article> findById(Long id);
}
