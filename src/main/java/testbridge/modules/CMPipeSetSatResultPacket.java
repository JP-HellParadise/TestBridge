package testbridge.modules;

import logisticspipes.network.abstractpackets.ModernPacket;

import logisticspipes.network.packets.pipe.CraftingPipeSetSatellitePacket;
import logisticspipes.utils.StaticResolve;

import net.minecraft.entity.player.EntityPlayer;

@StaticResolve
public class CMPipeSetSatResultPacket extends CraftingPipeSetSatellitePacket {

  public CMPipeSetSatResultPacket(int id) {
    super(id);
  }

  @Override
  public void processPacket(EntityPlayer player) {
    TB_ModuleCM module = this.getLogisticsModule(player, TB_ModuleCM.class);
    if (module == null) {
      return;
    }
    if (getInteger() == 0) {
      module.setSatelliteUUID(getPipeID());
    } else if (getInteger() == 1) {
      module.setResultUUID(getPipeID());
    }
  }

  @Override
  public ModernPacket template() {
    return new CMPipeSetSatResultPacket(getId());
  }
}
