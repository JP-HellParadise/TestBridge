package testbridge.network.packets.gui;

import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.tuples.Pair;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SubGuiScreen;

import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

import testbridge.client.gui.GuiSatelliteSelect;
import testbridge.client.popup.GuiSelectResultPopup;
import testbridge.helpers.AECustomGui;

@StaticResolve
public class ProvideSatResultListPacket extends ModernPacket {

  private List<Pair<String, UUID>> uuidList;
  private List<String> stringList;

  public ProvideSatResultListPacket(int id) {
    super(id);
  }

  @Override
  public void readData(LPDataInput input) {
    super.readData(input);
    uuidList = input.readArrayList(input1 -> new Pair<>(input1.readUTF(), input1.readUUID()));
    stringList = input.readArrayList(LPDataInput::readUTF);
  }

  @Override
  public void writeData(LPDataOutput output) {
    super.writeData(output);
    output.writeCollection(uuidList, (output1, object) -> {
      output1.writeUTF(object.getValue1());
      output1.writeUUID(object.getValue2());
    });
    output.writeCollection(stringList, LPDataOutput::writeUTF);
  }

  @Override
  public void processPacket(EntityPlayer player) {
    if (Minecraft.getMinecraft().currentScreen instanceof LogisticsBaseGuiScreen) {
      SubGuiScreen subGUI = ((LogisticsBaseGuiScreen) Minecraft.getMinecraft().currentScreen).getSubGui();
      if (subGUI instanceof GuiSelectResultPopup) {
        ((GuiSelectResultPopup) subGUI).handleResultList(uuidList);
      }
    } else if (Minecraft.getMinecraft().currentScreen instanceof AECustomGui) {
      AECustomGui thisGUI = ((AECustomGui) Minecraft.getMinecraft().currentScreen);
      if (thisGUI instanceof GuiSatelliteSelect) {
        ((GuiSatelliteSelect) thisGUI).handleSatList(stringList);
      }
    }
  }

  @Override
  public ModernPacket template() {
    return new ProvideSatResultListPacket(getId());
  }

  public ProvideSatResultListPacket setUuidList(List<Pair<String, UUID>> uuidList) {
    this.uuidList = uuidList;
    return this;
  }

  public ProvideSatResultListPacket setStringList(List<String> stringList) {
    this.stringList = stringList;
    return this;
  }
}
