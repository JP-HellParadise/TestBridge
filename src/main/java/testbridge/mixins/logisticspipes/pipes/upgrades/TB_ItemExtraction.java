package testbridge.mixins.logisticspipes.pipes.upgrades;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.upgrades.ItemExtractionUpgrade;

import testbridge.pipes.ResultPipe;

@Mixin(ItemExtractionUpgrade.class)
public abstract class TB_ItemExtraction {
  @Inject(method = "isAllowedForPipe", at = @At(value = "RETURN"), cancellable = true)
  private void isResultPipe(CoreRoutedPipe pipe, CallbackInfoReturnable<Boolean> cir) {
    if (pipe instanceof ResultPipe) cir.setReturnValue(true);
  }
}
