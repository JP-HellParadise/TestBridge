package net.jp.hellparadise.testbridge.mixins.logisticspipes.network;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import logisticspipes.network.exception.TargetNotFoundException;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

import net.jp.hellparadise.testbridge.pipes.PipeCraftingManager;
import net.minecraft.entity.player.EntityPlayer;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

@SuppressWarnings("UnusedDeclaration, unchecked")
@Mixin(value = ModuleCoordinatesPacket.class, remap = false)
public abstract class TB_MixinModuleCoordinatesPacket {

    @Shadow
    private int positionInt;

    @WrapOperation(
        method = "getLogisticsModule",
        at = @At(value = "JUMP", ordinal = 3, opcode = Opcodes.IFNE, shift = At.Shift.BEFORE),
        remap = false)
    public <T> boolean skipChassisChecker(Object original, Operation<Boolean> operator, EntityPlayer player,
        Class<T> clazz) {
        if (original instanceof PipeCraftingManager) {
            return true;
        } else {
            return operator.call(original);
        }
    }

    @Inject(
        method = "getLogisticsModule",
        at = @At(
            value = "FIELD",
            target = "logisticspipes/pipes/basic/LogisticsTileGenericPipe.pipe:Llogisticspipes/pipes/basic/CoreUnroutedPipe;",
            ordinal = 4,
            shift = At.Shift.BEFORE),
        cancellable = true,
        remap = false)
    public <T> void redirectCmModule(EntityPlayer player, Class<T> clazz, CallbackInfoReturnable<T> cir,
        @Local LogisticsTileGenericPipe pipe) {
        if (pipe.pipe instanceof PipeCraftingManager) {
            LogisticsModule module = ((PipeCraftingManager) pipe.pipe).getSubModule(this.positionInt);
            if (module != null) {
                if (!clazz.isAssignableFrom(module.getClass())) {
                    throw new TargetNotFoundException(
                        "Couldn't find " + clazz.getName() + ", found " + module.getClass(),
                        this);
                } else {
                    cir.setReturnValue((T) module);
                }
            } else {
                throw new TargetNotFoundException("Couldn't find " + clazz.getName(), this);
            }
        }
    }
}
