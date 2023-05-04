package net.jp.hellparadise.testbridge.mixins.logisticspipes.devenv;

import logisticspipes.LogisticsPipes;

import net.minecraftforge.fml.relauncher.FMLLaunchHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LogisticsPipes.class)
public abstract class DebugModeEnable {

    // Enable debug mode
    @Inject(method = "isDEBUG", at = @At(value = "HEAD"), cancellable = true, remap = false)
    private static void toggleDebug(CallbackInfoReturnable<Boolean> cir) {
        if (FMLLaunchHandler.isDeobfuscatedEnvironment()) cir.setReturnValue(true);
    }

}
