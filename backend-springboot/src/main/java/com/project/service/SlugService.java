package com.project.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;

@Service
public class SlugService {

    public String slugify(String input) {
        if (input == null) {
            return "";
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        String slug = normalized
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");

        return slug;
    }
}
