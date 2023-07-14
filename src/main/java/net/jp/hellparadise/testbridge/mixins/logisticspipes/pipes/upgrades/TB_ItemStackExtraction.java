package net.jp.hellparadise.testbridge.mixins.logisticspipes.pipes.upgrades;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.Arrays;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.upgrades.ItemStackExtractionUpgrade;
import net.jp.hellparadise.testbridge.pipes.ResultPipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("UnusedDeclaration")
@Mixin(value = ItemStackExtractionUpgrade.class, remap = false)
public abstract class TB_ItemStackExtraction {

    @Inject(method = "isAllowedForPipe", at = @At(value = "HEAD"), cancellable = true, remap = false)
    private void isResultPipe(CoreRoutedPipe pipe, CallbackInfoReturnable<Boolean> cir) {
        if (pipe instanceof ResultPipe) cir.setReturnValue(true);
    }

    @ModifyReturnValue(method = "getAllowedPipes", at = @At("RETURN"))
    private String[] injectNewPipeInfo(String[] original) {
        return ImmutableList.<String>builder()
            .addAll(Arrays.asList(original))
            .add("result")
            .add("crafting_manager")
            .build()
            .toArray(new String[0]);
    }
}
