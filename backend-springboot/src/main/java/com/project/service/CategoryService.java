package com.project.service;

import com.project.model.Category;
import com.project.repository.ArticleRepository;
import com.project.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;
    private final SlugService slugService;

    public CategoryService(CategoryRepository categoryRepository,
                            ArticleRepository articleRepository,
                            SlugService slugService) {
        this.categoryRepository = categoryRepository;
        this.articleRepository = articleRepository;
        this.slugService = slugService;
    }

    public List<Category> listAll() {
        return categoryRepository.findAll();
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id).orElseThrow();
    }

    @Transactional
    public Category create(String name, String description) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);

        String baseSlug = slugService.slugify(name);
        category.setSlug(ensureUniqueSlug(baseSlug, null));

        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, String name, String description) {
        Category category = categoryRepository.findById(id).orElseThrow();
        category.setName(name);
        category.setDescription(description);

        String baseSlug = slugService.slugify(name);
        category.setSlug(ensureUniqueSlug(baseSlug, category.getId()));

        return categoryRepository.save(category);
    }

    public boolean canDelete(Long id) {
        return articleRepository.countByCategoryId(id) == 0;
    }

    public void delete(Long id) {
        if (!canDelete(id)) {
            throw new IllegalStateException("CATEGORY_IN_USE");
        }
        categoryRepository.deleteById(id);
    }

    private String ensureUniqueSlug(String baseSlug, Long currentCategoryId) {
        if (baseSlug == null || baseSlug.isBlank()) {
            baseSlug = "categorie";
        }

        String candidate = baseSlug;
        int i = 2;
        while (true) {
            var existing = categoryRepository.findBySlug(candidate);
            if (existing.isEmpty()) {
                return candidate;
            }
            if (currentCategoryId != null && existing.get().getId().equals(currentCategoryId)) {
                return candidate;
            }
            candidate = baseSlug + "-" + i;
            i++;
        }
    }
}
