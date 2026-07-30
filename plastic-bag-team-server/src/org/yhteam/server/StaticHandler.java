package org.yhteam.server;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Serves the admin website (塑料袋子Team) and static assets from classpath.
 */
public class StaticHandler implements com.sun.net.httpserver.HttpHandler {
    public void handle(HttpExchange ex) throws IOException {
        if (Resp.preflight(ex)) return;
        String p = ex.getRequestURI().getPath();
        if (p.equals("/") || p.equals("/index.html")) {
            serve(ex, "/index.html", "text/html");
        } else if (p.equals("/style.css")) {
            serve(ex, "/style.css", "text/css");
        } else if (p.equals("/app.js")) {
            serve(ex, "/app.js", "application/javascript");
        } else {
            Resp.text(ex, 404, "text/plain", "404 Not Found");
        }
    }

    private void serve(HttpExchange ex, String res, String ct) throws IOException {
        try (InputStream in = Server.class.getResourceAsStream(res)) {
            if (in == null) {
                Resp.text(ex, 404, "text/plain", "404 Not Found");
                return;
            }
            byte[] b = in.readAllBytes();
            ex.getResponseHeaders().add("Content-Type", ct + "; charset=utf-8");
            ex.sendResponseHeaders(200, b.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(b); }
        }
    }
}
