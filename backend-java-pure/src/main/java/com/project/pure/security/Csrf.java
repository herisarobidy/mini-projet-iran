package com.project.pure.security;

import com.project.pure.http.SessionManager;

import java.security.SecureRandom;
import java.util.Base64;

public final class Csrf {

    public record Token(String parameterName, String token) {
    }

    private static final String CSRF_KEY = "csrf";

    private Csrf() {}

    public static Token ensure(SessionManager.Session session) {
        Object existing = session.data().get(CSRF_KEY);
        if (existing instanceof String s && !s.isBlank()) {
            return new Token("_csrf", s);
        }

        String t = newToken();
        session.data().put(CSRF_KEY, t);
        return new Token("_csrf", t);
    }

    public static boolean validate(SessionManager.Session session, String token) {
        if (session == null || token == null) {
            return false;
        }
        Object existing = session.data().get(CSRF_KEY);
        return existing instanceof String s && s.equals(token);
    }

    private static String newToken() {
        byte[] buf = new byte[24];
        new SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
