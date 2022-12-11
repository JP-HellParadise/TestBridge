package testbridge.mixins.logisticspipes.pipes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.Item;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import logisticspipes.pipes.PipeItemsSatelliteLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;

import network.rs485.logisticspipes.connection.*;

import testbridge.helpers.interfaces.ISatellitePipe;

@Mixin(PipeItemsSatelliteLogistics.class)
public abstract class TB_MixinSatellitePipe extends CoreRoutedPipe implements ISatellitePipe {

  // Dummy constructor
  public TB_MixinSatellitePipe(Item item) {
    super(item);
  }

  // Mixin start from here
  // Implement Adjacent
  @Unique
  @Nullable
  private SingleAdjacent pointedAdjacent = null;

  @Nonnull
  protected Adjacent getPointedAdjacentOrNoAdjacent() {
    // for public access, use getAvailableAdjacent()
    if (pointedAdjacent == null) {
      return NoAdjacent.INSTANCE;
    } else {
      return pointedAdjacent;
    }
  }

  /**
   * Returns just the adjacent this pipe points at or no adjacent.
   */
  @Nonnull
  @Override
  public Adjacent getAvailableAdjacent() {
    return getPointedAdjacentOrNoAdjacent();
  }

  /**
   * Updates pointedAdjacent on {@link CoreRoutedPipe}.
   */
  @Override
  protected void updateAdjacentCache() {
    super.updateAdjacentCache();
    final Adjacent adjacent = getAdjacent();
    if (adjacent instanceof SingleAdjacent) {
      pointedAdjacent = ((SingleAdjacent) adjacent);
    } else {
      final SingleAdjacent oldPointedAdjacent = pointedAdjacent;
      SingleAdjacent newPointedAdjacent = null;
      if (oldPointedAdjacent != null) {
        // update pointed adjacent with connection type or reset it
        newPointedAdjacent = adjacent.optionalGet(oldPointedAdjacent.getDir()).map(connectionType -> new SingleAdjacent(this, oldPointedAdjacent.getDir(), connectionType)).orElse(null);
      }
      if (newPointedAdjacent == null) {
        newPointedAdjacent = adjacent.neighbors().entrySet().stream().findAny().map(connectedNeighbor -> new SingleAdjacent(this, connectedNeighbor.getKey().getDirection(), connectedNeighbor.getValue())).orElse(null);
      }
      pointedAdjacent = newPointedAdjacent;
    }
  }
}
