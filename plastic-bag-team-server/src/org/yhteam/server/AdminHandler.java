package org.yhteam.server;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin API under /api/v1/admin
 *   POST /api/v1/admin/login                { "password": "admin123999" }
 *   GET  /api/v1/admin/accounts             (header X-Admin-Token)
 *   POST /api/v1/admin/account              { "username","password","durationDays","clientType" }
 *   POST /api/v1/admin/account/renew        { "username","durationDays" }
 *   POST /api/v1/admin/account/delete       { "username" }
 *   POST /api/v1/admin/account/resetmachine { "username" }
 *   GET  /api/v1/admin/products             (header X-Admin-Token)
 *   POST /api/v1/admin/product              { "id","name","description" }
 *   POST /api/v1/admin/product/delete       { "id" }
 */
public class AdminHandler implements com.sun.net.httpserver.HttpHandler {
    public void handle(HttpExchange ex) throws IOException {
        if (Resp.preflight(ex)) return;
        String path = ex.getRequestURI().getPath(); // e.g. /api/v1/admin/login
        String sub = path.substring("/api/v1/admin".length());
        if (sub.isEmpty() || sub.equals("/")) sub = "/login";

        if (sub.equals("/login")) {
            handleLogin(ex);
            return;
        }

        // all other admin endpoints require a valid session token
        String token = ex.getRequestHeaders().getFirst("X-Admin-Token");
        if (token == null) token = Resp.getParam(ex, "adminToken");
        if (!Database.validateAdminSession(token)) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("success", false);
            r.put("code", "UNAUTHORIZED");
            r.put("message", "Admin authentication required");
            Resp.json(ex, 401, r);
            return;
        }

