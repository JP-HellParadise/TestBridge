package testbridge.network.abstractpackets;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.network.abstractpackets.StringCoordinatesPacket;
import logisticspipes.utils.StaticResolve;

import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public abstract class CustomCoordinatesPacket extends StringCoordinatesPacket {

  @Getter
  @Setter
  private int side;

  @Getter
  @Setter
  private int id;

  public CustomCoordinatesPacket(int id) {
    super(id);
  }

  @Override
  public void writeData(LPDataOutput output) {
    super.writeData(output);
    output.writeInt(side);
    output.writeInt(id);
  }

  @Override
  public void readData(LPDataInput input) {
    super.readData(input);
    setSide(input.readInt());
    setId(input.readInt());
  }
}
