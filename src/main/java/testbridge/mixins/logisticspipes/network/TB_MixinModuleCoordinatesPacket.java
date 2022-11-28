package testbridge.mixins.logisticspipes.network;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

import testbridge.pipes.PipeCraftingManager;

@Mixin(ModuleCoordinatesPacket.class)
public abstract class TB_MixinModuleCoordinatesPacket {
  @Shadow
  private int positionInt;

  @Inject(method = "getLogisticsModule", at = @At(value = "NEW", target = "logisticspipes/network/exception/TargetNotFoundException"
      , shift = At.Shift.AFTER, ordinal = 5), cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT)
  private <T> void isCraftingManager(EntityPlayer player, Class<T> clazz, CallbackInfoReturnable<T> cir, LogisticsTileGenericPipe pipe) {
    LogisticsModule module;
    if (pipe.pipe instanceof PipeCraftingManager) {
      module = ((PipeCraftingManager) pipe.pipe).getSubModule(positionInt);
      if (module != null && clazz.isAssignableFrom(module.getClass())) {
        cir.setReturnValue((T) module);
      }
    }
  }
}
