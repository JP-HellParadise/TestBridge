package testbridge.mixins.logisticspipes;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;

import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import testbridge.pipes.PipeCraftingManager;

@Mixin(ModuleCoordinatesGuiProvider.class)
public abstract class TB_MixinModuleCGP extends CoordinatesGuiProvider {
  @Shadow
  private int positionInt;

  public TB_MixinModuleCGP(int id) {
    super(id);
  }

  @Inject(method = "getLogisticsModule", at = @At(value = "RETURN", ordinal = 1, shift = At.Shift.BEFORE), cancellable = true)
  public <T> void test(World world, Class<T> clazz, CallbackInfoReturnable<LogisticsModule> cir) {
    LogisticsTileGenericPipe pipe = this.getTileAs(world, LogisticsTileGenericPipe.class);
    LogisticsModule module;
    if(pipe.pipe instanceof PipeCraftingManager) {
      module = ((PipeCraftingManager)pipe.pipe).getSubModule(this.positionInt);
      cir.setReturnValue(module);
    }
  }
}
