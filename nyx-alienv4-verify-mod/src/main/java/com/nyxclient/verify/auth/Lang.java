package com.nyxclient.verify.auth;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal i18n. Default UI language is English; switchable at runtime.
 */
public final class Lang {
    public static final String[] LANGS = {"en_us", "zh_cn", "ja_jp", "ru_ru"};
    public static final String[] LANG_NAMES = {"English", "简体中文", "日本語", "Русский"};

    private static String current = Config.lang();
    private static final Map<String, Map<String, String>> TABLE = new HashMap<>();

    static {
        Map<String, String> en = new HashMap<>();
        en.put("title", "AlienV4 Authentication");
        en.put("subtitle", "Please verify your AlienV4 license to continue.");
        en.put("user", "Username");
        en.put("pass", "Password");
        en.put("login", "Login");
        en.put("lang", "Language");
        en.put("logging", "Verifying...");
        en.put("neterr", "Network error. Please check your connection and retry.");
        en.put("fail", "Verification failed: ");
        en.put("crash", "Verification failed. The game will now exit.");
        en.put("wrongpw", "Wrong username or password. Please try again.");
        en.put("showpass", "Show password");
        TABLE.put("en_us", en);

        Map<String, String> zh = new HashMap<>();
        zh.put("title", "AlienV4 验证");
        zh.put("subtitle", "请验证您的 AlienV4 授权以继续。");
        zh.put("user", "用户名");
        zh.put("pass", "密码");
        zh.put("login", "登录");
        zh.put("lang", "语言");
        zh.put("logging", "验证中...");
        zh.put("neterr", "网络错误，请检查连接后重试。");
        zh.put("fail", "验证失败：");
        zh.put("crash", "验证未通过，游戏即将退出。");
        zh.put("wrongpw", "用户名或密码错误，请重试。");
        zh.put("showpass", "显示密码");
        TABLE.put("zh_cn", zh);

        Map<String, String> ja = new HashMap<>();
        ja.put("title", "AlienV4 認証");
        ja.put("subtitle", "続行するには AlienV4 ライセンスを認証してください。");
        ja.put("user", "ユーザー名");
        ja.put("pass", "パスワード");
        ja.put("login", "ログイン");
        ja.put("lang", "言語");
        ja.put("logging", "認証中...");
        ja.put("neterr", "ネットワークエラー。接続を確認してください。");
        ja.put("fail", "認証失敗：");
        ja.put("crash", "認証に失敗しました。ゲームを終了します。");
        ja.put("wrongpw", "ユーザー名またはパスワードが違います。再度お試しください。");
        ja.put("showpass", "パスワードを表示");
        TABLE.put("ja_jp", ja);

        Map<String, String> ru = new HashMap<>();
        ru.put("title", "AlienV4 Авторизация");
        ru.put("subtitle", "Подтвердите лицензию AlienV4 для продолжения.");
        ru.put("user", "Имя пользователя");
        ru.put("pass", "Пароль");
        ru.put("login", "Войти");
        ru.put("lang", "Язык");
        ru.put("logging", "Проверка...");
        ru.put("neterr", "Ошибка сети. Проверьте подключение.");
        ru.put("fail", "Ошибка проверки: ");
        ru.put("crash", "Проверка не пройдена. Игра будет закрыта.");
        ru.put("wrongpw", "Неверное имя пользователя или пароль. Попробуйте снова.");
        ru.put("showpass", "Показать пароль");
        TABLE.put("ru_ru", ru);
    }

    public static void setLang(String lang) {
        current = lang;
        Config.setLang(lang);
    }

    public static String lang() {
        return current;
    }

    public static String t(String key) {
        Map<String, String> m = TABLE.getOrDefault(current, TABLE.get("en_us"));
        return m.getOrDefault(key, TABLE.get("en_us").getOrDefault(key, key));
    }
}
