package testbridge.network.packets.cmpipe;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

import network.rs485.logisticspipes.module.Gui;

import testbridge.pipes.PipeCraftingManager;

@StaticResolve
public class CMGui extends ModuleCoordinatesPacket {

  public CMGui(int id) {
    super(id);
  }

  @Override
  public void processPacket(EntityPlayer player) {
    final LogisticsTileGenericPipe pipe = getPipe(player.world, LTGPCompletionCheck.PIPE);
    if (pipe.pipe instanceof PipeCraftingManager) {
      LogisticsModule subModule = ((PipeCraftingManager) pipe.pipe).getSubModule(getPositionInt());
      if (subModule instanceof Gui) {
        Gui.getPipeGuiProvider((Gui) subModule).setPosX(getPosX()).setPosY(getPosY()).setPosZ(getPosZ()).open(player);
      }
    }
  }

  @Override
  public ModernPacket template() {
    return new CMGui(getId());
  }
}
