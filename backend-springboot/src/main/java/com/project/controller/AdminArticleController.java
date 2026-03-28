package com.project.controller;

import com.project.model.Article;
import com.project.model.Category;
import com.project.service.ArticleService;
import com.project.service.CategoryService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class AdminArticleController {

    private final ArticleService articleService;
    private final CategoryService categoryService;

    public AdminArticleController(ArticleService articleService, CategoryService categoryService) {
        this.articleService = articleService;
        this.categoryService = categoryService;
    }

    @GetMapping("/admin/articles")
    public String list(Model model) {
        model.addAttribute("articles", articleService.listAll());
        return "admin/articles/list";
    }

    @GetMapping("/admin/articles/new")
    public String createForm(Model model) {
        model.addAttribute("form", new ArticleForm());
        model.addAttribute("categories", categoryService.listAll());
        return "admin/articles/form";
    }

    @PostMapping("/admin/articles")
    public String create(@ModelAttribute("form") ArticleForm form, BindingResult bindingResult, Model model) {
        List<Category> categories = categoryService.listAll();
        if (form.getTitle() == null || form.getTitle().isBlank()) {
            bindingResult.rejectValue("title", "required", "Required");
        }
        if (form.getContent() == null || form.getContent().isBlank()) {
            bindingResult.rejectValue("content", "required", "Required");
        }
        if (form.getAuthor() == null || form.getAuthor().isBlank()) {
            bindingResult.rejectValue("author", "required", "Required");
        }
        if (form.getCategoryId() == null) {
            bindingResult.rejectValue("categoryId", "required", "Required");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categories);
            return "admin/articles/form";
        }

        articleService.create(form.getTitle(), form.getContent(), form.getAuthor(), form.getCategoryId(), form.isPublished(), form.getImage());
        return "redirect:/admin/articles";
    }

    @GetMapping("/admin/articles/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Article article = articleService.getById(id);
        ArticleForm form = ArticleForm.from(article);
        model.addAttribute("form", form);
        model.addAttribute("articleId", id);
        model.addAttribute("categories", categoryService.listAll());
        return "admin/articles/form";
    }

    @PostMapping("/admin/articles/{id}")
    public String update(@PathVariable Long id, @ModelAttribute("form") ArticleForm form, BindingResult bindingResult, Model model) {
        if (form.getTitle() == null || form.getTitle().isBlank()) {
            bindingResult.rejectValue("title", "required", "Required");
        }
        if (form.getContent() == null || form.getContent().isBlank()) {
            bindingResult.rejectValue("content", "required", "Required");
        }
        if (form.getAuthor() == null || form.getAuthor().isBlank()) {
            bindingResult.rejectValue("author", "required", "Required");
        }
        if (form.getCategoryId() == null) {
            bindingResult.rejectValue("categoryId", "required", "Required");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("articleId", id);
            model.addAttribute("categories", categoryService.listAll());
            return "admin/articles/form";
        }

        articleService.update(id, form.getTitle(), form.getContent(), form.getAuthor(), form.getCategoryId(), form.isPublished(), form.getImage());
        return "redirect:/admin/articles";
    }

    @PostMapping("/admin/articles/{id}/delete")
    public String delete(@PathVariable Long id) {
        articleService.delete(id);
        return "redirect:/admin/articles";
    }

    public static class ArticleForm {
        @NotBlank
        private String title;

        @NotBlank
        private String content;

        @NotBlank
        private String author;

        private String image;

        @NotNull
        private Long categoryId;

        private boolean published;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }

        public boolean isPublished() {
            return published;
        }

        public void setPublished(boolean published) {
            this.published = published;
        }

        public static ArticleForm from(Article article) {
            ArticleForm form = new ArticleForm();
            form.setTitle(article.getTitle());
            form.setContent(article.getContent());
            form.setAuthor(article.getAuthor());
            form.setImage(article.getImage());
            form.setCategoryId(article.getCategory() != null ? article.getCategory().getId() : null);
            form.setPublished(article.isPublished());
            return form;
        }
    }
}
