package testbridge.network.packets.resultpipe;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.StringCoordinatesPacket;
import logisticspipes.pipes.SatelliteNamingResult;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;

import testbridge.pipes.ResultPipe;

@StaticResolve
public class ResultSetNamePacket extends StringCoordinatesPacket {

  public ResultSetNamePacket(int id) {
    super(id);
  }

  @Override
  public void processPacket(EntityPlayer player) {
    LogisticsTileGenericPipe pipe = this.getPipe(player.getEntityWorld(), LTGPCompletionCheck.PIPE);
    if (pipe == null || pipe.pipe == null) { return; }
    String newName = getString();
    SatelliteNamingResult result = null;
    if (newName.trim().isEmpty()) {
      result = SatelliteNamingResult.BLANK_NAME;
    } else if (pipe.pipe instanceof ResultPipe) {
      final ResultPipe resultPipe = (ResultPipe) pipe.pipe;
      if (resultPipe.getSatellitesOfType().stream().anyMatch(it -> it.getSatellitePipeName().equals(newName))) {
        result = SatelliteNamingResult.DUPLICATE_NAME;
      } else {
        result = SatelliteNamingResult.SUCCESS;
        resultPipe.setSatellitePipeName(newName);
        resultPipe.updateWatchers();
        resultPipe.ensureAllSatelliteStatus();
      }
    }
    if (result != null) {
      MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SetNameResult.class).setResult(result).setNewName(getString()), player);
    }
  }

  @Override
  public ModernPacket template() {
    return new ResultSetNamePacket(getId());
  }
}

