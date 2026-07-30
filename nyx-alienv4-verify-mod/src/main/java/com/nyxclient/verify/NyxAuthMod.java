package com.nyxclient.verify;

import com.nyxclient.verify.auth.Config;
import net.fabricmc.api.ClientModInitializer;

/**
 * Entry point. The actual verification happens in MainMixin (before the game
 * window). This initializer just logs that the mod is loaded.
 */
public class NyxAuthMod implements ClientModInitializer {
    public static final String MOD_ID = "nyx-alienv4-verify";

    @Override
    public void onInitializeClient() {
        // no-op: auth is enforced pre-window via MainMixin
        System.out.println("[Nyx AlienV4 Verify] loaded. API base=" + Config.apiBase());
    }
}
