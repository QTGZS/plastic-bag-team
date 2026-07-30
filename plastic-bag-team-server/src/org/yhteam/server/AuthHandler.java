package org.yhteam.server;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * POST /api/v1/auth/verify
 * Body: { "username", "password", "machineCode", "clientType" }
 * Used by the Fabric mod to validate a user at launch.
 */
public class AuthHandler implements com.sun.net.httpserver.HttpHandler {
    @SuppressWarnings("unchecked")
    public void handle(HttpExchange ex) throws IOException {
        if (Resp.preflight(ex)) return;
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            Resp.error(ex, 405, "Method not allowed");
            return;
        }
        Object req = Json.parse(Resp.body(ex));
        String username = Json.getString(req, "username");
        String password = Json.getString(req, "password");
        String machineCode = Json.getString(req, "machineCode");
        String clientType = Json.getString(req, "clientType");
        if (clientType == null || clientType.isEmpty()) clientType = "AlienV4";

        Map<String, Object> resp = new LinkedHashMap<>();
        if (username == null || password == null || machineCode == null) {
            resp.put("success", false);
            resp.put("code", "BAD_REQUEST");
            resp.put("message", "Missing field(s): username, password, machineCode");
            Resp.json(ex, 400, resp);
            return;
        }

        Map<String, Object> acc = Database.findAccount(username);
        if (acc == null) {
            resp.put("success", false);
            resp.put("code", "NOT_PURCHASED");
            resp.put("message", "Account not found. AlienV4 client not purchased.");
            Resp.json(ex, 403, resp);
            return;
        }
        String ph = String.valueOf(acc.get("passwordHash"));
        if (!ph.equals(Util.sha256(password))) {
            resp.put("success", false);
            resp.put("code", "INVALID_CREDENTIALS");
            resp.put("message", "Wrong username or password.");
            Resp.json(ex, 403, resp);
            return;
        }
        if (!clientType.equalsIgnoreCase(String.valueOf(acc.get("clientType")))) {
            resp.put("success", false);
            resp.put("code", "NOT_PURCHASED");
            resp.put("message", "This account does not own client: " + clientType);
            Resp.json(ex, 403, resp);
            return;
        }
        Boolean active = (Boolean) acc.get("active");
        if (active != null && !active) {
            resp.put("success", false);
            resp.put("code", "DISABLED");
            resp.put("message", "This account has been disabled.");
            Resp.json(ex, 403, resp);
            return;
        }
        long expireAt = Json.getLong(acc, "expireAt", 0);
        if (expireAt > 0 && Util.now() > expireAt) {
            resp.put("success", false);
            resp.put("code", "EXPIRED");
            resp.put("message", "Your AlienV4 license has expired.");
            Resp.json(ex, 403, resp);
            return;
        }

        String bound = String.valueOf(acc.get("machineCode"));
        boolean needsBind = false;
        if (bound == null || bound.isEmpty() || "null".equals(bound)) {
            Database.bindMachine(username, machineCode);
            needsBind = true;
        } else if (!bound.equals(machineCode)) {
            resp.put("success", false);
            resp.put("code", "MACHINE_MISMATCH");
            resp.put("message", "This account is already bound to another machine. Contact admin to reset.");
            Resp.json(ex, 403, resp);
            return;
        }

        String token = Util.token();
        long tokenExpire = Util.now() + 7L * 86400000L;
        Database.setSession(username, token, tokenExpire);

        resp.put("success", true);
        resp.put("code", "OK");
        resp.put("message", needsBind ? "Verified. Machine bound." : "Verified.");
        resp.put("token", token);
        resp.put("expireAt", expireAt);
        resp.put("bound", true);
        resp.put("needsBind", needsBind);
        Resp.json(ex, 200, resp);
    }
}
