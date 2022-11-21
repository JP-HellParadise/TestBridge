package testbridge.network.packets.pipe;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;

import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import net.minecraft.entity.player.EntityPlayer;
import testbridge.pipes.PipeCraftingManager;

@StaticResolve
public class RequestCMOrientationPacket extends CoordinatesPacket {

  public RequestCMOrientationPacket(int id) {
    super(id);
  }

  @Override
  public void processPacket(EntityPlayer player) {
    LogisticsTileGenericPipe pipe = this.getPipe(player.world, LTGPCompletionCheck.PIPE);
    if (pipe.pipe instanceof PipeCraftingManager) {
      MainProxy.sendPacketToPlayer(PacketHandler.getPacket(CMOrientationPacket.class).setDir(((PipeCraftingManager) pipe.pipe).getPointedOrientation()).setPosX(getPosX()).setPosY(getPosY()).setPosZ(getPosZ()), player);
    }
  }

  @Override
  public ModernPacket template() {
    return new RequestCMOrientationPacket(getId());
  }
}
