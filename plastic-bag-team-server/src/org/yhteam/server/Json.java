package org.yhteam.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON parser &amp; stringifier — no external dependencies.
 */
public final class Json {

    @SuppressWarnings("unchecked")
    public static String getString(Object json, String key) {
        if (json instanceof Map) {
            Object v = ((Map<String, Object>) json).get(key);
            return v == null ? null : String.valueOf(v);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static boolean getBool(Object json, String key, boolean def) {
        if (json instanceof Map) {
            Object v = ((Map<String, Object>) json).get(key);
            if (v instanceof Boolean) return (Boolean) v;
            if (v != null) return Boolean.parseBoolean(v.toString());
        }
        return def;
    }

    @SuppressWarnings("unchecked")
    public static long getLong(Object json, String key, long def) {
        if (json instanceof Map) {
            Object v = ((Map<String, Object>) json).get(key);
            if (v instanceof Number) return ((Number) v).longValue();
            if (v != null) {
                try { return Long.parseLong(v.toString()); } catch (Exception ignored) {}
            }
        }
        return def;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getAccountList(Object json) {
        if (json instanceof Map) {
            Object v = ((Map<String, Object>) json).get("accounts");
            if (v instanceof List) return (List<Map<String, Object>>) v;
        }
        return new ArrayList<>();
    }

    // ── parser ──
    public static Object parse(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        return new Parser(s.trim()).parse();
    }

    static final class Parser {
        final String s;
        int i;

        Parser(String s) { this.s = s; }

        void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

        Object parse() {
            ws();
            if (i >= s.length()) return null;
            char c = s.charAt(i);
            if (c == '{') return parseObj();
            if (c == '[') return parseArr();
            if (c == '"') return parseStr();
            if (c == 't' || c == 'f') return parseBool();
            if (c == 'n') { i += 4; return null; }
            return parseNum();
        }

        Map<String, Object> parseObj() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            ws();
            if (i < s.length() && s.charAt(i) == '}') { i++; return m; }
            while (true) {
                ws();
                String key = parseStr();
                ws();
                i++; // skip ':'
                ws();
                m.put(key, parse());
                ws();
                if (i < s.length() && s.charAt(i) == ',') { i++; continue; }
                if (i < s.length() && s.charAt(i) == '}') { i++; break; }
                break;
            }
            return m;
        }

        List<Object> parseArr() {
            List<Object> a = new ArrayList<>();
            i++;
            ws();
            if (i < s.length() && s.charAt(i) == ']') { i++; return a; }
            while (true) {
                ws();
                a.add(parse());
                ws();
                if (i < s.length() && s.charAt(i) == ',') { i++; continue; }
                if (i < s.length() && s.charAt(i) == ']') { i++; break; }
                break;
            }
            return a;
        }

        String parseStr() {
            StringBuilder sb = new StringBuilder();
            i++; // skip opening "
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '\\') {
                    if (i >= s.length()) break;
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        case 'r': sb.append('\r'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'u':
                            if (i + 4 <= s.length()) {
                                sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                                i += 4;
                            }
                            break;
                        default: sb.append(e);
                    }
                } else if (c == '"') break;
                else sb.append(c);
            }
            return sb.toString();
        }

        Object parseNum() {
            int start = i;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (Character.isDigit(c) || c == '-' || c == '.' || c == 'e' || c == 'E' || c == '+')
                    i++;
                else break;
            }
            String num = s.substring(start, i);
            try {
                if (num.contains(".") || num.contains("e") || num.contains("E"))
                    return Double.parseDouble(num);
                return Long.parseLong(num);
            } catch (Exception e) {
                return num;
            }
        }

        Boolean parseBool() {
            if (s.startsWith("true", i)) { i += 4; return true; }
            i += 5;
            return false;
        }
    }

    // ── stringify ──
    public static String stringify(Object o) { return s(o); }

    static String s(Object o) {
        if (o == null) return "null";
        if (o instanceof String) return q((String) o);
        if (o instanceof Number || o instanceof Boolean) return o.toString();
        if (o instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> e : ((Map<String, Object>) o).entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append(q(e.getKey())).append(":").append(s(e.getValue()));
            }
            return sb.append("}").toString();
        }
        if (o instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object v : (List<?>) o) {
                if (!first) sb.append(",");
                first = false;
                sb.append(s(v));
            }
            return sb.append("]").toString();
        }
        if (o instanceof Object[]) return s(Arrays.asList((Object[]) o));
        return q(o.toString());
    }

    public static String q(String str) {
        StringBuilder sb = new StringBuilder("\"");
        for (int k = 0; k < str.length(); k++) {
            char c = str.charAt(k);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\t': sb.append("\\t"); break;
                case '\r': sb.append("\\r"); break;
                default:
                    if (c < 0x20)
                        sb.append(String.format("\\u%04x", (int) c));
                    else
                        sb.append(c);
            }
        }
        return sb.append("\"").toString();
    }
}
