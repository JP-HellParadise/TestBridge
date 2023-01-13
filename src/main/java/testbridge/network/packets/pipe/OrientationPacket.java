package testbridge.network.packets.pipe;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.Setter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;

import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeItemsSatelliteLogistics;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

import testbridge.helpers.interfaces.ISatellitePipe;
import testbridge.pipes.PipeCraftingManager;
import testbridge.pipes.ResultPipe;

@StaticResolve
public class OrientationPacket extends CoordinatesPacket {
  @Getter
  @Setter
  @Nullable
  private EnumFacing dir;

  public OrientationPacket(int id) {
    super(id);
  }

  @Override
  public void processPacket(EntityPlayer player) {
    LogisticsTileGenericPipe pipe = this.getPipe(player.world, LTGPCompletionCheck.PIPE);
    if (pipe.pipe instanceof PipeCraftingManager) {
      ((PipeCraftingManager) pipe.pipe).setPointedOrientation(dir);
    } else if (pipe.pipe instanceof ResultPipe) {
      ((ResultPipe) pipe.pipe).setPointedOrientation(dir);
    } else if (pipe.pipe instanceof PipeItemsSatelliteLogistics) {
      ((ISatellitePipe) pipe.pipe).setPointedOrientation(dir);
    }
  }

  @Override
  public ModernPacket template() {
    return new OrientationPacket(getId());
  }
}
