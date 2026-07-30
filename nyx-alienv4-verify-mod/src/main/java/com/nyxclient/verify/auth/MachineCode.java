package com.nyxclient.verify.auth;

import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Enumeration;

/**
 * Generates a stable per-machine code from hardware &amp; system info.
 * The code is a SHA-256 hex string and stays constant across launches on the same PC.
 */
public final class MachineCode {
    private static String cached;

    public static String get() {
        if (cached != null) return cached;
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(System.getProperty("os.name", ""));
            sb.append('|');
            sb.append(System.getProperty("os.arch", ""));
            sb.append('|');
            sb.append(System.getProperty("user.name", ""));
            sb.append('|');
            sb.append(System.getProperty("os.version", ""));
            sb.append('|');
            sb.append(getMac());
            sb.append('|');
            sb.append(totalMemory());
        } catch (Throwable t) {
            sb.append("fallback");
        }
        cached = sha256(sb.toString());
        return cached;
    }

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

    private static long totalMemory() {
        try {
            return Runtime.getRuntime().maxMemory();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            return "err";
        }
    }
}
