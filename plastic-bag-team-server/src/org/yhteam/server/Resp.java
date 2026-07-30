package org.yhteam.server;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Resp {
    public static void json(HttpExchange ex, int code, Map<String, Object> m) throws IOException {
        byte[] b = Json.stringify(m).getBytes(StandardCharsets.UTF_8);
        headers(ex);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    public static void text(HttpExchange ex, int code, String ct, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        headers(ex);
        ex.getResponseHeaders().add("Content-Type", ct + "; charset=utf-8");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    public static void error(HttpExchange ex, int code, String msg) throws IOException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", false);
        m.put("code", "ERROR");
        m.put("message", msg);
        json(ex, code, m);
    }

    public static String body(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static String getParam(HttpExchange ex, String name) {
        return Util.getQueryParam(ex.getRequestURI().getQuery(), name);
    }

    private static void headers(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, X-Admin-Token");
    }

    /** Handle CORS preflight */
    public static boolean preflight(HttpExchange ex) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            headers(ex);
            ex.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }
}
