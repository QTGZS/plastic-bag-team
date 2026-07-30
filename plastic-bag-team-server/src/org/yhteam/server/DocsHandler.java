package org.yhteam.server;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Serves the Chinese API documentation (API文档.md embedded as resource).
 */
public class DocsHandler implements com.sun.net.httpserver.HttpHandler {
    public void handle(HttpExchange ex) throws IOException {
        if (Resp.preflight(ex)) return;
        try (InputStream in = Server.class.getResourceAsStream("/api-docs.html")) {
            if (in == null) {
                Resp.text(ex, 404, "text/plain", "docs not found");
                return;
            }
            byte[] b = in.readAllBytes();
            ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            ex.sendResponseHeaders(200, b.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(b); }
        }
    }
}
