package testbridge.network.packets.pipehandler;

import lombok.Getter;
import lombok.Setter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import appeng.tile.AEBaseTile;
import appeng.tile.networking.TileCableBus;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.StringCoordinatesPacket;
import logisticspipes.network.exception.TargetNotFoundException;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

import testbridge.block.tile.TileEntityCraftingManager;
import testbridge.helpers.interfaces.ICraftingManagerHost;
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
      // No need to check if pipe null since it will throw Exception on LP side
      if (pipe.pipe instanceof ResultPipe) {
        ((ResultPipe) pipe.pipe).setSatellitePipeName(getString());
      }
    } catch (TargetNotFoundException tnfe1) {
      try {
        TileEntity TE = getTileAs(player.getEntityWorld(), IPartHost.class).getTile();
        if (TE instanceof TileCableBus) {
          IPart part = ((TileCableBus) TE).getPart(AEPartLocation.fromOrdinal(getSide()));
          if (part instanceof PartSatelliteBus) {
            ((PartSatelliteBus) part).setSatellitePipeName(getString());
          } else if (part instanceof ICraftingManagerHost) {
            ((ICraftingManagerHost) part).setSatellite(getString());
          }
        }
      } catch (TargetNotFoundException tnfe2) {
        TileEntity TE = getTileAs(player.getEntityWorld(), AEBaseTile.class).getTile();
        if (TE instanceof TileEntityCraftingManager) {
          ((TileEntityCraftingManager) TE).setSatellite(getString());
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