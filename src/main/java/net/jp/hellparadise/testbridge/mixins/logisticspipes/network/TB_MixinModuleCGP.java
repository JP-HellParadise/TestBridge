package net.jp.hellparadise.testbridge.mixins.logisticspipes.network;

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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

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
    public <T> boolean disableWrongError(Operation<Boolean> original, World ignoredWorld, Class<T> ignoredClazz,
        @Local LogisticsTileGenericPipe pipe) {
        if (pipe.pipe instanceof PipeCraftingManager) {
            return false;
        } else {
            return original.call();
        }
    }

    @Inject(
        method = "getLogisticsModule",
        at = @At(value = "RETURN", ordinal = 1, shift = At.Shift.BEFORE),
        cancellable = true,
        remap = false)
    public <T> void redirectToCmModule(World world, Class<T> clazz, CallbackInfoReturnable<T> cir,
        @Local LogisticsTileGenericPipe pipe) {
        if (pipe.pipe instanceof PipeCraftingManager) {
            cir.setReturnValue((T) ((PipeCraftingManager) pipe.pipe).getSubModule(this.positionInt));
        }
    }
}
