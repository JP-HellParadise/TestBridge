package testbridge.network.packets.pipe;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.Setter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;

import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

import testbridge.pipes.PipeCraftingManager;

@StaticResolve
public class CMOrientationPacket extends CoordinatesPacket {
  @Getter
  @Setter
  @Nullable
  private EnumFacing dir;

  public CMOrientationPacket(int id) {
    super(id);
  }

  @Override
  public void processPacket(EntityPlayer player) {
    LogisticsTileGenericPipe pipe = this.getPipe(player.world, LTGPCompletionCheck.PIPE);
    if (pipe.pipe instanceof PipeCraftingManager) {
      ((PipeCraftingManager) pipe.pipe).setPointedOrientation(dir);
    }
  }

  @Override
  public ModernPacket template() {
    return new CMOrientationPacket(getId());
  }
}
