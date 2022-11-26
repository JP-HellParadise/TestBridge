package testbridge.pipes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;

import logisticspipes.gui.hud.HUDSatellite;
import logisticspipes.interfaces.IChangeListener;
import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.interfaces.IHeadUpDisplayRendererProvider;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.hud.HUDStartWatchingPacket;
import logisticspipes.network.packets.hud.HUDStopWatchingPacket;
import logisticspipes.network.packets.orderer.OrdererManagerContent;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.routing.order.LogisticsItemOrderManager;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.tuples.Pair;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifierStack;

import network.rs485.logisticspipes.connection.*;
import network.rs485.logisticspipes.pipes.IChassisPipe;
import network.rs485.logisticspipes.SatellitePipe;

import testbridge.core.TestBridge;
import testbridge.network.packets.pipe.CMOrientationPacket;
import testbridge.network.packets.resultpipe.SyncResultNamePacket;
import testbridge.network.GuiIDs;
import testbridge.textures.TB_Textures;

public class ResultPipe extends CoreRoutedPipe implements IHeadUpDisplayRendererProvider, IChangeListener, SatellitePipe, IChassisPipe {
  public final LinkedList<ItemIdentifierStack> oldList = new LinkedList<>();
  public static final Set<ResultPipe> AllResults = Collections.newSetFromMap(new WeakHashMap<>());

  // called only on server shutdown
  public static void cleanup() {
    ResultPipe.AllResults.clear();
  }

  public final PlayerCollectionList localModeWatchers = new PlayerCollectionList();
  private final HUDSatellite HUD = new HUDSatellite(this);
  private boolean doContentUpdate = true;
  private String resultPipeName = "";

  @Nullable
  private SingleAdjacent pointedAdjacent = null;

  public ResultPipe(Item item) {
    super(item);
    throttleTime = 40;
    _orderItemManager = new LogisticsItemOrderManager(this, this);
  }


  private void checkContentUpdate() {
    LinkedList<ItemIdentifierStack> all = _orderItemManager.getContentList(getWorld());
    if (!oldList.equals(all)) {
      oldList.clear();
      oldList.addAll(all);
      MainProxy.sendToPlayerList(PacketHandler.getPacket(OrdererManagerContent.class).setIdentList(all).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), localModeWatchers);
    }
  }

  @Override
  public TextureType getCenterTexture() {
    return TB_Textures.TESTBRIDGE_RESULT_TEXTURE;
  }

  @Override
  public void enabledUpdateEntity() {
    super.enabledUpdateEntity();
    if (doContentUpdate) {
      checkContentUpdate();
    }
  }

  @Nullable
  @Override
  public LogisticsModule getLogisticsModule() {
    return null;
  }

  @Override
  public ItemSendMode getItemSendMode() {
    return ItemSendMode.Normal;
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
    final CMOrientationPacket packet = PacketHandler.getPacket(CMOrientationPacket.class);
    if (newNeighbor == null) {
      this.pointedAdjacent = null;
      packet.setDir(null);
    } else {
      this.pointedAdjacent = new SingleAdjacent(
          this, newNeighbor.getValue1().getDirection(), newNeighbor.getValue2());
      packet.setDir(newNeighbor.getValue1().getDirection());
    }
    refreshRender(true);
  }

  @Override
  public void setPointedOrientation(@Nullable EnumFacing dir) {
    if (dir == null) {
      pointedAdjacent = null;
    } else {
      pointedAdjacent = new SingleAdjacent(this, dir, ConnectionType.UNDEFINED);
    }
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

  @Override
  public IInventory getModuleInventory() {
    return null;
  }

  @Override
  public int getChassisSize() {
    return 0;
  }

  @Override
  public void startWatching() {
    MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStartWatchingPacket.class).setInteger(1).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
  }

  @Override
  public void stopWatching() {
    MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStopWatchingPacket.class).setInteger(1).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
  }

  @Override
  public void playerStartWatching(EntityPlayer player, int mode) {
    if (mode == 1) {
      localModeWatchers.add(player);
      final ModernPacket packet = PacketHandler.getPacket(SyncResultNamePacket.class).setString(resultPipeName).setPosX(getX()).setPosY(getY()).setPosZ(getZ());
      MainProxy.sendPacketToPlayer(packet, player);
    } else {
      super.playerStartWatching(player, mode);
    }
  }

  @Override
  public void playerStopWatching(EntityPlayer player, int mode) {
    super.playerStopWatching(player, mode);
    localModeWatchers.remove(player);
  }

  @Override
  public IHeadUpDisplayRenderer getRenderer() {
    return HUD;
  }

  @Override
  public void readFromNBT(NBTTagCompound nbttagcompound) {
    super.readFromNBT(nbttagcompound);
    if (nbttagcompound.hasKey("resultid")) {
      this.resultPipeName = Integer.toString(nbttagcompound.getInteger("resultid"));
    } else {
      this.resultPipeName = nbttagcompound.getString("resultPipeName");
    }
    if (MainProxy.isServer(getWorld())) {
      ensureAllSatelliteStatus();
    }
  }

  @Override
  public void writeToNBT(NBTTagCompound nbttagcompound) {
    nbttagcompound.setString("resultPipeName", this.resultPipeName);
    super.writeToNBT(nbttagcompound);
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
    CoordinatesPacket packet = PacketHandler.getPacket(SyncResultNamePacket.class).setString(resultPipeName).setTilePos(this.getContainer());
    MainProxy.sendToPlayerList(packet, localModeWatchers);
    MainProxy.sendPacketToAllWatchingChunk(this.getContainer(), packet);
  }

  @Override
  public void onAllowedRemoval() {
    if (MainProxy.isClient(getWorld())) {
      return;
    }
    ResultPipe.AllResults.remove(this);
  }

  @Override
  public void onWrenchClicked(EntityPlayer entityplayer) {
    // Send the result id when opening gui
    final ModernPacket packet = PacketHandler.getPacket(SyncResultNamePacket.class).setString(resultPipeName).setPosX(getX()).setPosY(getY()).setPosZ(getZ());
    MainProxy.sendPacketToPlayer(packet, entityplayer);
    entityplayer.openGui(TestBridge.instance, GuiIDs.GUI_ResultPipe_ID, getWorld(), getX(), getY(), getZ());
  }

  @Nonnull
  public Set<SatellitePipe> getSatellitesOfType() {
    return Collections.unmodifiableSet(AllResults);
  }

  @Override
  public String getSatellitePipeName() {
    return resultPipeName;
  }

  public void setSatellitePipeName(@Nonnull String resultPipeName) {
    this.resultPipeName = resultPipeName;
  }

  @Nonnull
  @Override
  public List<ItemIdentifierStack> getItemList() {
    return new LinkedList<>();
  }

  @Override
  public void listenedChanged() {

  }
}
