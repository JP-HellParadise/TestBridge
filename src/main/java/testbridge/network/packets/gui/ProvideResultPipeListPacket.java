package testbridge.network.packets.gui;

import testbridge.gui.popup.GuiSelectResultPopup;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.gui.ProvideSatellitePipeListPacket;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SubGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

public class ProvideResultPipeListPacket extends ProvideSatellitePipeListPacket {

  public ProvideResultPipeListPacket(int id) {
    super(id);
  }

  @Override
  public void processPacket(EntityPlayer player) {
    if (Minecraft.getMinecraft().currentScreen instanceof LogisticsBaseGuiScreen) {
      SubGuiScreen subGUI = ((LogisticsBaseGuiScreen) Minecraft.getMinecraft().currentScreen).getSubGui();
      if (subGUI instanceof GuiSelectResultPopup) {
        ((GuiSelectResultPopup) subGUI).handleResultList(getList());
      }
    }
  }

  @Override
  public ModernPacket template() {
    return new ProvideResultPipeListPacket(getId());
  }
}
