package testbridge.network.packets.craftingmanager;

import lombok.Getter;
import lombok.Setter;

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

  @Getter
  @Setter
  private int id;

  public CMGui(int id) {
    super(id);
  }

  @Override
  public void writeData(LPDataOutput output) {
    super.writeData(output);
    output.writeInt(id);
  }

  @Override
  public void readData(LPDataInput input) {
    super.readData(input);
    id = input.readInt();
  }

  @Override
  public void processPacket(EntityPlayer player) {
    final LogisticsTileGenericPipe pipe = getPipe(player.world, LTGPCompletionCheck.PIPE);
    if (pipe.pipe instanceof PipeCraftingManager) {
      LogisticsModule subModule = ((PipeCraftingManager) pipe.pipe).getSubModule(getId());
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
