package com.project.pure.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class Router implements HttpHandler {

    @FunctionalInterface
    public interface Handler {
        void handle(HttpExchange exchange) throws Exception;
    }

    private static class Route {
        final String method;
        final Pattern pathPattern;
        final Handler handler;

        Route(String method, Pattern pathPattern, Handler handler) {
            this.method = method;
            this.pathPattern = pathPattern;
            this.handler = handler;
        }
    }

    private final List<Route> routes = new ArrayList<>();

    public void get(String pathRegex, Handler handler) {
        add("GET", pathRegex, handler);
    }

    public void post(String pathRegex, Handler handler) {
        add("POST", pathRegex, handler);
    }

    private void add(String method, String pathRegex, Handler handler) {
        Objects.requireNonNull(method);
        Objects.requireNonNull(pathRegex);
        Objects.requireNonNull(handler);
        routes.add(new Route(method, Pattern.compile("^" + pathRegex + "$"), handler));
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            System.out.println("[HTTP] " + method + " " + path);

            for (Route r : routes) {
                if (r.method.equalsIgnoreCase(method) && r.pathPattern.matcher(path).matches()) {
                    r.handler.handle(exchange);
                    return;
                }
            }

            byte[] body = "Not Found".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(404, body.length);
            exchange.getResponseBody().write(body);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            System.err.println(sw);

            String msg = e.getClass().getName() + ": " + e.getMessage();
            byte[] body = ("Internal Server Error\n" + msg).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(500, body.length);
            exchange.getResponseBody().write(body);
        } finally {
            exchange.close();
        }
    }
}
