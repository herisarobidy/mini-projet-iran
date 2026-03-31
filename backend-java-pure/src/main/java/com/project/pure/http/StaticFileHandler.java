package com.project.pure.http;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class StaticFileHandler {

    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path == null || path.isBlank() || "/".equals(path)) {
            notFound(exchange);
            return;
        }

        String resourcePath = path.startsWith("/") ? path.substring(1) : path;
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream("static/" + resourcePath);
        }

        try (InputStream input = in) {
            if (input == null) {
                notFound(exchange);
                return;
            }

            byte[] bytes = input.readAllBytes();
            String contentType = URLConnection.guessContentTypeFromName(resourcePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }

    private void notFound(HttpExchange exchange) throws IOException {
        byte[] body = "Not Found".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(404, body.length);
        exchange.getResponseBody().write(body);
    }
}
