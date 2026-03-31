package com.project.pure.http;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class QueryParams {

    private QueryParams() {}

    public static Map<String, String> parse(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return Collections.emptyMap();
        }

        Map<String, String> out = new HashMap<>();
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            if (pair.isBlank()) {
                continue;
            }
            int idx = pair.indexOf('=');
            String k = idx >= 0 ? pair.substring(0, idx) : pair;
            String v = idx >= 0 ? pair.substring(idx + 1) : "";
            k = URLDecoder.decode(k, StandardCharsets.UTF_8);
            v = URLDecoder.decode(v, StandardCharsets.UTF_8);
            out.put(k, v);
        }
        return out;
    }

    public static int getInt(Map<String, String> query, String key, int defaultValue) {
        if (query == null) {
            return defaultValue;
        }
        String v = query.get(key);
        if (v == null || v.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
