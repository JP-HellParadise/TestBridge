package testbridge.network.packets.pipe;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeItemsSatelliteLogistics;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;

import testbridge.pipes.PipeCraftingManager;
import testbridge.pipes.ResultPipe;

@StaticResolve
public class RequestOrientationPacket extends CoordinatesPacket {

  public RequestOrientationPacket(int id) {
    super(id);
  }

  @Override
  public void processPacket(EntityPlayer player) {
    LogisticsTileGenericPipe pipe = this.getPipe(player.world, LTGPCompletionCheck.PIPE);
    if (pipe.pipe instanceof PipeCraftingManager) {
      MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OrientationPacket.class).setDir(((PipeCraftingManager) pipe.pipe).getPointedOrientation()).setPosX(getPosX()).setPosY(getPosY()).setPosZ(getPosZ()), player);
    } else if (pipe.pipe instanceof ResultPipe) {
      MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OrientationPacket.class).setDir(((ResultPipe) pipe.pipe).getPointedOrientation()).setPosX(getPosX()).setPosY(getPosY()).setPosZ(getPosZ()), player);
    } else if (pipe.pipe instanceof PipeItemsSatelliteLogistics) {
      MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OrientationPacket.class).setDir(((PipeItemsSatelliteLogistics) pipe.pipe).getPointedOrientation()).setPosX(getPosX()).setPosY(getPosY()).setPosZ(getPosZ()), player);
    }
  }

  @Override
  public ModernPacket template() {
    return new RequestOrientationPacket(getId());
  }
}
