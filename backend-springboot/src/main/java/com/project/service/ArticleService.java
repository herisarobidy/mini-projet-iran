package com.project.service;

import com.project.model.Article;
import com.project.model.Category;
import com.project.repository.ArticleRepository;
import com.project.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final SlugService slugService;

    public ArticleService(ArticleRepository articleRepository,
                          CategoryRepository categoryRepository,
                          SlugService slugService) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.slugService = slugService;
    }

    public List<Article> listPublished() {
        return articleRepository.findAllByPublishedTrueOrderByCreatedAtDesc();
    }

    public List<Article> listAll() {
        return articleRepository.findAll();
    }

    public Article getById(Long id) {
        return articleRepository.findById(id).orElseThrow();
    }

    public Article getBySlug(String slug) {
        return articleRepository.findBySlug(slug).orElseThrow();
    }

    @Transactional
    public Article create(String title,
                          String content,
                          String author,
                          Long categoryId,
                          boolean published,
                          String image) {
        Article article = new Article();
        article.setTitle(title);
        article.setContent(content);
        article.setAuthor(author);
        article.setPublished(published);
        article.setImage(image);
        article.setCreatedAt(OffsetDateTime.now());

        Category category = categoryRepository.findById(categoryId).orElseThrow();
        article.setCategory(category);

        String baseSlug = slugService.slugify(title);
        article.setSlug(ensureUniqueSlug(baseSlug, null));

        return articleRepository.save(article);
    }

    @Transactional
    public Article update(Long id,
                          String title,
                          String content,
                          String author,
                          Long categoryId,
                          boolean published,
                          String image) {
        Article article = articleRepository.findById(id).orElseThrow();

        article.setTitle(title);
        article.setContent(content);
        article.setAuthor(author);
        article.setPublished(published);
        article.setImage(image);

        Category category = categoryRepository.findById(categoryId).orElseThrow();
        article.setCategory(category);

        String baseSlug = slugService.slugify(title);
        article.setSlug(ensureUniqueSlug(baseSlug, article.getId()));

        return articleRepository.save(article);
    }

    public void delete(Long id) {
        articleRepository.deleteById(id);
    }

    private String ensureUniqueSlug(String baseSlug, Long currentArticleId) {
        if (baseSlug == null || baseSlug.isBlank()) {
            baseSlug = "article";
        }

        String candidate = baseSlug;
        int i = 2;
        while (true) {
            var existing = articleRepository.findBySlug(candidate);
            if (existing.isEmpty()) {
                return candidate;
            }
            if (currentArticleId != null && existing.get().getId().equals(currentArticleId)) {
                return candidate;
            }
            candidate = baseSlug + "-" + i;
            i++;
        }
    }
}
