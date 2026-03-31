package com.project.pure.template;

import com.sun.net.httpserver.HttpExchange;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.standard.StandardDialect;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.TreeMap;

public class TemplateRenderer {

    private final TemplateEngine engine;

    private TemplateRenderer(TemplateEngine engine) {
        this.engine = engine;
    }

    public static TemplateRenderer fromClasspath() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);

        StandardDialect standardDialect = new StandardDialect();
        standardDialect.setJavaScriptSerializer(new JacksonJavaScriptSerializer());
        engine.setDialect(standardDialect);

        engine.addDialect(new FieldDialect());
        engine.addDialect(new Java8TimeDialect());
        engine.setLinkBuilder(new SimpleLinkBuilder());
        return new TemplateRenderer(engine);
    }

    public void render(HttpExchange exchange, String template, Map<String, Object> model) throws IOException {
        Context ctx = new Context(Locale.FRANCE);
        if (model != null) {
            ctx.setVariables(model);
        }

        ctx.setVariable("param", buildParamMap(exchange.getRequestURI().getRawQuery()));

        String html = engine.process(template, ctx);
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private static Map<String, String[]> buildParamMap(String rawQuery) {
        Map<String, List<String>> tmp = new TreeMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return Map.of();
        }

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
            tmp.computeIfAbsent(k, __ -> new ArrayList<>()).add(v);
        }

        Map<String, String[]> out = new TreeMap<>();
        for (var e : tmp.entrySet()) {
            out.put(e.getKey(), e.getValue().toArray(new String[0]));
        }
        return out;
    }
}
