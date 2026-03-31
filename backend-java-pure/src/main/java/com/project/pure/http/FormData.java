package com.project.pure.http;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class FormData {

    private FormData() {}

    public static Map<String, String> parseUrlEncoded(HttpExchange exchange) throws IOException {
        String ct = exchange.getRequestHeaders().getFirst("Content-Type");
        if (ct == null || !ct.toLowerCase().startsWith("application/x-www-form-urlencoded")) {
            return Collections.emptyMap();
        }

        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String body = new String(bytes, StandardCharsets.UTF_8);
        if (body.isBlank()) {
            return Collections.emptyMap();
        }

        Map<String, String> out = new HashMap<>();
        String[] pairs = body.split("&");
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
}
