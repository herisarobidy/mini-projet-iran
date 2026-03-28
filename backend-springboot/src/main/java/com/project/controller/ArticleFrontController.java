package com.project.controller;

import com.project.model.Article;
import com.project.service.ArticleService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ArticleFrontController {

    private final ArticleService articleService;

    public ArticleFrontController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/articles/{slug}")
    public String article(@PathVariable String slug, Model model) {
        Article article;
        try {
            article = articleService.getBySlug(slug);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if (!article.isPublished()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        model.addAttribute("article", article);
        return "articles/detail";
    }
}
