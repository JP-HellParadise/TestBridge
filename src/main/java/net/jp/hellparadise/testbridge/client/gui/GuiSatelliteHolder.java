package net.jp.hellparadise.testbridge.client.gui;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.layout.CrossAxisAlignment;
import com.cleanroommc.modularui.manager.GuiCreationContext;
import com.cleanroommc.modularui.manager.GuiInfos;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.Tooltip;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.value.sync.GuiSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.layout.Column;
import com.cleanroommc.modularui.widgets.layout.Row;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.jp.hellparadise.testbridge.client.TB_Textures;
import net.jp.hellparadise.testbridge.helpers.interfaces.SatelliteInfo;
import net.jp.hellparadise.testbridge.network.guis.GuiHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Specific for Satellite Bus/Result Pipe
 */
public interface GuiSatelliteHolder extends IGuiHolder, SatelliteInfo {

    @Override
    default ModularPanel buildUI(GuiCreationContext creationContext, GuiSyncManager syncManager, boolean isClient) {
        final String[] response = {""};
        ModularPanel panel = ModularPanel.defaultPanel("satellite", 140, 47);
        Widget<? extends Widget<?>> indicator = new Widget<>();

        panel.child(new Column().height(20).paddingTop(7)
            .child(IKey.lang("gui.satellite.GuiName").asWidget())
            .child(new Row().topRel(1.0f).left(7)
                .child(new Column().coverChildren().crossAxisAlignment(CrossAxisAlignment.START)
                    .child(new TextFieldWidget() {
                            @Override
                            public void onRemoveFocus(GuiContext context) {
                                boolean isExist = isExist(this.getText());
                                response[0] = IKey.lang(isExist ?
                                        "gui.popup.select.failed" : "gui.popup.select.success").get();
                                indicator.overlay((isExist ?
                                        TB_Textures.UI_CROSS : TB_Textures.UI_TICK)
                                        .asIcon());
                                if (!isExist) super.onRemoveFocus(context);
                            }
                        }
                        .value(new StringSyncValue(this::getSatelliteName, this::setSatelliteName) {
                            @Override
                            public void readOnServer(int id, PacketBuffer buf) {
                                super.readOnServer(id, buf);
                                ensureAllSatelliteStatus();
                                if (getContainer() != null) getContainer().markDirty();
                            }
                        })
                        .size(106, 18)))
                .child(
                    new Column().coverChildrenHeight()
                        .width(21)
                        .crossAxisAlignment(CrossAxisAlignment.END)
                        .child(indicator.size(18)
                            .tooltip(tooltip -> {
                                tooltip.addLine(IKey.dynamic(() -> response[0]));
                                tooltip.pos(Tooltip.Pos.RIGHT);
                            })))));
        return panel;
    }

    default void openUI(
            @Nonnull EntityPlayerMP player,
            @Nonnull World world,
            @Nonnull BlockPos blockPos,
            @Nullable EnumFacing facing) {
        if (facing != null) {
            GuiHandler.getAE2PartUiInfo(facing)
                .open(player, world, blockPos);
        } else {
            GuiInfos.TILE_ENTITY.open(player, world, blockPos);
        }
    }
}
