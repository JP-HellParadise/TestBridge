package testbridge.network.packets.pipe;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.network.abstractpackets.InventoryModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

import testbridge.pipes.PipeCraftingManager;

@StaticResolve
public class CMPipeModuleContent extends InventoryModuleCoordinatesPacket {
  public CMPipeModuleContent(int id) {
    super(id);
  }

  @Override
  public ModernPacket template() {
    return new CMPipeModuleContent(getId());
  }

  @Override
  public void processPacket(EntityPlayer player) {
    final LogisticsTileGenericPipe pipe = this.getPipe(player.world, LTGPCompletionCheck.PIPE);
    if (pipe.pipe instanceof PipeCraftingManager) {
      PipeCraftingManager chassis = (PipeCraftingManager) pipe.pipe;
      chassis.handleModuleItemIdentifierList(getIdentList());
    }
  }
}
