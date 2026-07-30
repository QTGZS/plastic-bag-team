package com.nyxclient.verify.mixin;

import com.nyxclient.verify.auth.AuthScreen;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects at the very HEAD of Minecraft's main entry point, BEFORE the game
 * window (and MinecraftClient) is constructed. This guarantees the auth window
 * shows before the game window appears. If verification fails, we throw so the
 * game crashes by design.
 */
@Mixin(Main.class)
public abstract class MainMixin {

    @Inject(method = "main", at = @At("HEAD"))
    private static void onMainHead(String[] args, CallbackInfo ci) {
        boolean verified = AuthScreen.showAndWait();
        if (!verified) {
            throw new RuntimeException(
                    "[Nyx AlienV4] Verification failed. Game crashed by design.");
        }
    }
}
