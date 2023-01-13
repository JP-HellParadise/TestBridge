package testbridge.mixins.logisticspipes.pipes;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextComponentTranslation;

import logisticspipes.network.PacketHandler;
import logisticspipes.pipes.PipeItemsSatelliteLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.security.SecuritySettings;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.tuples.Pair;

import network.rs485.logisticspipes.connection.*;

import testbridge.client.TB_Textures;
import testbridge.helpers.interfaces.ISatellitePipe;
import testbridge.network.packets.pipe.OrientationPacket;
import testbridge.network.packets.pipe.RequestOrientationPacket;

@Mixin(PipeItemsSatelliteLogistics.class)
public abstract class TB_MixinSatellitePipe extends CoreRoutedPipe implements ISatellitePipe {

  // Dummy constructor
  public TB_MixinSatellitePipe(Item item) {
    super(item);
  }

  /**
   * Mixin start from here
    */
  // Implement Adjacent
  @Unique
  private boolean initial = false;
  @Unique
  @Nullable
  private SingleAdjacent pointedAdjacent = null;

  @Override
  public TextureType getNonRoutedTexture(EnumFacing connection) {
    if (pointedAdjacent != null && connection.equals(pointedAdjacent.getDir())) {
      return TB_Textures.LOGISTICSPIPE_CHASSI_DIRECTION_TEXTURE;
    }
    if (isPowerProvider(connection)) {
      return TB_Textures.LOGISTICSPIPE_POWERED_TEXTURE;
    }
    return TB_Textures.LOGISTICSPIPE_CHASSI_NOTROUTED_TEXTURE;
  }

  @Override
  public void nextOrientation() {
    final SingleAdjacent pointedAdjacent = this.pointedAdjacent;
    Pair<NeighborTileEntity<TileEntity>, ConnectionType> newNeighbor;
    if (pointedAdjacent == null) {
      newNeighbor = nextPointedOrientation(null);
    } else {
      newNeighbor = nextPointedOrientation(pointedAdjacent.getDir());
    }
    final OrientationPacket packet = PacketHandler.getPacket(OrientationPacket.class);
    if (newNeighbor == null) {
      this.pointedAdjacent = null;
      packet.setDir(null);
    } else {
      this.pointedAdjacent = new SingleAdjacent(
          this, newNeighbor.getValue1().getDirection(), newNeighbor.getValue2());
      packet.setDir(newNeighbor.getValue1().getDirection());
    }
    assert container != null;
    MainProxy.sendPacketToAllWatchingChunk(container, packet.setTilePos(container));
    refreshRender(true);
  }

  /**
   * Returns the pointed adjacent EnumFacing or null, if this chassis does not have an attached inventory.
   */
  @Nullable
  @Override
  public EnumFacing getPointedOrientation() {
    if (pointedAdjacent == null) return null;
    return pointedAdjacent.getDir();
  }

  @Override
  public void setPointedOrientation(@Nullable EnumFacing dir) {
    if (dir == null) {
      pointedAdjacent = null;
    } else {
      pointedAdjacent = new SingleAdjacent(this, dir, ConnectionType.UNDEFINED);
    }
  }

  @Nullable
  private Pair<NeighborTileEntity<TileEntity>, ConnectionType> nextPointedOrientation(@Nullable EnumFacing previousDirection) {
    final Map<NeighborTileEntity<TileEntity>, ConnectionType> neighbors = getAdjacent().neighbors();
    final Stream<NeighborTileEntity<TileEntity>> sortedNeighborsStream = neighbors.keySet().stream()
        .sorted(Comparator.comparingInt(n -> n.getDirection().ordinal()));
    if (previousDirection == null) {
      return sortedNeighborsStream.findFirst().map(neighbor -> new Pair<>(neighbor, neighbors.get(neighbor))).orElse(null);
    } else {
      final List<NeighborTileEntity<TileEntity>> sortedNeighbors = sortedNeighborsStream.collect(Collectors.toList());
      if (sortedNeighbors.size() == 0) return null;
      final Optional<NeighborTileEntity<TileEntity>> nextNeighbor = sortedNeighbors.stream()
          .filter(neighbor -> neighbor.getDirection().ordinal() > previousDirection.ordinal())
          .findFirst();
      return nextNeighbor.map(neighbor -> new Pair<>(neighbor, neighbors.get(neighbor)))
          .orElse(new Pair<>(sortedNeighbors.get(0), neighbors.get(sortedNeighbors.get(0))));
    }
  }

  @Nonnull
  protected Adjacent getPointedAdjacentOrNoAdjacent() {
    // for public access, use getAvailableAdjacent()
    if (pointedAdjacent == null) {
      return NoAdjacent.INSTANCE;
    } else {
      return pointedAdjacent;
    }
  }

  @Nonnull
  @Override
  public Adjacent getAvailableAdjacent() {
    return getPointedAdjacentOrNoAdjacent();
  }

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

  @Override
  public void ignoreDisableUpdateEntity() {
    if (!initial) {
      initial = true;
      if (MainProxy.isClient(getWorld())) {
        MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestOrientationPacket.class).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
      }
    }
  }

  @Override
  public boolean handleClick(EntityPlayer entityplayer, SecuritySettings settings) {
    if (entityplayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).isEmpty()) {
      return false;
    }

    if (entityplayer.isSneaking() && SimpleServiceLocator.configToolHandler.canWrench(entityplayer, entityplayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), container)) {
      if (MainProxy.isServer(getWorld())) {
        if (settings == null || settings.openGui) {
          ((ISatellitePipe) container.pipe).nextOrientation();
        } else {
          entityplayer.sendMessage(new TextComponentTranslation("lp.chat.permissiondenied"));
        }
      }
      SimpleServiceLocator.configToolHandler.wrenchUsed(entityplayer, entityplayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), container);
      return true;
    }

    return false;
  }
}
