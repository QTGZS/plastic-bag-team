package com.nyxclient.verify.auth;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Enumeration;

/**
 * Machine code: SHA-256 of 「os.name|os.arch|os.version|MAC|CPU|BIOS」.
 */
public final class MachineCode {
    private static String cached;

    public static String get() {
        if (cached != null) return cached;
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(osName()).append('|');
            sb.append(osArch()).append('|');
            sb.append(osVersion()).append('|');
            sb.append(getMac()).append('|');
            sb.append(getCpuId()).append('|');
            sb.append(getBiosId());
        } catch (Throwable t) {
            sb.append("fallback");
        }
        cached = sha256(sb.toString());
        return cached;
    }

    private static String osName()  { return prop("os.name"); }
    private static String osArch()  { return prop("os.arch"); }
    private static String osVersion(){ return prop("os.version"); }
    private static String prop(String k) { return System.getProperty(k, ""); }

    // ── MAC ──
    private static String getMac() {
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            while (nets.hasMoreElements()) {
                NetworkInterface nif = nets.nextElement();
                if (nif.isLoopback() || nif.isVirtual() || !nif.isUp()) continue;
                byte[] mac = nif.getHardwareAddress();
                if (mac != null) {
                    StringBuilder m = new StringBuilder();
                    for (byte b : mac) m.append(String.format("%02x", b));
                    return m.toString();
                }
            }
        } catch (Throwable ignored) {}
        return "no-mac";
    }

    // ── CPU ID ──
    private static String getCpuId() {
        StringBuilder s = new StringBuilder();
        s.append("cores=").append(Runtime.getRuntime().availableProcessors()).append('|');

        // Windows: env var PROCESSOR_IDENTIFIER — e.g. "Intel64 Family 6 Model 158 Stepping 13"
        String env = System.getenv("PROCESSOR_IDENTIFIER");
        if (env != null && !env.isEmpty()) {
            s.append("win_cpu=").append(env);
            return s.toString();
        }

        // Linux: /proc/cpuinfo
        String cpuinfo = readFile("/proc/cpuinfo");
        if (cpuinfo != null) {
            for (String key : new String[]{"model name", "cpu family", "model", "stepping", "cpu cores"}) {
                String v = grepFirst(cpuinfo, key);
                if (v != null) s.append(key).append('=').append(v.trim()).append('|');
            }
            return s.toString();
        }

        // macOS
        String macCpu = exec("sysctl -n machdep.cpu.brand_string");
        if (macCpu != null) { s.append("mac_cpu=").append(macCpu); return s.toString(); }

        s.append("cpu_unknown");
        return s.toString();
    }

    // ── BIOS ID ──
    private static String getBiosId() {
        // Linux DMI sysfs (不需要 root）
        for (String p : new String[]{
                "/sys/class/dmi/id/bios_version",
                "/sys/devices/virtual/dmi/id/bios_version"
        }) {
            String v = readFile(p);
            if (v != null && !v.isEmpty()) return "bios=" + v.trim();
        }

        // Windows
        if (osName().toLowerCase().contains("win")) {
            String w = exec("wmic bios get serialnumber /format:value");
            if (w != null) {
                for (String line : w.split("\n")) {
                    if (line.startsWith("SerialNumber="))
                        return "bios_serial=" + line.substring(13).trim();
                }
            }
            return "bios_win_unknown";
        }

        // macOS
        String macBios = exec("sysctl -n hw.model");
        if (macBios != null) return "mac_model=" + macBios.trim();

        return "bios_unknown";
    }

    // ── helpers ──
    private static String readFile(String path) {
        try { return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8); } catch (Exception e) { return null; }
    }

    private static String exec(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) { if (!line.trim().isEmpty()) { if (sb.length()>0) sb.append('\n'); sb.append(line); } }
                return sb.toString();
            }
        } catch (Exception e) { return null; }
    }

    private static String grepFirst(String text, String key) {
        for (String line : text.split("\n")) {
            if (line.startsWith(key + ":")) {
                int idx = line.indexOf(':');
                return idx >= 0 ? line.substring(idx + 1).trim() : null;
            }
        }
        return null;
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) { return "err"; }
    }
}
