package testbridge.network;

import logisticspipes.network.abstractpackets.CoordinatesPacket;

import logisticspipes.network.abstractpackets.ModernPacket;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import net.minecraft.entity.player.EntityPlayer;

import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@ToString
public class SetIDPacket extends CoordinatesPacket {

  @Getter
  @Setter
  private String pid;
  @Getter
  @Setter
  private int id;
  @Getter
  @Setter
  private int side;

  public SetIDPacket(int id) {
    super(id);
  }

  @Override
  public void writeData(LPDataOutput output) {
    super.writeData(output);
    output.writeUTF(pid);
    output.writeInt(id);
    output.writeInt(side);
  }
  @Override
  public void readData(LPDataInput input) {
    super.readData(input);
    pid = input.readUTF();
    id = input.readInt();
    side = input.readInt();
  }

  @Override
  public void processPacket(EntityPlayer entityPlayer) {

  }

  @Override
  public ModernPacket template() {
    return new SetIDPacket(getId());
  }

}
