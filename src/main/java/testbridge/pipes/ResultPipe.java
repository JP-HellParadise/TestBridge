package testbridge.pipes;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.modules.ModuleSatellite;
import logisticspipes.pipes.PipeItemsSatelliteLogistics;
import logisticspipes.request.RequestTree;
import lombok.Getter;
import testbridge.core.TestBridge;
import testbridge.network.packets.resultpipe.SyncResultNamePacket;
import testbridge.network.GuiIDs;
import testbridge.textures.Textures;

import logisticspipes.gui.hud.HUDSatellite;
import logisticspipes.interfaces.IChangeListener;
import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.interfaces.IHeadUpDisplayRendererProvider;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.hud.ChestContent;
import logisticspipes.network.packets.hud.HUDStartWatchingPacket;
import logisticspipes.network.packets.hud.HUDStopWatchingPacket;
import logisticspipes.network.packets.orderer.OrdererManagerContent;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.routing.order.LogisticsItemOrderManager;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifierStack;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;

import network.rs485.logisticspipes.SatellitePipe;
import network.rs485.logisticspipes.connection.LPNeighborTileEntityKt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class ResultPipe extends CoreRoutedPipe implements IHeadUpDisplayRendererProvider, IChangeListener, SatellitePipe {

  //private final ModuleSatellite moduleResult;

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
    return Textures.TESTBRIDGE_RESULT_TEXTURE;
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
