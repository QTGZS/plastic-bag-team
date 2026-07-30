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
    }

    public static Result verify(String username, String password) {
        Result r = new Result();
        try {
            String base = Config.apiBase();
            String path = Config.get("api.verify.path", "/api/v1/auth/verify");
            URL url = new URL(base + path);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setConnectTimeout(10000);
            c.setReadTimeout(10000);
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            c.setRequestProperty("User-Agent", "NyxAlienV4Client/1.0");

            String body = buildJson(username, password, MachineCode.get(), Config.clientType());
            try (OutputStream os = c.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int code = c.getResponseCode();
            String resp = read(c);
            parse(r, resp, code);
        } catch (Exception e) {
            // network / parse error -> treat as failure, allow retry
            r.success = false;
            r.code = "NETWORK_ERROR";
            r.message = e.getMessage();
        }
        return r;
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
