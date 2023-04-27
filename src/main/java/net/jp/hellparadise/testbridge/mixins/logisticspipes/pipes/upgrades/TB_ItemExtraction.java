package net.jp.hellparadise.testbridge.mixins.logisticspipes.pipes.upgrades;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.upgrades.ItemExtractionUpgrade;

import net.jp.hellparadise.testbridge.pipes.ResultPipe;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemExtractionUpgrade.class, remap = false)
public abstract class TB_ItemExtraction {

    @Inject(method = "isAllowedForPipe", at = @At(value = "HEAD"), cancellable = true, remap = false)
    private void isResultPipe(CoreRoutedPipe pipe, CallbackInfoReturnable<Boolean> cir) {
        if (pipe instanceof ResultPipe) cir.setReturnValue(true);
    }
}
