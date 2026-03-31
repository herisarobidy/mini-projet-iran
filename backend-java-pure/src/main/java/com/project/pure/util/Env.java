package com.project.pure.util;

public final class Env {

    private Env() {}

    public static String get(String key, String defaultValue) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            return defaultValue;
        }
        return v;
    }

    public static int getInt(String key, int defaultValue) {
        String v = System.getenv(key);
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
