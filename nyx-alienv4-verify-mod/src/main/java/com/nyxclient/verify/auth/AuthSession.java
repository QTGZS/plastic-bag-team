package com.nyxclient.verify.auth;

/**
 * Persistent login credentials. After the first successful login,
 * username and password are saved locally. On subsequent launches
 * the mod silently re-authenticates with the server — no token caching.
 */
public final class AuthSession {
    private static final String KEY_USER = "auth.username";
    private static final String KEY_PASS = "auth.password";

    /** True if we have saved credentials. */
    public static boolean hasSavedCredentials() {
        return !Config.get(KEY_USER, "").isEmpty()
            && !Config.get(KEY_PASS, "").isEmpty();
    }

    public static String getUsername() { return Config.get(KEY_USER, ""); }
    public static String getPassword() { return Config.get(KEY_PASS, ""); }

    /** Called after a successful verification. Saves (username, password). */
    public static void saveCredentials(String username, String password) {
        Config.set(KEY_USER, username);
        Config.set(KEY_PASS, password);
    }

    /** Clear saved credentials (verification failed on a later launch). */
    public static void clearCredentials() {
        Config.set(KEY_USER, "");
        Config.set(KEY_PASS, "");
    }
}
