package testbridge.network.packets.pipehandler;

import lombok.Getter;
import lombok.Setter;

import net.minecraft.entity.player.EntityPlayer;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.StringCoordinatesPacket;
import logisticspipes.network.exception.TargetNotFoundException;
import logisticspipes.pipes.SatelliteNamingResult;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;

import network.rs485.logisticspipes.SatellitePipe;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

import testbridge.part.PartSatelliteBus;
import testbridge.pipes.ResultPipe;

@StaticResolve
public class TB_SetNamePacket extends StringCoordinatesPacket {

  @Getter
  @Setter
  private int side;

  public TB_SetNamePacket(int id) {
    super(id);
  }

  @Override
  public void processPacket(EntityPlayer player) {
    String newName = getString();
    SatelliteNamingResult result = null;
    if (newName.trim().isEmpty()) {
      result = SatelliteNamingResult.BLANK_NAME;
    } else {
      SatellitePipe progress = null;
      try {
        LogisticsTileGenericPipe pipe = this.getPipe(player.getEntityWorld(), LTGPCompletionCheck.PIPE);
        if (pipe.pipe instanceof ResultPipe) {
          progress = (SatellitePipe) pipe.pipe;
        }
      } catch (TargetNotFoundException e) {
        IPart iPart = getTileAs(player.getEntityWorld(), IPartHost.class).getPart(AEPartLocation.fromOrdinal(getSide()));
        if (iPart instanceof PartSatelliteBus) {
          progress = (SatellitePipe) iPart;
        }
      }

      if (progress != null) {
        if (progress.getSatellitesOfType().stream().anyMatch(it -> it.getSatellitePipeName().equals(newName))) {
          result = SatelliteNamingResult.DUPLICATE_NAME;
        } else {
          result = SatelliteNamingResult.SUCCESS;
          progress.setSatellitePipeName(newName);
          progress.updateWatchers();
          progress.ensureAllSatelliteStatus();
          progress.getContainer().markDirty();
        }
      }
    }

    if (result != null) {
      MainProxy.sendPacketToPlayer(PacketHandler.getPacket(TB_SetNameResult.class).setResult(result).setNewName(getString()), player);
    }
  }

  @Override
  public void writeData(LPDataOutput output) {
    super.writeData(output);
    output.writeInt(side);
  }

  @Override
  public void readData(LPDataInput input) {
    super.readData(input);
    side = input.readInt();
  }

  @Override
  public ModernPacket template() {
    return new TB_SetNamePacket(getId());
  }
}
