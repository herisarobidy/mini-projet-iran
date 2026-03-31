package com.project.pure.http;

import com.sun.net.httpserver.HttpExchange;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    public record Session(String id, Map<String, Object> data) {
    }

    private static final String COOKIE_NAME = "SID";

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public Session getOrCreate(HttpExchange exchange) {
        String sid = Cookies.get(exchange, COOKIE_NAME);
        if (sid != null) {
            Session existing = sessions.get(sid);
            if (existing != null) {
                return existing;
            }
        }

        String newSid = newId();
        Session session = new Session(newSid, new ConcurrentHashMap<>());
        sessions.put(newSid, session);
        Cookies.set(exchange, COOKIE_NAME, newSid);
        return session;
    }

    public Session get(HttpExchange exchange) {
        String sid = Cookies.get(exchange, COOKIE_NAME);
        if (sid == null) {
            return null;
        }
        return sessions.get(sid);
    }

    public void invalidate(HttpExchange exchange) {
        String sid = Cookies.get(exchange, COOKIE_NAME);
        if (sid != null) {
            sessions.remove(sid);
        }
        Cookies.clear(exchange, COOKIE_NAME);
    }

    private String newId() {
        byte[] buf = new byte[24];
        random.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
