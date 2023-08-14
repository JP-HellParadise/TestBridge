package net.jp.hellparadise.testbridge.mixins.appliedenergistics2.accessor;

import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.me.ItemRepo;
import net.jp.hellparadise.testbridge.helpers.interfaces.ae2.AccessorGuiMEMonitorable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GuiMEMonitorable.class, remap = false)
public abstract class TB_GuiMEMonitorable implements AccessorGuiMEMonitorable {

    @Final
    @Shadow(remap = false)
    protected ItemRepo repo;

    @Override
    public ItemRepo getRepo() {
        return repo;
    }
}
