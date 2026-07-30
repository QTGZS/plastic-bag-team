package org.yhteam.server;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public final class Server {
    public static final int PORT = 14639;

    public static void main(String[] args) throws Exception {
        System.out.println("======================================");
        System.out.println("  塑料袋子Team API Server v1.0");
        System.out.println("  yh-team.org : " + PORT);
        System.out.println("======================================");
        Database.load();
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 16);
        server.createContext("/api/v1/auth/verify", new AuthHandler());
        server.createContext("/api/v1/account/info", new AccountHandler());
        server.createContext("/api/v1/admin", new AdminHandler());
        server.createContext("/api-docs", new DocsHandler());
        server.createContext("/", new StaticHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("✓ Server started on port " + PORT);
        System.out.println("✓ Admin URL: http://0.0.0.0:" + PORT + "/");
        System.out.println("✓ API Docs: http://0.0.0.0:" + PORT + "/api-docs");
        System.out.println("  (in production, configure nginx reverse proxy for yh-team.org)");
    }
}
