package net.jp.hellparadise.testbridge.mixins.logisticspipes.network;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import net.jp.hellparadise.testbridge.pipes.PipeCraftingManager;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("unchecked")
@Mixin(value = ModuleCoordinatesGuiProvider.class, remap = false)
public abstract class TB_MixinModuleCGP extends CoordinatesGuiProvider {

    @Shadow
    private int positionInt;

    public TB_MixinModuleCGP(int id) {
        super(id);
    }

    @WrapOperation(
        method = "getLogisticsModule",
        at = @At(value = "INVOKE", target = "Llogisticspipes/LogisticsPipes;isDEBUG()Z", ordinal = 1),
        remap = false)
    private <T> boolean disableWrongError(Operation<Boolean> original, World ignoredWorld, Class<T> ignoredClazz,
        @Local LogisticsTileGenericPipe pipe) {
        return !(pipe.pipe instanceof PipeCraftingManager) && original.call();
    }

    @Inject(
        method = "getLogisticsModule",
        at = @At(value = "RETURN", ordinal = 1, shift = At.Shift.BEFORE),
        cancellable = true,
        remap = false)
    private <T> void redirectToCmModule(World world, Class<T> clazz, CallbackInfoReturnable<T> cir,
        @Local LogisticsTileGenericPipe pipe) {
        if (pipe.pipe instanceof PipeCraftingManager cmPipe) {
            cir.setReturnValue((T) cmPipe.getSubModule(this.positionInt));
        }
    }
}
