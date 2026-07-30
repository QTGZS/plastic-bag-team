package org.yhteam.server;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GET /api/v1/account/info?token=xxx
 * Returns account info for a valid session token.
 */
public class AccountHandler implements com.sun.net.httpserver.HttpHandler {
    public void handle(HttpExchange ex) throws IOException {
        if (Resp.preflight(ex)) return;
        String token = Resp.getParam(ex, "token");
        Map<String, Object> resp = new LinkedHashMap<>();
        if (token == null) {
            resp.put("success", false);
            resp.put("code", "NO_TOKEN");
            resp.put("message", "Missing token");
            Resp.json(ex, 400, resp);
            return;
        }
        Map<String, Object> acc = Database.findAccountByToken(token);
        if (acc == null) {
            resp.put("success", false);
            resp.put("code", "INVALID_TOKEN");
            resp.put("message", "Invalid or expired session");
            Resp.json(ex, 401, resp);
            return;
        }
        resp.put("success", true);
        resp.put("username", acc.get("username"));
        resp.put("clientType", acc.get("clientType"));
        resp.put("expireAt", acc.get("expireAt"));
        resp.put("bound", !String.valueOf(acc.get("machineCode")).isEmpty());
        resp.put("active", acc.get("active"));
        Resp.json(ex, 200, resp);
    }
}