        switch (sub) {
            case "/accounts":          handleList(ex); break;
            case "/account":            handleAdd(ex); break;
            case "/account/renew":      handleRenew(ex); break;
            case "/account/delete":     handleDelete(ex); break;
            case "/account/resetmachine": handleReset(ex); break;
            case "/products":          handleProducts(ex); break;
            case "/product":            handleAddProduct(ex); break;
            case "/product/delete":     handleDeleteProduct(ex); break;
            default:
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("success", false);
                r.put("code", "NOT_FOUND");
                r.put("message", "Unknown admin endpoint: " + sub);
                Resp.json(ex, 404, r);
        }
    }

    private void handleLogin(HttpExchange ex) throws IOException {
        Map<String, Object> r = new LinkedHashMap<>();
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            Resp.error(ex, 405, "Method not allowed");
            return;
        }
        Object req = Json.parse(Resp.body(ex));
        String password = Json.getString(req, "password");
        if (password == null || !Database.checkAdminPassword(password)) {
            r.put("success", false);
            r.put("code", "WRONG_PASSWORD");
            r.put("message", "Wrong admin password");
            Resp.json(ex, 403, r);
            return;
        }
        String token = Database.createAdminSession();
        r.put("success", true);
        r.put("message", "Admin login OK");
        r.put("adminToken", token);
        Resp.json(ex, 200, r);
    }

    @SuppressWarnings("unchecked")
    private void handleList(HttpExchange ex) throws IOException {
        Map<String, Object> r = new LinkedHashMap<>();
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (Map<String, Object> a : Database.getAccountList()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("username", a.get("username"));
            m.put("clientType", a.get("clientType"));
            m.put("machineCode", mask((String) a.get("machineCode")));
            m.put("createdAt", a.get("createdAt"));
            m.put("expireAt", a.get("expireAt"));
            m.put("active", a.get("active"));
            long exp = ((Number) a.get("expireAt")).longValue();
            m.put("expired", exp > 0 && exp < Util.now());
            out.add(m);
        }
        r.put("success", true);
        r.put("count", out.size());
        r.put("accounts", out);
        Resp.json(ex, 200, r);
    }

    @SuppressWarnings("unchecked")
    private void handleAdd(HttpExchange ex) throws IOException {
        Map<String, Object> r = new LinkedHashMap<>();
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            Resp.error(ex, 405, "Method not allowed");
            return;
        }
        Object req = Json.parse(Resp.body(ex));
        String username = Json.getString(req, "username");
        String password = Json.getString(req, "password");
        String clientType = Json.getString(req, "clientType");
        int days = (int) Json.getLong(req, "durationDays", 30);
        if (username == null || password == null) {
            r.put("success", false);
            r.put("code", "BAD_REQUEST");
            r.put("message", "username and password required");
            Resp.json(ex, 400, r);
            return;
        }
        if (Database.findAccount(username) != null) {
            r.put("success", false);
            r.put("code", "EXISTS");
            r.put("message", "Account already exists");
            Resp.json(ex, 409, r);
            return;
        }
        Database.addAccountInternal(username, password, clientType, days);
        r.put("success", true);
        r.put("message", "Account created: " + username);
        r.put("expireAt", Util.now() + days * 86400000L);
        Resp.json(ex, 200, r);
    }

    @SuppressWarnings("unchecked")
    private void handleRenew(HttpExchange ex) throws IOException {
        Map<String, Object> r = new LinkedHashMap<>();
        Object req = Json.parse(Resp.body(ex));
        String username = Json.getString(req, "username");
        int days = (int) Json.getLong(req, "durationDays", 30);
        Map<String, Object> acc = Database.findAccount(username);
        if (acc == null) {
            r.put("success", false);
            r.put("code", "NOT_FOUND");
            r.put("message", "Account not found");
            Resp.json(ex, 404, r);
            return;
        }
        long current = ((Number) acc.get("expireAt")).longValue();
        long base = current > Util.now() ? current : Util.now();
        acc.put("expireAt", base + days * 86400000L);
        acc.put("active", true);
        Database.save();
        r.put("success", true);
        r.put("message", "Renewed " + days + " days");
        r.put("expireAt", acc.get("expireAt"));
        Resp.json(ex, 200, r);
    }

    @SuppressWarnings("unchecked")
    private void handleDelete(HttpExchange ex) throws IOException {
        Map<String, Object> r = new LinkedHashMap<>();
        Object req = Json.parse(Resp.body(ex));
        String username = Json.getString(req, "username");
        List<Map<String, Object>> list = Database.getAccountList();
        boolean removed = list.removeIf(a -> username.equals(a.get("username")));
        Database.save();
        r.put("success", removed);
        r.put("message", removed ? "Deleted: " + username : "Account not found");
        Resp.json(ex, removed ? 200 : 404, r);
    }

    @SuppressWarnings("unchecked")
    private void handleReset(HttpExchange ex) throws IOException {
        Map<String, Object> r = new LinkedHashMap<>();
        Object req = Json.parse(Resp.body(ex));
        String username = Json.getString(req, "username");
        Map<String, Object> acc = Database.findAccount(username);
        if (acc == null) {
            r.put("success", false);
            r.put("code", "NOT_FOUND");
            r.put("message", "Account not found");
            Resp.json(ex, 404, r);
            return;
        }
        acc.put("machineCode", "");
        acc.put("token", "");
        acc.put("tokenExpire", 0L);
        Database.save();
        r.put("success", true);
        r.put("message", "Machine binding reset for: " + username);
        Resp.json(ex, 200, r);
    }

    // ── Client type (product) management ──

    @SuppressWarnings("unchecked")
    private void handleProducts(HttpExchange ex) throws IOException {
        Map<String, Object> r = new LinkedHashMap<>();
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (Map<String, Object> p : Database.getProductList()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.get("id"));
            m.put("name", p.get("name"));
            m.put("description", p.get("description"));
            m.put("createdAt", p.get("createdAt"));
            out.add(m);
        }
        r.put("success", true);
        r.put("count", out.size());
        r.put("products", out);
        Resp.json(ex, 200, r);
    }

    @SuppressWarnings("unchecked")
    private void handleAddProduct(HttpExchange ex) throws IOException {
        Map<String, Object> r = new LinkedHashMap<>();
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            Resp.error(ex, 405, "Method not allowed");
            return;
        }
        Object req = Json.parse(Resp.body(ex));
        String id = Json.getString(req, "id");
        String name = Json.getString(req, "name");
        String description = Json.getString(req, "description");
        if (id == null || id.isEmpty()) {
            r.put("success", false);
            r.put("code", "BAD_REQUEST");
            r.put("message", "client id required");
            Resp.json(ex, 400, r);
            return;
        }
        if (Database.findProduct(id) != null) {
            r.put("success", false);
            r.put("code", "EXISTS");
            r.put("message", "Client already exists");
            Resp.json(ex, 409, r);
            return;
        }
        Database.addProduct(id, name, description);
        r.put("success", true);
        r.put("message", "Client created: " + id);
        Resp.json(ex, 200, r);
    }

    @SuppressWarnings("unchecked")
    private void handleDeleteProduct(HttpExchange ex) throws IOException {
        Map<String, Object> r = new LinkedHashMap<>();
        Object req = Json.parse(Resp.body(ex));
        String id = Json.getString(req, "id");
        if (id == null || id.isEmpty()) {
            r.put("success", false);
            r.put("code", "BAD_REQUEST");
            r.put("message", "client id required");
            Resp.json(ex, 400, r);
            return;
        }
        boolean removed = Database.removeProduct(id);
        r.put("success", removed);
        r.put("message", removed ? "Client deleted: " + id : "Client not found");
        Resp.json(ex, removed ? 200 : 404, r);
    }

    private static String mask(String s) {
        if (s == null || s.isEmpty() || "null".equals(s)) return "";
        if (s.length() <= 8) return s;
        return s.substring(0, 8) + "****";
    }
}
