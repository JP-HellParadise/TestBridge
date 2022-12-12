package testbridge.client.gui;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.client.gui.implementations.GuiUpgradeable;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.gui.widgets.GuiToggleButton;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketConfigButton;
import appeng.core.sync.packets.PacketSwitchGuis;

import logisticspipes.utils.gui.GuiGraphics;

import testbridge.container.ContainerCraftingManager;
import testbridge.core.AE2Plugin;
import testbridge.core.TB_ItemHandlers;
import testbridge.helpers.interfaces.ICraftingManagerHost;

public class GuiCraftingManager extends GuiUpgradeable {
  private GuiTabButton priority;
  private GuiTabButton satellite;
  private GuiImgButton BlockMode;
  private GuiToggleButton interfaceMode;

  public GuiCraftingManager(final InventoryPlayer ip, final ICraftingManagerHost te) {
    super(new ContainerCraftingManager(ip, te));
    this.ySize = 200;
  }

  @Override
  protected void addButtons() {
    this.priority = new GuiTabButton(this.guiLeft + 154, this.guiTop, 2 + 4 * 16, GuiText.Priority.getLocal(), this.itemRender);
    this.buttonList.add(this.priority);

    this.satellite = new GuiTabButton(this.guiLeft + 133, this.guiTop, TB_ItemHandlers.satelliteBus,
        I18n.format("gui.crafting_manager.satellite") , this.itemRender);
    this.buttonList.add(this.satellite);

    this.BlockMode = new GuiImgButton(this.guiLeft - 18, this.guiTop, Settings.BLOCK, YesNo.NO);
    this.buttonList.add(this.BlockMode);

    this.interfaceMode = new GuiToggleButton(this.guiLeft - 18, this.guiTop + 26, 84, 85, GuiText.InterfaceTerminal.getLocal(), GuiText.InterfaceTerminalHint.getLocal());
    this.buttonList.add(this.interfaceMode);
  }

  @Override
  public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
    if (this.interfaceMode != null) {
      this.interfaceMode.setState(((ContainerCraftingManager) this.cvb).getInterfaceTerminalMode() == YesNo.YES);
    }

    this.fontRenderer.drawString(I18n.format("item.appliedenergistics2.multi_part.craftingmanager_part.name"), 8, 6, 4210752);

    this.fontRenderer.drawString(GuiText.Patterns.getLocal(), 8, 6 + 18, 4210752);

  }

  @Override
  public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
    this.handleButtonVisibility();

    GuiGraphics.drawGuiBackGround(mc, guiLeft, guiTop + ySize - 200, guiLeft + 177, guiTop + ySize - 16, zLevel, true);

    for(int i = 0; i < 3; i++) {
      for(int column = 0; column < 9; ++column) {
        GuiGraphics.drawSlotBackground(mc, guiLeft + 8 + column * 18 - 1, guiTop + 36 + i * 18 - 1);
      }
    }

    GuiGraphics.drawPlayerInventoryBackground(mc, guiLeft + 8, guiTop + ySize - 98 );
  }

  @Override
  protected void actionPerformed(final GuiButton btn) throws IOException {
    super.actionPerformed(btn);

    final boolean backwards = Mouse.isButtonDown(1);

    if (btn == this.priority) {
      NetworkHandler.instance().sendToServer(new PacketSwitchGuis(GuiBridge.GUI_PRIORITY));
    }

    if (btn == this.satellite) {
      NetworkHandler.instance().sendToServer(new PacketSwitchGuis(AE2Plugin.GUI_SATELLITESELECT));
    }

    if (btn == this.interfaceMode) {
      NetworkHandler.instance().sendToServer(new PacketConfigButton(Settings.INTERFACE_TERMINAL, backwards));
    }

    if (btn == this.BlockMode) {
//      final BlockingMode newMode = blockingModeOverlay.write(EnumProperty::next);
//      BlockMode.displayString = TextUtil.translate(PREFIX + "blocking." + newMode.toString().toLowerCase());
    }
  }

  @Override
  public List<Rectangle> getJEIExclusionArea() {
    return new ArrayList<>();
  }
}
