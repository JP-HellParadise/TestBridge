package net.jp.hellparadise.testbridge.client.gui;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import logisticspipes.utils.gui.GuiGraphics;

import net.jp.hellparadise.testbridge.container.ContainerCraftingManager;
import net.jp.hellparadise.testbridge.helpers.interfaces.ICraftingManagerHost;
import net.jp.hellparadise.testbridge.integration.modules.appliedenergistics2.AE2Module;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

import org.lwjgl.input.Mouse;

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
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.handleButtonVisibility();

        GuiGraphics
            .drawGuiBackGround(mc, guiLeft, guiTop + ySize - 200, guiLeft + 177, guiTop + ySize - 16, zLevel, true);

        for (int i = 0; i < 3; i++) {
            for (int column = 0; column < 9; ++column) {
                GuiGraphics.drawSlotBackground(mc, guiLeft + 8 + column * 18 - 1, guiTop + 36 + i * 18 - 1);
            }
        }

        GuiGraphics.drawPlayerInventoryBackground(mc, guiLeft + 8, guiTop + ySize - 98);
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
                .sendToServer(new PacketSwitchGuis(AE2Module.GUI_SATELLITESELECT));
        }

        if (btn == this.BlockMode) {
            NetworkHandler.instance()
                .sendToServer(new PacketConfigButton(this.BlockMode.getSetting(), backwards));
        }
    }

    @Override
    public List<Rectangle> getJEIExclusionArea() {
        return new ArrayList<>();
    }
}
