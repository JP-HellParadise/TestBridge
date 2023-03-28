package testbridge.network.packets.cmpipe;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

import network.rs485.logisticspipes.module.Gui;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

import testbridge.pipes.PipeCraftingManager;

@StaticResolve
public class CMGui extends CoordinatesPacket {

  private int modulePos;

  public CMGui(int id) {
    super(id);
  }

  @Override
  public void writeData(LPDataOutput output) {
    output.writeInt(modulePos);
    super.writeData(output);
  }

  @Override
  public void readData(LPDataInput input) {
    modulePos = input.readInt();
    super.readData(input);
  }

  @Override
  public void processPacket(EntityPlayer player) {
    final LogisticsTileGenericPipe pipe = getPipe(player.world, LTGPCompletionCheck.PIPE);
    if (pipe.pipe instanceof PipeCraftingManager) {
      LogisticsModule subModule = ((PipeCraftingManager) pipe.pipe).getSubModule(modulePos);
      if (subModule instanceof Gui) {
        Gui.getPipeGuiProvider((Gui) subModule).setPosX(getPosX()).setPosY(getPosY()).setPosZ(getPosZ()).open(player);
      }
    }
  }

  @Override
  public ModernPacket template() {
    return new CMGui(getId());
  }

  public CMGui setModulePos(int modulePos) {
    this.modulePos = modulePos;
    return this;
  }
}
