package com.project.controller;

import com.project.service.ArticleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class SitemapController {

    private final ArticleService articleService;

    public SitemapController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap(HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName();
        if (!((request.getScheme().equals("http") && request.getServerPort() == 80)
                || (request.getScheme().equals("https") && request.getServerPort() == 443))) {
            baseUrl += ":" + request.getServerPort();
        }

        var articles = articleService.listPublished();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");

        appendUrl(xml, baseUrl + "/", null);

        for (var a : articles) {
            OffsetDateTime lastMod = a.getCreatedAt();
            appendUrl(xml, baseUrl + "/articles/" + escapeXml(a.getSlug()), lastMod);
        }

        xml.append("</urlset>");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(xml.toString());
    }

    private void appendUrl(StringBuilder xml, String loc, OffsetDateTime lastMod) {
        xml.append("<url>");
        xml.append("<loc>").append(escapeXml(loc)).append("</loc>");
        if (lastMod != null) {
            xml.append("<lastmod>")
                    .append(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(lastMod))
                    .append("</lastmod>");
        }
        xml.append("</url>");
    }

    private String escapeXml(String s) {
        if (s == null) {
            return "";
        }
        return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
