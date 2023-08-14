package net.jp.hellparadise.testbridge.client.gui;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.manager.GuiCreationContext;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.GuiSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.value.sync.SyncHandlers;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Column;
import com.cleanroommc.modularui.widgets.layout.Row;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.jp.hellparadise.testbridge.core.TestBridge;
import net.jp.hellparadise.testbridge.helpers.ListSyncHandler;
import net.jp.hellparadise.testbridge.helpers.interfaces.ae2.ICraftingManagerHost;
import net.jp.hellparadise.testbridge.network.packets.implementation.CManagerMenuSwitch;
import net.minecraft.client.entity.EntityPlayerSP;

/**
 * Specific for ME Crafting Manager
 */
public interface GuiSatelliteSelect extends IGuiHolder, ICraftingManagerHost {

    @Override
    default ModularPanel buildUI(GuiCreationContext creationContext, GuiSyncManager syncManager, boolean isClient) {
        // Initial sync
        syncManager.syncValue("satName", SyncHandlers.string(this::getSatelliteName, this::setSatelliteName))
                .syncValue("satList", new ListSyncHandler<>(
                        this::getAvailableSat,
                        ((buffer, value) -> buffer.writeByteArray(value.getBytes(StandardCharsets.UTF_8))),
                        buffer -> new String(buffer.readByteArray(Integer.MAX_VALUE), StandardCharsets.UTF_8)));

        ListSyncHandler<String> satListSync = (ListSyncHandler<String>) syncManager.getSyncHandler("satList");
        StringSyncValue satNameSync = (StringSyncValue) syncManager.getSyncHandler("satName");

        // Initialize stuff
        Map<String, ? extends IWidget> map = new Object2ObjectOpenHashMap<>();
        Map<? extends IWidget, String> map_reverse = new Object2ObjectOpenHashMap<>();
        ListWidget<String, ?, ?> listWidget = new ListWidget<>(map::get, map_reverse::get).coverChildrenWidth()
                .flex(flex -> flex.startDefaultMode().width(80).height(140).endDefaultMode());

        // Initial panel
        ModularPanel panel = ModularPanel.defaultPanel("satellite_select")
                .child(new Column().height(20).padding(7)
                .child(IKey.lang("item.testbridge.item_package.name").asWidget())
                .child(new Row().topRel(1.F)
                    .child(new Column().widthRel(0.5F)
                        .child(IKey.str("Current select:").asWidget())
                        .child(new TextFieldWidget().align(Alignment.Center).width(80).height(18).topRel(1.5F)
                            .value(SyncHandlers.string(this::getSatelliteName, null))))
                    .child(new Column().widthRel(0.5F).child(listWidget))));

        // Register everything that needed
        syncManager.addCloseListener(entityPlayer -> { // Switch back to old gui in a complicated way ever :trolled:
            if (entityPlayer instanceof EntityPlayerSP)
                TestBridge.getNetwork().sendToServer(new CManagerMenuSwitch()
                        .setPos(creationContext.getBlockPos())
                        .setSide(sideOrdinal()));
        });

        listWidget.child(new ButtonWidget<>().size(80, 18)
                .overlay(IKey.str("<None>"))
                .onMousePressed(mouseButton1 -> {
                    satNameSync.setValue("");
                    return true;
                }));

        satListSync.setChangeListener(() -> {
            satListSync.getValue().forEach(value -> listWidget.addChild(
                    new ButtonWidget<>().size(80, 18)
                            .overlay(IKey.str(value))
                            .onMousePressed(mouseButton1 -> {
                                satNameSync.setValue(value);
                                return true;
                            }), -1));
            WidgetTree.resize(panel);
        });

        return panel;
    }
}
