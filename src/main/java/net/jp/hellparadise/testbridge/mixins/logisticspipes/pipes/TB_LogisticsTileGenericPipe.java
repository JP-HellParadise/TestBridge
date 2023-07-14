package net.jp.hellparadise.testbridge.mixins.logisticspipes.pipes;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.sync.GuiSyncHandler;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = LogisticsTileGenericPipe.class, remap = false)
public abstract class TB_LogisticsTileGenericPipe implements IGuiHolder {

    @Shadow
    public CoreUnroutedPipe pipe;

    @Override
    public void buildSyncHandler(GuiSyncHandler guiSyncHandler, EntityPlayer entityPlayer) {
        if (pipe instanceof IGuiHolder) ((IGuiHolder) pipe).buildSyncHandler(guiSyncHandler, entityPlayer);
    }

    @Override
    public ModularScreen createClientGui(EntityPlayer entityPlayer) {
        return pipe instanceof IGuiHolder ? ((IGuiHolder) pipe).createClientGui(entityPlayer) : null;
    }
}
