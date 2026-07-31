package com.nyxclient.verify.mixin;

import com.nyxclient.verify.auth.AuthScreen;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.GraphicsEnvironment;

/**
 * Injects at the very HEAD of Minecraft's main entry point, BEFORE the game
 * window (and MinecraftClient) is constructed. This guarantees the auth window
 * shows before the Minecraft window appears. If verification fails, we throw so
 * the game crashes by design.
 */
@Mixin(Main.class)
public abstract class MainMixin {

    @Inject(method = "main", at = @At("HEAD"))
    private static void onMainHead(String[] args, CallbackInfo ci) {
        // Some launchers start with -Djava.awt.headless=true. Override it here
        // before any AWT class is initialized so the Swing auth window can show.
        System.setProperty("java.awt.headless", "false");

        boolean verified;
        if (GraphicsEnvironment.isHeadless()) {
            // No display available (e.g. CI / container) -> use console fallback.
            verified = AuthScreen.consoleFallback();
        } else {
            verified = AuthScreen.showAndWait();
        }

        if (!verified) {
            throw new RuntimeException(
                    "[Nyx RusherHack] Verification failed. Game crashed by design.");
        }
    }
}
