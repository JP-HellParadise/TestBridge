package testbridge.pipes;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.tileentity.TileEntity;

import logisticspipes.interfaces.IChangeListener;
import logisticspipes.interfaces.ISendRoutedItem;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.order.LogisticsItemOrderManager;
import logisticspipes.security.SecuritySettings;
import logisticspipes.utils.EnumFacingUtil;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;
import logisticspipes.textures.Textures.TextureType;

import network.rs485.logisticspipes.connection.*;
import network.rs485.logisticspipes.SatellitePipe;

import testbridge.core.TestBridge;
import testbridge.network.packets.pipe.OrientationPacket;
import testbridge.network.packets.pipe.RequestOrientationPacket;
import testbridge.network.packets.pipehandler.TB_SyncNamePacket;
import testbridge.network.GuiIDs;
import testbridge.client.TB_Textures;

public class ResultPipe extends CoreRoutedPipe implements IChangeListener, SatellitePipe, ISendRoutedItem {

  public static final Set<ResultPipe> AllResults = Collections.newSetFromMap(new WeakHashMap<>());
  private String resultPipeName = "";
  private boolean initial = false;
  @Nullable
  private SingleAdjacent pointedAdjacent = null;

  @Override
  public TextureType getCenterTexture() {
    return TB_Textures.TESTBRIDGE_RESULT_TEXTURE;
  }

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

  public ResultPipe(Item item) {
    super(item);
    throttleTime = 40;
    _orderItemManager = new LogisticsItemOrderManager(this, this); // null by default when not needed
  }

  @Override
  public String getSatellitePipeName() {
    return resultPipeName;
  }

  public void setSatellitePipeName(@Nonnull String resultPipeName) {
    this.resultPipeName = resultPipeName;
  }

  // called only on server shutdown
  public static void cleanup() {
    ResultPipe.AllResults.clear();
  }

  @Override
  public ItemSendMode getItemSendMode() {
    return ItemSendMode.Normal;
  }

  @Override
  public void writeToNBT(NBTTagCompound nbttagcompound) {
    nbttagcompound.setString("resultPipeName", this.resultPipeName);
    nbttagcompound.setInteger("Orientation", pointedAdjacent == null ? -1 : pointedAdjacent.getDir().ordinal());
    super.writeToNBT(nbttagcompound);
  }

  @Override
  public void readFromNBT(@Nonnull NBTTagCompound nbttagcompound) {
    super.readFromNBT(nbttagcompound);
    int tmp = nbttagcompound.getInteger("Orientation");
    if (tmp != -1) {
      setPointedOrientation(EnumFacingUtil.getOrientation(tmp % 6));
    }
    if (nbttagcompound.hasKey("resultid")) {
      int resultId = nbttagcompound.getInteger("resultid");
      this.resultPipeName = Integer.toString(resultId);
    } else {
      this.resultPipeName = nbttagcompound.getString("resultPipeName");
    }
    if (MainProxy.isServer(getWorld())) {
      ensureAllSatelliteStatus();
    }
  }

  @Override
  public void ensureAllSatelliteStatus() {
    if (resultPipeName.isEmpty()) {
      ResultPipe.AllResults.remove(this);
    }
    if (!resultPipeName.isEmpty()) {
      ResultPipe.AllResults.add(this);
    }
  }

  @Override
  public void updateWatchers() {
    CoordinatesPacket packet = PacketHandler.getPacket(TB_SyncNamePacket.class).setString(resultPipeName).setTilePos(this.getContainer());
    MainProxy.sendPacketToAllWatchingChunk(this.container, packet);
  }

  @Override
  public void onAllowedRemoval() {
    if (MainProxy.isClient(getWorld())) {
      return;
    }
    ResultPipe.AllResults.remove(this);
  }

  /**
   * Returns the pointed adjacent EnumFacing or null, if this pipe does not have an attached inventory.
   */
  @Nullable
  @Override
  public EnumFacing getPointedOrientation() {
    if (pointedAdjacent == null) return null;
    return pointedAdjacent.getDir();
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
    MainProxy.sendPacketToAllWatchingChunk(container, packet.setTilePos(container));
    refreshRender(true);
  }

  public void setPointedOrientation(@Nullable EnumFacing dir) {
    if (dir == null) {
      pointedAdjacent = null;
    } else {
      pointedAdjacent = new SingleAdjacent(this, dir, ConnectionType.UNDEFINED);
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
          ((ResultPipe) container.pipe).nextOrientation();
        } else {
          entityplayer.sendMessage(new TextComponentTranslation("lp.chat.permissiondenied"));
        }
      }
      SimpleServiceLocator.configToolHandler.wrenchUsed(entityplayer, entityplayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), container);
      return true;
    }

    return false;
  }

  @Override
  public void onWrenchClicked(EntityPlayer entityplayer) {
    // Send the result id when opening gui
    final ModernPacket packet = PacketHandler.getPacket(TB_SyncNamePacket.class).setString(resultPipeName).setPosX(getX()).setPosY(getY()).setPosZ(getZ());
    MainProxy.sendPacketToPlayer(packet, entityplayer);
    entityplayer.openGui(TestBridge.INSTANCE, GuiIDs.RESULT_PIPE.ordinal(), getWorld(), getX(), getY(), getZ());
  }

  @Nonnull
  public Set<SatellitePipe> getSatellitesOfType() {
    return Collections.unmodifiableSet(AllResults);
  }

  @Nonnull
  @Override
  public List<ItemIdentifierStack> getItemList() {
    return new LinkedList<>();
  }

  @Nullable
  @Override
  public LogisticsModule getLogisticsModule() {
    return null;
  }

  @Override
  public void listenedChanged() {}
}
