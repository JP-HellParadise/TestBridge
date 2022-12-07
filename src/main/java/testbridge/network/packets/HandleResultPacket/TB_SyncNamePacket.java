package testbridge.network.packets.HandleResultPacket;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import appeng.tile.networking.TileCableBus;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.exception.TargetNotFoundException;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

import testbridge.network.abstractpackets.CustomCoordinatesPacket;
import testbridge.part.PartSatelliteBus;
import testbridge.pipes.ResultPipe;

@StaticResolve
public class TB_SyncNamePacket extends CustomCoordinatesPacket {

  public TB_SyncNamePacket(int id) {
    super(id);
  }

  @Override
  public ModernPacket template() {
    return new TB_SyncNamePacket(getId());
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
    try {
      LogisticsTileGenericPipe pipe = getPipe(player.world, LTGPCompletionCheck.PIPE);
      if (pipe == null || pipe.pipe == null) {
        return;
      }
      if (pipe.pipe instanceof ResultPipe) {
        ((ResultPipe) pipe.pipe).setSatellitePipeName(getString());
      }
    } catch (TargetNotFoundException e) {
      TileEntity TE = getTileAs(player.getEntityWorld(), IPartHost.class).getTile();
      if (TE instanceof TileCableBus) {
        IPart iPart = ((TileCableBus) TE).getPart(AEPartLocation.fromOrdinal(getSide()));
        if (iPart instanceof PartSatelliteBus) {
          ((PartSatelliteBus) iPart).setSatellitePipeName(getString());
        }
      }
    }
  }
}