package net.jp.hellparadise.testbridge.client.gui;

import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.client.gui.implementations.GuiUpgradeable;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketConfigButton;
import appeng.core.sync.packets.PacketSwitchGuis;
import java.io.IOException;
import net.jp.hellparadise.testbridge.container.ContainerCraftingManager;
import net.jp.hellparadise.testbridge.helpers.interfaces.ICraftingManagerHost;
import net.jp.hellparadise.testbridge.integration.modules.appliedenergistics2.AE2Module;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import org.lwjgl.input.Mouse;

@SuppressWarnings("unused") // Handle by AE2
public class GuiCraftingManager extends GuiUpgradeable {

    private GuiTabButton priority;
    private GuiTabButton satellite;
    private GuiImgButton BlockMode;

    public GuiCraftingManager(final InventoryPlayer ip, final ICraftingManagerHost te) {
        super(new ContainerCraftingManager(ip, te));
        this.ySize = 200;
    }

    @Override
    protected void addButtons() {
        this.priority = new GuiTabButton(
            this.guiLeft + 154,
            this.guiTop,
            2 + 4 * 16,
            GuiText.Priority.getLocal(),
            this.itemRender);
        this.buttonList.add(this.priority);

        this.satellite = new GuiTabButton(
            this.guiLeft + 133,
            this.guiTop,
            AE2Module.SATELLITE_BUS_SRC.stack(1),
            I18n.format("gui.crafting_manager.satellite"),
            this.itemRender);
        this.buttonList.add(this.satellite);

        this.BlockMode = new GuiImgButton(this.guiLeft - 18, this.guiTop, Settings.BLOCK, YesNo.NO);
        this.buttonList.add(this.BlockMode);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        if (this.BlockMode != null) {
            this.BlockMode.set(((ContainerCraftingManager) this.cvb).getBlockingMode());
        }

        this.fontRenderer
            .drawString(I18n.format("item.appliedenergistics2.multi_part.craftingmanager_part.name"), 8, 6, 4210752);

        this.fontRenderer.drawString(GuiText.Patterns.getLocal(), 8, 6 + 18, 4210752);
    }

    @Override
    protected String getBackground() {
        return "guis/crafting_manager.png";
    }

    @Override
    protected void actionPerformed(final GuiButton btn) throws IOException {
        super.actionPerformed(btn);

        final boolean backwards = Mouse.isButtonDown(1);

        if (btn == this.priority) {
            NetworkHandler.instance()
                .sendToServer(new PacketSwitchGuis(GuiBridge.GUI_PRIORITY));
        }

        if (btn == this.satellite) {
            NetworkHandler.instance()
                .sendToServer(new PacketSwitchGuis(AE2Module.GUI_SATSELECT));
        }

        if (btn == this.BlockMode) {
            NetworkHandler.instance()
                .sendToServer(new PacketConfigButton(this.BlockMode.getSetting(), backwards));
        }
    }
}
