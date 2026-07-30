package com.nyxclient.verify.auth;

/**
 * Persistent login session. After the first successful verification,
 * the token and expiry are saved. On subsequent launches, if the
 * session is still valid, the auth window is skipped.
 */
public final class AuthSession {
    private static final String KEY_TOKEN   = "auth.token";
    private static final String KEY_EXPIRE  = "auth.token.expire";
    private static final String KEY_USER    = "auth.username";

    /** True if we have a saved token that hasn't expired yet. */
    public static boolean hasValidSession() {
        String token = Config.get(KEY_TOKEN, "");
        if (token.isEmpty()) return false;
        long expire = parseExpire(Config.get(KEY_EXPIRE, "0"));
        return expire > System.currentTimeMillis();
    }

    /** The saved username (for the welcome message). */
    public static String getUsername() {
        return Config.get(KEY_USER, "");
    }

    /** Called right after a successful server-side verification. */
    public static void saveSession(String token, String username, long expireAt) {
        Config.set(KEY_TOKEN, token);
        Config.set(KEY_USER, username);
        Config.set(KEY_EXPIRE, String.valueOf(expireAt));
    }

    /** Clear the saved session (e.g. after a failed re-auth). */
    public static void clearSession() {
        Config.set(KEY_TOKEN, "");
        Config.set(KEY_USER, "");
        Config.set(KEY_EXPIRE, "0");
    }

    private static long parseExpire(String v) {
        try { return Long.parseLong(v); } catch (NumberFormatException e) { return 0L; }
    }
}
