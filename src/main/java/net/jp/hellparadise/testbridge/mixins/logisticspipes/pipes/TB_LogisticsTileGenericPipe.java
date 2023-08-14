package net.jp.hellparadise.testbridge.mixins.logisticspipes.pipes;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.manager.GuiCreationContext;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.GuiSyncManager;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = LogisticsTileGenericPipe.class, remap = false)
public abstract class TB_LogisticsTileGenericPipe implements IGuiHolder {

    @Shadow
    public CoreUnroutedPipe pipe;

    @Override
    public ModularPanel buildUI(GuiCreationContext creationContext, GuiSyncManager syncManager, boolean isClient) {
        return pipe instanceof IGuiHolder ? ((IGuiHolder) pipe).buildUI(creationContext, syncManager, isClient) : null;
    }
}
