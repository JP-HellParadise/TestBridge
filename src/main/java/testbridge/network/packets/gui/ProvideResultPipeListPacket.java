package testbridge.network.packets.gui;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.tuples.Pair;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SubGuiScreen;

import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

import testbridge.gui.popup.GuiSelectResultPopup;

@StaticResolve
public class ProvideResultPipeListPacket extends ModernPacket {

  @Getter
  @Setter
  private List<Pair<String, UUID>> list;

  public ProvideResultPipeListPacket(int id) {
    super(id);
  }

  @Override
  public void readData(LPDataInput input) {
    super.readData(input);
    list = input.readArrayList(input1 -> new Pair<>(input1.readUTF(), input1.readUUID()));
  }

  @Override
  public void writeData(LPDataOutput output) {
    super.writeData(output);
    output.writeCollection(list, (output1, object) -> {
      output1.writeUTF(object.getValue1());
      output1.writeUUID(object.getValue2());
    });
  }

  @Override
  public void processPacket(EntityPlayer player) {
    if (Minecraft.getMinecraft().currentScreen instanceof LogisticsBaseGuiScreen) {
      SubGuiScreen subGUI = ((LogisticsBaseGuiScreen) Minecraft.getMinecraft().currentScreen).getSubGui();
      if (subGUI instanceof GuiSelectResultPopup) {
        ((GuiSelectResultPopup) subGUI).handleResultList(list);
      }
    }
  }

  @Override
  public ModernPacket template() {
    return new ProvideResultPipeListPacket(getId());
  }
}
