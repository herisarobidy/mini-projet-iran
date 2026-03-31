package com.project.pure.http;

import com.sun.net.httpserver.HttpExchange;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class Cookies {

    private Cookies() {}

    public static String get(HttpExchange exchange, String name) {
        List<String> values = exchange.getRequestHeaders().get("Cookie");
        if (values == null || values.isEmpty()) {
            return null;
        }

        for (String header : values) {
            if (header == null || header.isBlank()) {
                continue;
            }
            String[] parts = header.split(";");
            for (String part : parts) {
                String p = part.trim();
                int idx = p.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String k = p.substring(0, idx).trim();
                String v = p.substring(idx + 1).trim();
                if (name.equals(k)) {
                    return URLDecoder.decode(v, StandardCharsets.UTF_8);
                }
            }
        }

        return null;
    }

    public static void set(HttpExchange exchange, String name, String value) {
        String v = URLEncoder.encode(value, StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Set-Cookie", name + "=" + v + "; Path=/; HttpOnly; SameSite=Lax");
    }

    public static void clear(HttpExchange exchange, String name) {
        exchange.getResponseHeaders().add("Set-Cookie", name + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
    }
}
