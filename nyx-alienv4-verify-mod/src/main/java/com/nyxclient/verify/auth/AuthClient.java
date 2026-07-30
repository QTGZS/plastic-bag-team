package com.nyxclient.verify.auth;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Talks to the 塑料袋子Team verification API.
 * POST {base}/api/v1/auth/verify
 */
public final class AuthClient {

    public static final class Result {
        public boolean success;
        public String code = "";
        public String message = "";
        public String token = "";
        public boolean needsBind;
        public long expireAt;
    }

    /** One raw HTTP attempt. */
    private static final class Attempt {
        Result result = new Result();
        String url = "";
        int httpCode = -1;
        String body = "";
        String error = "";
    }

    /**
     * Try every candidate endpoint (configured base + known domains, each with
     * http and https) until one responds. As soon as a server answers — even
     * with an auth failure — we return that result. We only move to the next
     * candidate on a NETWORK_ERROR (unreachable / TLS failure).
     */
    public static Result verify(String username, String password) {
        java.util.LinkedHashSet<String> candidates = new java.util.LinkedHashSet<>();
        candidates.add(Config.apiBase());
        candidates.add("https://play.simpfun.cn:14639");
        candidates.add("http://play.simpfun.cn:14639");
        candidates.add("https://yh-team.org");
        candidates.add("http://yh-team.org");
        candidates.add("https://3c3u.org");
        candidates.add("http://3c3u.org");

        Attempt last = new Attempt();
        for (String base : candidates) {
            Attempt a = tryAttempt(base, username, password);
            if (a.result.success || !"NETWORK_ERROR".equals(a.result.code)) {
                return a.result;
            }
            last = a;
        }
        return last.result;
    }

    private static Attempt tryAttempt(String base, String username, String password) {
        Attempt a = new Attempt();
        try {
            String path = Config.get("api.verify.path", "/api/v1/auth/verify");
            URL url = new URL(base + path);
            a.url = url.toString();
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setConnectTimeout(5000);
            c.setReadTimeout(8000);
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            c.setRequestProperty("User-Agent", "NyxAlienV4Client/1.0");

            String body = buildJson(username, password, MachineCode.get(), Config.clientType());
            try (OutputStream os = c.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            a.httpCode = c.getResponseCode();
            a.body = read(c);
            parse(a.result, a.body, a.httpCode);
        } catch (Exception e) {
            a.error = e.toString();
            // network / parse error -> treat as failure, allow retry
            a.result.success = false;
            a.result.code = "NETWORK_ERROR";
            a.result.message = e.getMessage();
        }
        return a;
    }

    private static void parse(Result r, String resp, int httpCode) {
        if (resp == null || resp.isEmpty()) {
            r.success = false;
            r.code = "EMPTY";
            return;
        }
        // minimal JSON field extraction (no external lib)
        r.success = boolField(resp, "success");
        r.code = strField(resp, "code");
        r.message = strField(resp, "message");
        r.token = strField(resp, "token");
        r.needsBind = boolField(resp, "needsBind");
        r.expireAt = longField(resp, "expireAt");
    }

    private static long longField(String json, String key) {
        String v = field(json, key);
        if (v == null || v.isEmpty()) return 0L;
        try { return Long.parseLong(v); } catch (NumberFormatException e) { return 0L; }
    }

    private static boolean boolField(String json, String key) {
        String v = field(json, key);
        return v != null && (v.equals("true"));
    }

    private static String strField(String json, String key) {
        String v = field(json, key);
        if (v == null) return "";
        return v;
    }

    private static String field(String json, String key) {
        // find "key":"value" or "key":value
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + key.length() + 2);
        if (colon < 0) return null;
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        char ch = json.charAt(i);
        if (ch == '"') {
            int end = json.indexOf('"', i + 1);
            if (end < 0) return null;
            return json.substring(i + 1, end);
        } else {
            int end = i;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}' && json.charAt(end) != ' ')
                end++;
            return json.substring(i, end).trim();
        }
    }

    private static String read(HttpURLConnection c) throws Exception {
        java.io.InputStream in = c.getResponseCode() < 400 ? c.getInputStream() : c.getErrorStream();
        if (in == null) return "";
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        return bos.toString(StandardCharsets.UTF_8);
    }

    private static String buildJson(String user, String pass, String mc, String ct) {
        StringBuilder b = new StringBuilder();
        b.append("{");
        b.append("\"username\":\"").append(esc(user)).append("\",");
        b.append("\"password\":\"").append(esc(pass)).append("\",");
        b.append("\"machineCode\":\"").append(esc(mc)).append("\",");
        b.append("\"clientType\":\"").append(esc(ct)).append("\"");
        b.append("}");
        return b.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
