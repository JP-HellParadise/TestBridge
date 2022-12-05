package testbridge.network.packets.HandleResultPacket;

import lombok.Getter;
import lombok.Setter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import appeng.tile.networking.TileCableBus;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.StringCoordinatesPacket;
import logisticspipes.network.exception.TargetNotFoundException;
import logisticspipes.pipes.SatelliteNamingResult;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;

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
      try {
        LogisticsTileGenericPipe pipe = this.getPipe(player.getEntityWorld(), LTGPCompletionCheck.PIPE);
        if (pipe != null && pipe.pipe instanceof ResultPipe) {
          ResultPipe resultPipe = (ResultPipe) pipe.pipe;
          if (resultPipe.getSatellitesOfType().stream().anyMatch(it -> it.getSatellitePipeName().equals(newName))) {
            result = SatelliteNamingResult.DUPLICATE_NAME;
          } else {
            result = SatelliteNamingResult.SUCCESS;
            resultPipe.setSatellitePipeName(newName);
            resultPipe.updateWatchers();
            resultPipe.ensureAllSatelliteStatus();
            pipe.getTile().markDirty();
          }
        }
      } catch (TargetNotFoundException e) {
        TileEntity TE = getTileAs(player.getEntityWorld(), IPartHost.class).getTile();
        if (TE instanceof TileCableBus) {
          IPart iPart = ((TileCableBus) TE).getPart(AEPartLocation.fromOrdinal(side));
          if (iPart instanceof PartSatelliteBus) {
            if (((PartSatelliteBus) iPart).getSatellitesOfType().stream().anyMatch(it -> it.getSatellitePipeName().equals(newName))) {
              result = SatelliteNamingResult.DUPLICATE_NAME;
            } else {
              result = SatelliteNamingResult.SUCCESS;
              ((PartSatelliteBus) iPart).setSatellitePipeName(newName);
              ((PartSatelliteBus) iPart).updateWatchers();
              ((PartSatelliteBus) iPart).ensureAllSatelliteStatus();
              TE.markDirty();
            }
          }
        }
      }
    }
    if (result != null) {
      MainProxy.sendPacketToPlayer(PacketHandler.getPacket(TB_SetNameResult.class).setResult(result).setNewName(getString()), player);
    }
  }

  @Override
  public ModernPacket template() {
    return new TB_SetNamePacket(getId());
  }

  public void writeData(LPDataOutput output) {
    super.writeData(output);
    output.writeInt(this.side);
  }

  public void readData(LPDataInput input) {
    super.readData(input);
    this.side = input.readInt();
  }
}

