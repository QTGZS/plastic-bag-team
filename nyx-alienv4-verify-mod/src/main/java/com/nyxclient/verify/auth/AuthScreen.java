package com.nyxclient.verify.auth;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pre-launch authentication window (Swing).
 * Shown by MainMixin BEFORE Minecraft's game window is created.
 * Default language is English; the user can switch at runtime.
 *
 * On a definitive verification failure the game must crash -> we return false
 * and the mixin throws, aborting launch.
 */
public final class AuthScreen {

    private static volatile boolean launched = false;

    /** Block until the user is verified (true) or the game must crash (false). */
    public static boolean showAndWait() {
        if (launched) return true;
        launched = true;
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean ok = new AtomicBoolean(false);
        SwingUtilities.invokeLater(() -> build(latch, ok));
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return ok.get();
    }

    private static void build(CountDownLatch latch, AtomicBoolean ok) {
        JFrame frame = new JFrame(Lang.t("title"));
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setSize(420, 360);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(6, 0, 6, 0);
        g.gridx = 0; g.gridy = 0;

        JLabel title = new JLabel(Lang.t("title"), SwingConstants.CENTER);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        g.gridwidth = 2;
        panel.add(title, g);

        g.gridy++;
        JLabel subtitle = new JLabel(Lang.t("subtitle"), SwingConstants.CENTER);
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        subtitle.setForeground(Color.GRAY);
        panel.add(subtitle, g);

        // language selector
        g.gridy++; g.gridwidth = 1;
        JLabel langLabel = new JLabel(Lang.t("lang"));
        panel.add(langLabel, g);
        JComboBox<String> langBox = new JComboBox<>(Lang.LANG_NAMES);
        int cur = indexOf(Lang.lang());
        langBox.setSelectedIndex(cur < 0 ? 0 : cur);
        g.gridx = 1;
        panel.add(langBox, g);

        g.gridx = 0; g.gridy++;
        JLabel userLabel = new JLabel(Lang.t("user"));
        panel.add(userLabel, g);
        JTextField userField = new JTextField();
        g.gridx = 1;
        panel.add(userField, g);

        g.gridx = 0; g.gridy++;
        JLabel passLabel = new JLabel(Lang.t("pass"));
        panel.add(passLabel, g);
        JPasswordField passField = new JPasswordField();
        g.gridx = 1;
        panel.add(passField, g);

        JCheckBox showPass = new JCheckBox(Lang.t("showpass"));
        g.gridx = 1; g.gridy++;
        panel.add(showPass, g);
        showPass.addActionListener(e ->
                passField.setEchoChar(showPass.isSelected() ? (char) 0 : '•'));

        // debug mode toggle (persisted to nyx-auth.properties)
        JCheckBox debugChk = new JCheckBox("Debug 调试模式");
        debugChk.setSelected(Config.debug());
        g.gridx = 1; g.gridy++;
        panel.add(debugChk, g);
        debugChk.addActionListener(e -> Config.setDebug(debugChk.isSelected()));

        JButton loginBtn = new JButton(Lang.t("login"));
        g.gridx = 0; g.gridy++; g.gridwidth = 2;
        g.fill = GridBagConstraints.NONE;
        panel.add(loginBtn, g);

        JLabel status = new JLabel("", SwingConstants.CENTER);
        status.setForeground(Color.RED);
        g.gridy++;
        panel.add(status, g);

        frame.add(panel, BorderLayout.CENTER);

        // language switch re-builds labels
        langBox.addActionListener(e -> {
            Lang.setLang(Lang.LANGS[langBox.getSelectedIndex()]);
            frame.dispose();
            build(latch, ok);
        });

        loginBtn.addActionListener(e -> {
            final String u = userField.getText().trim();
            final String p = new String(passField.getPassword());
            if (u.isEmpty() || p.isEmpty()) {
                status.setText(Lang.t("fail") + " empty");
                return;
            }
            loginBtn.setEnabled(false);
            status.setText(Lang.t("logging"));
            new Thread(() -> {
                AuthClient.Result r = AuthClient.verify(u, p);
                // 调试模式：用信息框显示每次访问的 URL / 状态码 / 响应内容
                if (Config.debug() && r.debugTrace != null && !r.debugTrace.isEmpty()) {
                    showDebug(r.debugTrace);
                }
                if (r.success) {
                    SwingUtilities.invokeLater(() -> {
                        frame.dispose();
                        ok.set(true);
                        latch.countDown();
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        loginBtn.setEnabled(true);
                        if ("NETWORK_ERROR".equals(r.code)) {
                            status.setText(Lang.t("neterr"));
                        } else {
                            // definitive failure -> crash the game
                            status.setText(Lang.t("fail") + r.message);
                            frame.dispose();
                            ok.set(false);
                            latch.countDown();
                        }
                    });
                }
            }).start();
        });

        frame.setVisible(true);
    }

    private static int indexOf(String lang) {
        for (int i = 0; i < Lang.LANGS.length; i++)
            if (Lang.LANGS[i].equals(lang)) return i;
        return -1;
    }

    /** Show the raw API response(s) in a modal info box (debug mode). */
    private static void showDebug(String text) {
        try {
            final String content = text.length() > 8000
                    ? text.substring(0, 8000) + "\n... (truncated)"
                    : text;
            SwingUtilities.invokeAndWait(() -> {
                JTextArea ta = new JTextArea(content);
                ta.setEditable(false);
                ta.setRows(24);
                ta.setColumns(62);
                ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                ta.setLineWrap(true);
                ta.setWrapStyleWord(true);
                JScrollPane sp = new JScrollPane(ta);
                JOptionPane.showMessageDialog(null, sp,
                        "Debug · API Response", JOptionPane.INFORMATION_MESSAGE);
            });
        } catch (Exception ignored) {}
    }

    /** Fallback when AWT is headless (no display server). */
    public static boolean consoleFallback() {
        java.io.Console console = System.console();
        if (console == null) {
            System.err.println("[Nyx AlienV4] No display and no console. Cannot authenticate.");
            return false;
        }
        System.out.println("=== " + Lang.t("title") + " ===");
        System.out.println(Lang.t("subtitle"));
        String u = console.readLine(Lang.t("user") + ": ");
        String p = new String(console.readPassword(Lang.t("pass") + ": "));
        AuthClient.Result r = AuthClient.verify(u, p);
        if (r.success) {
            System.out.println("✓ " + r.message);
            return true;
        } else {
            System.out.println("✗ " + Lang.t("fail") + (r.message != null ? r.message : r.code));
            return false;
        }
    }
}
