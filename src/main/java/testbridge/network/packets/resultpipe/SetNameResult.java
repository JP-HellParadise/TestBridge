package testbridge.network.packets.resultpipe;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.client.FMLClientHandler;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;

import testbridge.gui.GuiResultPipe;

@StaticResolve
public class SetNameResult extends logisticspipes.network.packets.satpipe.SetNameResult {

  public SetNameResult(int id) {
    super(id);
  }

  @Override
  public void processPacket(EntityPlayer player) {
    if (FMLClientHandler.instance().getClient().currentScreen instanceof GuiResultPipe) {
      ((GuiResultPipe) FMLClientHandler.instance().getClient().currentScreen).handleResponse(getResult(), getNewName());
    }
  }

  @Override
  public ModernPacket template() {
    return new SetNameResult(getId());
  }

}
