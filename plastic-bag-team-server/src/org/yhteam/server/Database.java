package org.yhteam.server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Persistent JSON-file database for accounts and admin config.
 * Thread safety via synchronized block.
 */
public final class Database {
    static final File FILE = new File("yhteam_data.json");
    static final Map<String, Object> data = new LinkedHashMap<>();

    // In-memory admin sessions: token -> expiry
    static final Map<String, Long> adminSessions = new HashMap<>();
    static final long ADMIN_SESSION_TTL = 1000L * 60 * 60 * 24; // 24h

    static {
        data.put("accounts", new ArrayList<Map<String, Object>>());
        data.put("adminPasswordHash", Util.sha256("admin123999"));
    }

    public static synchronized void load() {
        if (FILE.exists()) {
            try {
                String txt = Files.readString(FILE.toPath());
                Object parsed = Json.parse(txt);
                if (parsed instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) parsed;
                    if (m.containsKey("accounts"))
                        data.put("accounts", m.get("accounts"));
                    if (m.containsKey("adminPasswordHash"))
                        data.put("adminPasswordHash", m.get("adminPasswordHash"));
                }
                System.out.println("✓ DB loaded (" + getAccountList().size() + " accounts)");
            } catch (Exception e) {
                System.err.println("! DB load error: " + e.getMessage());
                save();
            }
        } else {
            // 首次启动：随机生成管理员密码，仅打印一次
            String adminPw = generatePassword(12);
            data.put("adminPasswordHash", Util.sha256(adminPw));
            addAccountInternal("admin", "admin123", "AlienV4", 365);
            save();
            System.out.println("========================================");
            System.out.println("  初始管理员密码(随机生成，请妥善保存): " + adminPw);
            System.out.println("  默认测试账号: admin / admin123 (365天)");
            System.out.println("========================================");
        }
    }

    /** Generate a random admin password (no ambiguous chars). */
    private static String generatePassword(int len) {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        java.security.SecureRandom r = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }

    public static synchronized void save() {
        try {
            File parent = FILE.getParentFile();
            if (parent != null) parent.mkdirs();
            Files.writeString(FILE.toPath(), Json.stringify(data), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("! DB save error: " + e.getMessage());
        }
    }

    // ── Account helpers ──

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getAccountList() {
        Object v = data.get("accounts");
        if (v instanceof List) return (List<Map<String, Object>>) v;
        List<Map<String, Object>> list = new ArrayList<>();
        data.put("accounts", list);
        return list;
    }

    public static Map<String, Object> findAccount(String username) {
        for (Map<String, Object> a : getAccountList()) {
            if (username.equals(a.get("username"))) return a;
        }
        return null;
    }

    public static Map<String, Object> findAccountByToken(String token) {
        for (Map<String, Object> a : getAccountList()) {
            if (token != null && token.equals(a.get("token"))) {
                Long expire = (Long) a.get("tokenExpire");
                if (expire != null && expire > Util.now()) return a;
            }
        }
        return null;
    }

    // ── Account creation ──

    public static synchronized void addAccountInternal(
            String username, String password, String clientType, int durationDays) {
        Map<String, Object> acc = new LinkedHashMap<>();
        acc.put("username", username);
        acc.put("passwordHash", Util.sha256(password));
        acc.put("clientType", clientType != null ? clientType : "AlienV4");
        acc.put("machineCode", "");
        acc.put("createdAt", Util.now());
        acc.put("expireAt", Util.now() + durationDays * 86400000L);
        acc.put("active", true);
        acc.put("token", "");
        acc.put("tokenExpire", 0L);
        getAccountList().add(acc);
        save();
    }

    // ── Admin session ──

    public static synchronized String createAdminSession() {
        String token = Util.token();
        adminSessions.put(token, Util.now() + ADMIN_SESSION_TTL);
        return token;
    }

    public static synchronized boolean validateAdminSession(String token) {
        if (token == null) return false;
        Long expire = adminSessions.get(token);
        if (expire == null) return false;
        if (Util.now() > expire) {
            adminSessions.remove(token);
            return false;
        }
        return true;
    }

    // ── Auth check ──

    public static boolean checkAdminPassword(String password) {
        String stored = String.valueOf(data.get("adminPasswordHash"));
        return stored.equals(Util.sha256(password));
    }

    // ── Mutation helpers ──

    public static synchronized void bindMachine(String username, String machineCode) {
        Map<String, Object> acc = findAccount(username);
        if (acc != null) {
            acc.put("machineCode", machineCode);
            save();
        }
    }

    public static synchronized void setSession(String username, String token, long tokenExpire) {
        Map<String, Object> acc = findAccount(username);
        if (acc != null) {
            acc.put("token", token);
            acc.put("tokenExpire", tokenExpire);
            save();
        }
    }
}
