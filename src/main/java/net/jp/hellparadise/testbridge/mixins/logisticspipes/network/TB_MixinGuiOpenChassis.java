package net.jp.hellparadise.testbridge.mixins.logisticspipes.network;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.packets.gui.GuiOpenChassis;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import net.jp.hellparadise.testbridge.network.guis.pipe.CMGuiProvider;
import net.jp.hellparadise.testbridge.pipes.PipeCraftingManager;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiOpenChassis.class, remap = false)
public abstract class TB_MixinGuiOpenChassis extends CoordinatesPacket {

    // stub
    public TB_MixinGuiOpenChassis(int id) {
        super(id);
    }

    @Inject(method = "processPacket", at = @At(value = "HEAD", ordinal = 0), cancellable = true, remap = false)
    public void isCraftingManager(EntityPlayer player, CallbackInfo ci) {
        LogisticsTileGenericPipe pipe = this
            .getPipe(player.getEntityWorld(), CoordinatesPacket.LTGPCompletionCheck.PIPE);
        if (pipe.pipe instanceof PipeCraftingManager cm) {
            NewGuiHandler.getGui(CMGuiProvider.class)
                .setBufferUpgrade(cm.hasBufferUpgrade())
                .setBlockingMode(cm.getBlockingByInt())
                .setContainerConnected(
                    cm.getAvailableAdjacent()
                        .inventories()
                        .isEmpty())
                .setSlot(LogisticsModule.ModulePositionType.IN_PIPE)
                .setPositionInt(0)
                .setPosX(getPosX())
                .setPosY(getPosY())
                .setPosZ(getPosZ())
                .open(player);
            ci.cancel();
        }
    }
}
