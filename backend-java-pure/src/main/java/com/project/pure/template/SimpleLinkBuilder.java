package com.project.pure.template;

import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.linkbuilder.ILinkBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Link builder for Thymeleaf standalone usage (no IWebContext).
 * Supports templates that use @{/...} by treating them as absolute-path links.
 */
public class SimpleLinkBuilder implements ILinkBuilder {

    @Override
    public String getName() {
        return "simple-link-builder";
    }

    @Override
    public Integer getOrder() {
        return 0;
    }

    @Override
    public String buildLink(IExpressionContext context, String base, Map<String, Object> parameters) {
        if (base == null || base.isBlank()) {
            return base;
        }

        ReplaceResult rr = replacePathVariables(base, parameters);
        return appendQueryParams(rr.url, parameters, rr.usedKeys);
    }

    private record ReplaceResult(String url, Set<String> usedKeys) {
    }

    private static ReplaceResult replacePathVariables(String base, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty() || base == null || base.isBlank()) {
            return new ReplaceResult(base, Set.of());
        }

        String out = base;
        Set<String> used = new HashSet<>();
        for (Map.Entry<String, Object> e : parameters.entrySet()) {
            String k = e.getKey();
            Object v = e.getValue();
            if (k == null || k.isBlank() || v == null) {
                continue;
            }
            String placeholder = "{" + k + "}";
            if (out.contains(placeholder)) {
                out = out.replace(placeholder, encode(String.valueOf(v)));
                used.add(k);
            }
        }
        return new ReplaceResult(out, used);
    }

    private static String appendQueryParams(String base, Map<String, Object> parameters, Set<String> excludedKeys) {
        if (parameters == null || parameters.isEmpty()) {
            return base;
        }

        Set<String> excluded = excludedKeys == null ? Set.of() : excludedKeys;

        List<String> pairs = new ArrayList<>();
        for (Map.Entry<String, Object> e : parameters.entrySet()) {
            String k = e.getKey();
            Object v = e.getValue();
            if (k == null || k.isBlank() || v == null) {
                continue;
            }

            if (excluded.contains(k)) {
                continue;
            }

            String placeholder = "{" + k + "}";
            if (base != null && base.contains(placeholder)) {
                continue;
            }
            pairs.add(encode(k) + "=" + encode(String.valueOf(v)));
        }

        if (pairs.isEmpty()) {
            return base;
        }

        String sep = base.contains("?") ? "&" : "?";
        return base + sep + String.join("&", pairs);
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
