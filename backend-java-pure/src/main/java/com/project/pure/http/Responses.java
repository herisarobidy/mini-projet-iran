package com.project.pure.http;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class Responses {

    private Responses() {}

    public static void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
    }

    public static void notFound(HttpExchange exchange) throws IOException {
        byte[] body = "Not Found".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(404, body.length);
        exchange.getResponseBody().write(body);
    }

    public static void forbidden(HttpExchange exchange, String message) throws IOException {
        String m = message == null ? "Forbidden" : message;
        byte[] body = ("Forbidden\n" + m).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(403, body.length);
        exchange.getResponseBody().write(body);
    }
}
