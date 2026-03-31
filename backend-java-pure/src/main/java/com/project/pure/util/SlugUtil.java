package com.project.pure.util;

import java.text.Normalizer;

public final class SlugUtil {

    private SlugUtil() {}

    public static String slugify(String input) {
        if (input == null) {
            return "";
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        return normalized
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }
}
