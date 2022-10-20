package testbridge.network.packets.resultpipe;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.StringCoordinatesPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;
import network.rs485.logisticspipes.SatellitePipe;

@StaticResolve
public class SyncResultNamePacket extends StringCoordinatesPacket {

  public SyncResultNamePacket(int id) {
    super(id);
  }

  @Override
  public ModernPacket template() {
    return new SyncResultNamePacket(getId());
  }

  @Override
  public void writeData(LPDataOutput output) {
    super.writeData(output);
  }

  @Override
  public void readData(LPDataInput input) {
    super.readData(input);
  }

  @Override
  public void processPacket(EntityPlayer player) {
    final LogisticsTileGenericPipe pipe = getPipe(player.world, LTGPCompletionCheck.PIPE);
    if (pipe == null || pipe.pipe == null) { return; }
    if (pipe.pipe instanceof SatellitePipe) {
      ((SatellitePipe) pipe.pipe).setSatellitePipeName(getString());
    }
  }
}