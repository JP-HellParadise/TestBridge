package net.jp.hellparadise.testbridge.client.gui;

import appeng.api.AEApi;
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
import net.jp.hellparadise.testbridge.core.TestBridge;
import net.jp.hellparadise.testbridge.helpers.interfaces.ae2.AccessorApiParts;
import net.jp.hellparadise.testbridge.helpers.interfaces.ae2.ICraftingManagerHost;
import net.jp.hellparadise.testbridge.network.packets.implementation.CManagerMenuSwitch;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;

/**
 * Handled by AE2
 */
@SuppressWarnings("unused")
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
                ((AccessorApiParts) AEApi.instance()
                        .definitions()
                        .parts()).satelliteBus()
                        .maybeStack(1)
                        .orElse(ItemStack.EMPTY),
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
            .drawString(I18n.format("item.appliedenergistics2.multi_part.crafting_manager.name"), 8, 6, 4210752);

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
            TestBridge.getNetwork().sendToServer(
                    new CManagerMenuSwitch()
                            .setPos(((ContainerCraftingManager) cvb).getHost().getBlockPos())
                            .setSide(((ContainerCraftingManager) cvb).getHost().sideOrdinal())
                            .isSatSwitchMenu(true));
        }

        if (btn == this.BlockMode) {
            NetworkHandler.instance()
                .sendToServer(new PacketConfigButton(this.BlockMode.getSetting(), backwards));
        }
    }
}
