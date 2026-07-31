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
        // 有已保存的凭据？静默重新验证（用户名+密码+当前机器码）
        if (AuthSession.hasSavedCredentials()) {
            String u = AuthSession.getUsername();
            String p = AuthSession.getPassword();
            AuthClient.Result r = AuthClient.verify(u, p);
            if (r.success) {
                showWelcome(u);
                return true;
            }
            // 验证失败（密码改/过期/机器换），清空凭据，弹出窗口让玩家重新登录
            AuthSession.clearCredentials();
        }
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
                if (r.success) {
                    // 保存凭据，下次启动自动验证
                    AuthSession.saveCredentials(u, p);
                    SwingUtilities.invokeLater(() -> {
                        frame.dispose();
                        ok.set(true);
                        latch.countDown();
                    });
                } else if ("NETWORK_ERROR".equals(r.code)) {
                    // 网络问题：提示并允许重试
                    SwingUtilities.invokeLater(() -> {
                        loginBtn.setEnabled(true);
                        status.setText(Lang.t("neterr"));
                    });
                } else if ("INVALID_CREDENTIALS".equals(r.code)) {
                    // 密码错误：弹错误框通知，允许重试（不崩溃）
                    showError(Lang.t("wrongpw"));
                    SwingUtilities.invokeLater(() -> {
                        loginBtn.setEnabled(true);
                        status.setText(Lang.t("wrongpw"));
                    });
                } else {
                    // 其它明确失败（未购买/机器码不符/过期/禁用）：弹框后崩溃
                    showError(Lang.t("fail") + r.message);
                    SwingUtilities.invokeLater(() -> {
                        frame.dispose();
                        ok.set(false);
                        latch.countDown();
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

    /** Show an error dialog (modal). Called from the worker thread. */
    private static void showError(String text) {
        try {
            SwingUtilities.invokeAndWait(() ->
                    JOptionPane.showMessageDialog(null, text,
                            "RusherHack Client", JOptionPane.ERROR_MESSAGE));
        } catch (Exception ignored) {}
    }

    /** Show the welcome info box (cached session is valid). */
    private static void showWelcome(String username) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                String msg = Lang.t("welcome");
                if (username != null && !username.isEmpty()) {
                    msg += "\n" + Lang.t("welcome_as") + ": " + username;
                }
                JOptionPane.showMessageDialog(null, msg,
                        "RusherHack Client", JOptionPane.INFORMATION_MESSAGE);
            });
        } catch (Exception ignored) {}
    }

    /** Fallback when AWT is headless (no display server). */
    public static boolean consoleFallback() {
        java.io.Console console = System.console();
        if (console == null) {
            System.err.println("[Nyx RusherHack] No display and no console. Cannot authenticate.");
            return false;
        }
        System.out.println("=== " + Lang.t("title") + " ===");
        System.out.println(Lang.t("subtitle"));
        String u = console.readLine(Lang.t("user") + ": ");
        String p = new String(console.readPassword(Lang.t("pass") + ": "));
        AuthClient.Result r = AuthClient.verify(u, p);
        if (r.success) {
            AuthSession.saveCredentials(u, p);
            System.out.println("✓ " + r.message);
            return true;
        } else {
            System.out.println("✗ " + Lang.t("fail") + (r.message != null ? r.message : r.code));
            return false;
        }
    }
}
