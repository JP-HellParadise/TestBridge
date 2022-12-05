package testbridge.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;

import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.gui.DummyContainer;

import testbridge.part.PartSatelliteBus;
import testbridge.pipes.ResultPipe;
import testbridge.gui.GuiResultPipe;

public class GuiHandler extends logisticspipes.network.GuiHandler{

  @Override
  public Object getServerGuiElement(int ID, EntityPlayer player, World world, final int x, final int y, final int z) {
    // Satellite Bus checker
    if(ID >= 100 && ID < 120){
      AEPartLocation side = AEPartLocation.fromOrdinal( ID - 100 );
      TileEntity TE = world.getTileEntity( new BlockPos( x, y, z ) );
      if( TE instanceof IPartHost ) {
        IPart part = ( (IPartHost) TE ).getPart( side );
        if(part instanceof PartSatelliteBus){
          return new DummyContainer(player.inventory, null);
        }
      }
      return null;
    }

    TileEntity tile = null;
    if (y != -1) {
      tile = world.getTileEntity(new BlockPos(x, y, z));
    }
    LogisticsTileGenericPipe pipe = null;
    if (tile instanceof LogisticsTileGenericPipe) {
      pipe = (LogisticsTileGenericPipe) tile;
    }

    DummyContainer dummy;
    int xOffset;
    int yOffset;

    if (ID < 100 && ID > 0) {
      switch (ID) {
        case GuiIDs.GUI_ResultPipe_ID:
          if (pipe != null && pipe.pipe instanceof ResultPipe) {
            return new DummyContainer(player.inventory, null);
          }

        default:
          break;
      }
    }
    return null;
  }

  @Override
  public Object getClientGuiElement(int ID, EntityPlayer player, final World world, int x, int y, int z) {
    // Satellite Bus checker
    if(ID >= 100 && ID < 120){
      AEPartLocation side = AEPartLocation.fromOrdinal( ID - 100 );
      TileEntity TE = world.getTileEntity( new BlockPos( x, y, z ) );
      if( TE instanceof IPartHost) {
        IPart part = ((IPartHost) TE).getPart( side );
        if(part instanceof PartSatelliteBus) {
          return new GuiResultPipe<>((PartSatelliteBus) part, "gui.satellite.");
        }
      }
      return null;
    }

    TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
    LogisticsTileGenericPipe pipe = null;
    if (tile instanceof LogisticsTileGenericPipe) {
      pipe = (LogisticsTileGenericPipe) tile;
    }

    if (ID < 100 && ID > 0) {
      switch (ID) {

        case GuiIDs.GUI_ResultPipe_ID:
          if (pipe != null && pipe.pipe instanceof ResultPipe) {
            return new GuiResultPipe<>(((ResultPipe) pipe.pipe), "gui.result.");
          }
          return null;

        default:
          break;
      }
    }
    return null;
  }

}

