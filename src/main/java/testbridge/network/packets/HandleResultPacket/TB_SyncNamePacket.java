package testbridge.network.packets.HandleResultPacket;

import lombok.Getter;
import lombok.Setter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import appeng.tile.networking.TileCableBus;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.StringCoordinatesPacket;
import logisticspipes.network.exception.TargetNotFoundException;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

import testbridge.part.PartSatelliteBus;
import testbridge.pipes.ResultPipe;

@StaticResolve
public class TB_SyncNamePacket extends StringCoordinatesPacket {

  @Getter
  @Setter
  private int side;

  public TB_SyncNamePacket(int id) {
    super(id);
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
    return new TB_SyncNamePacket(getId());
  }
}