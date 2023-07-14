package net.jp.hellparadise.testbridge.client.gui;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.layout.CrossAxisAlignment;
import com.cleanroommc.modularui.manager.GuiInfos;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.Tooltip;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.sync.GuiSyncHandler;
import com.cleanroommc.modularui.sync.SyncHandlers;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Column;
import com.cleanroommc.modularui.widgets.layout.Row;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import net.jp.hellparadise.testbridge.client.TB_Textures;
import net.jp.hellparadise.testbridge.helpers.interfaces.SatelliteInfo;
import net.jp.hellparadise.testbridge.network.guis.GuiHandler;
import net.jp.hellparadise.testbridge.part.PartSatelliteBus;
import net.jp.hellparadise.testbridge.utils.TextUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public interface SatelliteGuiHolder extends IGuiHolder, SatelliteInfo {

    default boolean isAE2Part() {
        return false;
    }

    default void openUI(EntityPlayerMP player) {
        if (isAE2Part()) {
            PartSatelliteBus part = (PartSatelliteBus) this;
            GuiHandler.getCoverUiInfo(
                part.getSide()
                    .getFacing())
                .open(
                    player,
                    part.getTile()
                        .getWorld(),
                    part.getTile()
                        .getPos());
        } else {
            GuiInfos.TILE_ENTITY.open(
                player,
                this.getContainer()
                    .getWorld(),
                this.getContainer()
                    .getPos());
        }
    }

    @Override
    default void buildSyncHandler(GuiSyncHandler guiSyncHandler, EntityPlayer entityPlayer) {
        guiSyncHandler.syncValue(0, SyncHandlers.string(this::getSatelliteName, this::setSatelliteName));
    }

    default ModularPanel createPanel(GuiContext context) {
        context.disableJei();
        final String[] response = {""};
        ModularPanel panel = ModularPanel.defaultPanel(context, 140, 47);
        ButtonWidget<? extends ButtonWidget<?>> indicator = new ButtonWidget<>().size(18)
            .tooltip(tooltip -> {
                tooltip.addLine(IKey.dynamic(() -> response[0]));
                tooltip.pos(Tooltip.Pos.RIGHT);
            });
        panel.child(
            new Column().height(18)
                .child(
                    IKey.lang("gui." + context.screen.getName() + ".GuiName")
                        .asWidget()
                        .align(Alignment.TopCenter)
                        .top(7))
                .child(
                    new Row().coverChildrenHeight()
                        .topRel(1)
                        .left(7)
                        .child(
                            new Column().coverChildrenHeight()
                                .crossAxisAlignment(CrossAxisAlignment.START)
                                .child(new TextFieldWidget() {

                                    @Override
                                    public void onRemoveFocus(GuiContext context) {
                                        if (getSatellitesOfType().stream()
                                            .anyMatch(
                                                it -> it.getSatelliteName()
                                                    .equals(getText()))) {
                                            response[0] = TextUtil.translate("Failed");
                                            indicator.overlay(TB_Textures.UI_CROSS.asIcon());
                                            return;
                                        }
                                        response[0] = TextUtil.translate("Success");
                                        indicator.overlay(TB_Textures.UI_TICK.asIcon());
                                        super.onRemoveFocus(context);
                                    }
                                }.setSynced(0)
                                    .size(106, 18)))
                        .child(
                            new Column().coverChildrenHeight()
                                .width(22)
                                .crossAxisAlignment(CrossAxisAlignment.END)
                                .child(indicator))));
        return panel;
    }
}
