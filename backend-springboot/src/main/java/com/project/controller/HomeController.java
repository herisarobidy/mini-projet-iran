package com.project.controller;

import com.project.model.Article;
import com.project.service.ArticleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private final ArticleService articleService;

    public HomeController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/")
    public String home(@RequestParam(name = "page", defaultValue = "0") int page,
                       Model model) {
        int size = 5;
        int safePage = Math.max(0, page);
        Pageable pageable = PageRequest.of(safePage, size);
        Page<Article> articlesPage = articleService.listPublished(pageable);

        model.addAttribute("articles", articlesPage.getContent());
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", articlesPage.getTotalPages());
        model.addAttribute("hasPrev", articlesPage.hasPrevious());
        model.addAttribute("hasNext", articlesPage.hasNext());
        return "index";
    }
}
