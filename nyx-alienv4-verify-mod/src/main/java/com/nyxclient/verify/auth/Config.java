package com.nyxclient.verify.auth;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * Persisted mod config. Stored next to the game directory as nyx-auth.properties.
 * Default API base points at the production domain yh-team.org.
 */
public final class Config {
    private static final File FILE = new File("nyx-auth.properties");
    private static final Properties props = new Properties();

    public static final String DEFAULT_API_BASE = "https://play.simpfun.cn:14639";

    static {
        props.setProperty("api.base.url", DEFAULT_API_BASE);
        props.setProperty("api.verify.path", "/api/v1/auth/verify");
        props.setProperty("client.type", "AlienV4");
        props.setProperty("lang", "en_us");
        props.setProperty("debug", "false");
        load();
    }

    public static void load() {
        if (FILE.exists()) {
            try (FileReader r = new FileReader(FILE)) {
                props.load(r);
            } catch (IOException ignored) {}
        } else {
            save();
        }
    }

    public static void save() {
        try (FileWriter w = new FileWriter(FILE)) {
            props.store(w, "Nyx AlienV4 Auth Config");
        } catch (IOException ignored) {}
    }

    public static String get(String key) {
        return props.getProperty(key);
    }

    public static String get(String key, String def) {
        return props.getProperty(key, def);
    }

    public static void set(String key, String value) {
        props.setProperty(key, value);
        save();
    }

    public static String apiBase() {
        return get("api.base.url", DEFAULT_API_BASE);
    }

    public static String clientType() {
        return get("client.type", "AlienV4");
    }

    public static String lang() {
        return get("lang", "en_us");
    }

    public static void setLang(String lang) {
        set("lang", lang);
    }

    public static boolean debug() {
        return "true".equalsIgnoreCase(get("debug", "false"));
    }

    public static void setDebug(boolean on) {
        set("debug", on ? "true" : "false");
    }
}
