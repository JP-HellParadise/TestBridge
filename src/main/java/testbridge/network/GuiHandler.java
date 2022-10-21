package testbridge.network;

import testbridge.gui.GuiResultPipe;

import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.gui.DummyContainer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import network.rs485.logisticspipes.SatellitePipe;
import testbridge.pipes.ResultPipe;

public class GuiHandler extends logisticspipes.network.GuiHandler{

  @Override
  public Object getServerGuiElement(int ID, EntityPlayer player, World world, final int x, final int y, final int z) {

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

    if (ID < 110 && ID > 0) {
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
    if (ID == -1) {
      return getClientGuiElement(-100 * 20 + x, player, world, 0, -1, z);
    }

    TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
    LogisticsTileGenericPipe pipe = null;
    if (tile instanceof LogisticsTileGenericPipe) {
      pipe = (LogisticsTileGenericPipe) tile;
    }

    if (ID < 110 && ID > 0) {
      switch (ID) {

        case GuiIDs.GUI_ResultPipe_ID:
          if (pipe != null && pipe.pipe instanceof ResultPipe) {
            return new GuiResultPipe(((ResultPipe) pipe.pipe));
          }
          return null;

        default:
          break;
      }
    }
    return null;
  }

}

