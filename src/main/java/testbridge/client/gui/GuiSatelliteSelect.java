package testbridge.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;

import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketSwitchGuis;

import logisticspipes.network.PacketHandler;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.IGuiAccess;
import logisticspipes.utils.gui.GuiGraphics;
import logisticspipes.utils.gui.TextListDisplay;

import network.rs485.logisticspipes.util.TextUtil;

import testbridge.container.ContainerSatelliteSelect;
import testbridge.helpers.AECustomGui;
import testbridge.helpers.interfaces.ICraftingManagerHost;
import testbridge.network.packets.TB_CustomAE2Packet;

public class GuiSatelliteSelect extends AECustomGui implements IGuiAccess {
  static String GUI_LANG_KEY = "gui.popup.selectsatellite.";
  private final Consumer<String> handleResult;
  private List<String> satList = new ArrayList<>();
  private final TextListDisplay textList;
  private GuiButton select;
  private GuiButton exit;
  private GuiButton unset;
  private GuiButton up;
  private GuiButton down;
  private GuiBridge OriginalGui;

  public GuiSatelliteSelect(final InventoryPlayer inventoryPlayer, final ICraftingManagerHost te) {
    super(new ContainerSatelliteSelect(inventoryPlayer, te), 0, 0);
    this.handleResult = satName -> MainProxy.sendPacketToServer(PacketHandler.getPacket(TB_CustomAE2Packet.class).setKey("CMSatellite.Setting").setValue(satName).setSetting(true).setBlockPos(te.getBlockPos()));
    this.textList = new TextListDisplay(this, 6, 16, 6, 30, 12, new TextListDisplay.List() {

      @Override
      public int getSize() {
        return satList.size();
      }

      @Override
      public String getTextAt(int index) {
        return satList.get(index);
      }

      @Override
      public int getTextColor(int index) {
        return 0xFFFFFF;
      }
    });
    MainProxy.sendPacketToServer(PacketHandler.getPacket(TB_CustomAE2Packet.class).setKey("CMSatellite.Opening").setBlockPos(te.getBlockPos()));
  }

  @Override
  public void initGui() {
    super.initGui();
    this.buttonList.add(this.select = new GuiButton(0, xCenter + 16, bottom - 27, 50, 10, TextUtil.translate(GUI_LANG_KEY + "select")));
    this.buttonList.add(this.exit = new GuiButton(1, xCenter + 16, bottom - 15, 50, 10, TextUtil.translate(GUI_LANG_KEY + "exit")));
    this.buttonList.add(this.unset = new GuiButton(2, xCenter - 66, bottom - 27, 50, 10, TextUtil.translate(GUI_LANG_KEY + "unset")));
    this.buttonList.add(this.up = new GuiButton(3, xCenter - 12, bottom - 27, 25, 10, "/\\"));
    this.buttonList.add(this.down = new GuiButton(4, xCenter - 12, bottom - 15, 25, 10, "\\/"));

    final ContainerSatelliteSelect con = ((ContainerSatelliteSelect) this.inventorySlots);
    this.OriginalGui = con.getCMHost().getGuiBridge();
  }

  protected void mouseClicked(int xCoord, int yCoord, int btn) throws IOException {
    textList.mouseClicked(xCoord, yCoord, btn);
    super.mouseClicked(xCoord, yCoord, btn);
  }

  @Override
  public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
  }

  @Override
  public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
    GuiGraphics.drawGuiBackGround(mc, guiLeft, guiTop, right, bottom, zLevel, true);
    mc.fontRenderer.drawStringWithShadow(TextUtil.translate(GUI_LANG_KEY + "title"), xCenter - (mc.fontRenderer.getStringWidth(TextUtil.translate(GUI_LANG_KEY + "title")) / 2f), guiTop + 6, 0xFFFFFF);

    textList.renderGuiBackground(mouseX, mouseY);
  }

  public final void handleMouseInput() throws IOException {
    int wheel = org.lwjgl.input.Mouse.getDWheel() / 120;
    if (wheel == 0) {
      super.handleMouseInput();
    }
    if (wheel < 0) {
      textList.scrollUp();
    } else if (wheel > 0) {
      textList.scrollDown();
    }
  }

  @Override
  protected void actionPerformed(final GuiButton btn) throws IOException {
    super.actionPerformed(btn);

    if (btn == this.select) {
      int selected = textList.getSelected();
      if (selected >= 0) {
        handleResult.accept(satList.get(selected));
        NetworkHandler.instance().sendToServer(new PacketSwitchGuis(this.OriginalGui));
      }
    }

    if (btn == this.up) {
      textList.scrollUp();
    }

    if (btn == this.down) {
      textList.scrollDown();
    }

    if (btn == this.exit) {
      NetworkHandler.instance().sendToServer(new PacketSwitchGuis(this.OriginalGui));
    }

    if (btn == this.unset) {
      handleResult.accept("");
      NetworkHandler.instance().sendToServer(new PacketSwitchGuis(this.OriginalGui));
    }
  }

  public void handleSatList(List<String> list) {
    satList = list;
  }
}
