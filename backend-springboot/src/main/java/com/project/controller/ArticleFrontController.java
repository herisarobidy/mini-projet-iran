package com.project.controller;

import com.project.model.Article;
import com.project.service.ArticleService;
import com.project.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ArticleFrontController {

    private final ArticleService articleService;
    private final CommentService commentService;

    public ArticleFrontController(ArticleService articleService, CommentService commentService) {
        this.articleService = articleService;
        this.commentService = commentService;
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
        model.addAttribute("comments", commentService.listApprovedForArticle(article.getId()));
        return "articles/detail";
    }

    @PostMapping("/articles/{slug}/comments")
    public String createComment(@PathVariable String slug,
                                @RequestParam(name = "author", required = false) String author,
                                @RequestParam(name = "content", required = false) String content) {
        Article article;
        try {
            article = articleService.getBySlug(slug);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if (!article.isPublished()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if (author == null || author.isBlank() || content == null || content.isBlank()) {
            return "redirect:/articles/" + slug + "?comment=error";
        }

        commentService.create(article.getId(), author.trim(), content.trim());
        return "redirect:/articles/" + slug + "?comment=sent";
    }
}
