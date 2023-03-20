package testbridge.modules;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import lombok.Getter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import logisticspipes.interfaces.*;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeItemsSatelliteLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.computers.objects.CCSinkResponder;
import logisticspipes.routing.ExitRoute;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.PlayerCollectionList;

import network.rs485.logisticspipes.connection.LPNeighborTileEntityKt;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.module.Gui;
import network.rs485.logisticspipes.property.*;

import testbridge.helpers.TextHelper;
import testbridge.helpers.interfaces.ISatellitePipe;
import testbridge.helpers.interfaces.ITranslationKey;
import testbridge.helpers.interfaces.TB_IInventoryUtil;
import testbridge.network.guis.pipe.CMGuiProvider;
import testbridge.network.packets.pipe.CMPipeUpdatePacket;
import testbridge.pipes.PipeCraftingManager;
import testbridge.pipes.ResultPipe;

public class TB_ModuleCM extends LogisticsModule implements Gui, ITranslationKey, IGuiOpenControler {

  public final InventoryProperty excludedInventory = new InventoryProperty(
      new ItemIdentifierInventory(3, "Excluded Filter Item", 1), "ExcludedInv");
  @Getter
  private final UUIDProperty satelliteUUID = new UUIDProperty(null, "satelliteUUID");
  @Getter
  private final UUIDProperty resultUUID = new UUIDProperty(null, "resultUUID");
  @Getter
  private final EnumProperty<BlockingMode> blockingMode = new EnumProperty<>(BlockingMode.OFF, "blockingMode", BlockingMode.VALUES);
  @Getter
  private final ClientSideSatResultNames clientSideSatResultNames = new ClientSideSatResultNames();
  private UpdateSatResultFromNames updateSatResultFromNames = null;
  protected SinkReply _sinkReply;
  private final List<Property<?>> properties;
  private final SlottedModuleListProperty modules;
  private final PipeCraftingManager parentPipe;
  private final int neededEnergy = 20;
  private int sendCooldown = 0;
  protected final PlayerCollectionList guiWatcher = new PlayerCollectionList();
  private Queue<HashMap<IRequestItems, List<ItemIdentifierStack>>> craftingList;
  private HashMap<IRequestItems, List<ItemIdentifierStack>> waitingToSend;

  public TB_ModuleCM(int moduleCount, PipeCraftingManager parentPipe) {
    this.modules = new SlottedModuleListProperty(moduleCount, "modules");
    this.parentPipe = parentPipe;
    this.registerPosition(ModulePositionType.IN_PIPE, 0);
    this.properties = ImmutableList.<Property<?>>builder()
        .addAll(Collections.singletonList(this.modules))
        .add(this.excludedInventory)
        .add(this.satelliteUUID)
        .add(this.resultUUID)
        .add(this.blockingMode)
        .build();
  }

  @Nonnull
  @Override
  public String getLPName() {
    return "crafting_manager";
  }

  @Nonnull
  @Override
  public List<Property<?>> getProperties() {
    return this.properties;
  }

  public void installModule(int slot, LogisticsModule module) {
    this.modules.set(slot, module);
  }

  public void removeModule(int slot) {
    this.modules.clear(slot);
  }

  @Nullable
  public LogisticsModule getModule(int slot) {
    return this.modules.get(slot).getModule();
  }

  public boolean hasModule(int slot) {
    return !this.modules.get(slot).isEmpty();
  }

  public Stream<LogisticsModule> getModules() {
    return this.modules.stream()
        .filter(slottedModule -> !slottedModule.isEmpty())
        .map(SlottedModule::getModule);
  }

  public Stream<SlottedModule> slottedModules() {
    return this.modules.stream();
  }

  @Override
  public void registerPosition(@Nonnull ModulePositionType slot, int positionInt) {
    super.registerPosition(slot, positionInt);
    this._sinkReply = new SinkReply(SinkReply.FixedPriority.ItemSink, 0, true, false, 1, 0,
        new PipeCraftingManager.CMTargetInformation(this.getPositionInt()));
  }

  private UUID getUUIDForSatelliteName(String name) {
    for (PipeItemsSatelliteLogistics pipe : PipeItemsSatelliteLogistics.AllSatellites) {
      if (pipe.getSatellitePipeName().equals(name)) {
        return pipe.getRouter().getId();
      }
    }
    return null;
  }

  private UUID getUUIDForResultName(String name) {
    for (ResultPipe pipe : ResultPipe.AllResults) {
      if (pipe.getSatellitePipeName().equals(name)) {
        return pipe.getRouter().getId();
      }
    }
    return null;
  }

  @Override
  public void tick() {
    final IPipeServiceProvider service = this._service;
    if (service == null) return;
    this.enabledUpdateEntity();
    if (this.updateSatResultFromNames != null && service.isNthTick(100)) {
      if (!this.updateSatResultFromNames.satelliteName.isEmpty()) {
        UUID uuid = this.getUUIDForSatelliteName(this.updateSatResultFromNames.satelliteName);
        if (uuid != null) {
          this.updateSatResultFromNames.satelliteName = "";
          this.satelliteUUID.setValue(uuid);
        }
      }
      if (!this.updateSatResultFromNames.resultName.isEmpty()) {
        UUID uuid = this.getUUIDForResultName(this.updateSatResultFromNames.resultName);
        if (uuid != null) {
          this.updateSatResultFromNames.resultName = "";
          this.resultUUID.setValue(uuid);
        }
      }
      if (this.updateSatResultFromNames.satelliteName.isEmpty()
          && this.updateSatResultFromNames.resultName.isEmpty()) {
        this.updateSatResultFromNames = null;
      }
    }

    this.modules.stream().map(SlottedModule::getModule).filter(Objects::nonNull).forEach(LogisticsModule::tick);
  }

  @Override
  public void finishInit() {
    super.finishInit();
    getModules().forEach(LogisticsModule::finishInit);
  }

  @Override
  public boolean hasGenericInterests() {
    return false;
  }

  @Override
  public boolean interestedInAttachedInventory() {
    return false;
    // when we are default we are interested in everything anyway, otherwise we're only interested in our filter.
  }

  @Override
  public boolean interestedInUndamagedID() {
    return false;
  }

  @Override
  public boolean recievePassive() {
    return false;
  }

  @Override
  public List<CCSinkResponder> queueCCSinkEvent(ItemIdentifierStack item) {
    List<CCSinkResponder> list = new ArrayList<>();
    for (SlottedModule slottedModule : modules) {
      final LogisticsModule module = slottedModule.getModule();
      if (module != null) {
        list.addAll(module.queueCCSinkEvent(item));
      }
    }
    return list;
  }

  @Nonnull
  @Override
  public ModuleCoordinatesGuiProvider getPipeGuiProvider() {
    return NewGuiHandler.getGui(CMGuiProvider.class)
        .setBufferUpgrade(parentPipe.hasBufferUpgrade())
        .setBlockingMode(blockingMode.getValue().ordinal())
        .setContainerConnected(parentPipe.getAvailableAdjacent().inventories().isEmpty());
  }

  @Nonnull
  @Override
  public ModuleInHandGuiProvider getInHandGuiProvider() {
    throw new UnsupportedOperationException("Crafting Manager GUI can never be opened in hand");
  }

  @Override
  public void readFromNBT(@Nonnull NBTTagCompound tag) {
    super.readFromNBT(tag);
    // Stream crafter modules
    modules.stream()
        .filter(slottedModule -> !slottedModule.isEmpty() && tag.hasKey("slot" + slottedModule.getSlot()))
        .forEach(slottedModule -> Objects.requireNonNull(slottedModule.getModule())
            .readFromNBT(tag.getCompoundTag("slot" + slottedModule.getSlot())));
    // Showing update sat and result info
    if (tag.hasKey("satellitename") || tag.hasKey("resultname")) {
      updateSatResultFromNames = new UpdateSatResultFromNames();
      updateSatResultFromNames.satelliteName = tag.getString("satellitename");
      updateSatResultFromNames.resultName = tag.getString("resultname");
    }
  }

  public ModernPacket getCMPipePacket() {
    return PacketHandler.getPacket(CMPipeUpdatePacket.class)
        .setSatelliteName(this.getNameByUUID(satelliteUUID.getValue(), false))
        .setResultName(this.getNameByUUID(resultUUID.getValue(), true))
        .setBlockingMode(this.blockingMode.getValue().ordinal())
        .setModulePos(this);
  }

  public String getNameByUUID(UUID uuid, boolean isResult) {
    if (UUIDPropertyKt.isZero(uuid)) {
      return new TextHelper(top$cm_prefix + "none").getTranslated();
    }
    int routerId = SimpleServiceLocator.routerManager.getIDforUUID(uuid);
    try {
      List<ExitRoute> exitRoutes = parentPipe.getRouter().getRouteTable().get(routerId);
      if (exitRoutes != null && !exitRoutes.isEmpty()) {
        CoreRoutedPipe pipe = SimpleServiceLocator.routerManager.getRouter(routerId).getPipe();
        if (pipe instanceof PipeItemsSatelliteLogistics || pipe instanceof ResultPipe) {
          String name = "";
          if (!isResult && pipe instanceof PipeItemsSatelliteLogistics) {
            name = ((PipeItemsSatelliteLogistics) pipe).getSatellitePipeName();
          } else if (isResult && pipe instanceof ResultPipe) {
            name = ((ResultPipe) pipe).getSatellitePipeName();
          }
          return new TextHelper(top$cm_prefix + "valid")
              .addArgument(name.isEmpty() ? new TextHelper(top$cm_prefix + "none").getTranslated() : name).getTranslated();
        }
      }
    } catch (IndexOutOfBoundsException ignore) {}
    return new TextHelper(top$cm_prefix + "router_error").getTranslated();
  }

  public void handleCMUpdatePacket(CMPipeUpdatePacket packet) {
    if (MainProxy.isClient(getWorld())) {
      clientSideSatResultNames.satelliteName = packet.getSatelliteName();
      clientSideSatResultNames.resultName = packet.getResultName();
      blockingMode.setValue(BlockingMode.VALUES[packet.getBlockingMode()]);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private void updateSatResultsOnClient() {
    MainProxy.sendToPlayerList(getCMPipePacket(), guiWatcher);
  }

  public void setSatelliteUUID(@Nullable UUID pipeID) {
    if (pipeID == null) {
      satelliteUUID.zero();
    } else {
      satelliteUUID.setValue(pipeID);
    }
    updateSatResultsOnClient();
    updateSatResultFromNames = null;
  }

  public void setResultUUID(@Nullable UUID pipeID) {
    if (pipeID == null) {
      resultUUID.zero();
    } else {
      resultUUID.setValue(pipeID);
    }
    updateSatResultsOnClient();
    updateSatResultFromNames = null;
  }

  @Override
  public void guiOpenedByPlayer(EntityPlayer player) {
    guiWatcher.add(player);
  }

  @Override
  public void guiClosedByPlayer(EntityPlayer player) {
    guiWatcher.remove(player);
  }

  private static class UpdateSatResultFromNames {
    public String satelliteName;
    public String resultName;
  }

  public static class ClientSideSatResultNames {
    public @Nonnull
    String satelliteName = "";
    public @Nonnull
    String resultName = "";
  }

  // Crafting start from here

  private void enabledUpdateEntity() {
    if (!this.parentPipe.isNthTick(5)) {
      return;
    }

    if (this.hasItemsToSend()) {
      this.parentPipe.spawnParticle(Particles.GoldParticle, 1);
      this.pushItemsOut();
      return;
    }

    if (this.parentPipe.hasBufferUpgrade()) {
      if (this.sendCooldown > 0) {
        this.parentPipe.spawnParticle(Particles.RedParticle, 1);
        if (this.sendCooldown > 0) {
          this.sendCooldown--;
        }
        return;
      }
      if (this.hasItemsToCraft()) {
        if (this.isBlocking()) {
          this.parentPipe.spawnParticle(Particles.RedParticle, 1);
        } else {
          this.startCrafting();
        }
      }
    }
  }

  public void startCrafting() {
    IInventoryUtil util = getBufferInventory();
    if (parentPipe.canUseEnergy(neededEnergy) && util != null) {
      Optional<HashMap<IRequestItems, List<ItemIdentifierStack>>> bufferOpt = craftingList.stream().findFirst();
      if (bufferOpt.isPresent()
          && bufferOpt.get().entrySet().stream().allMatch(this::acceptItems)) {
        HashMap<IRequestItems, List<ItemIdentifierStack>> bufferList = bufferOpt.get();
        bufferList.forEach((key, value) -> value.forEach(it -> addToWaitingList(key, it)));
        craftingList.remove(bufferList);
        return;
      }
    }
    this.parentPipe.spawnParticle(Particles.RedParticle, 1);
  }

  private boolean acceptItems(Map.Entry<IRequestItems, List<ItemIdentifierStack>> entry) {
    return acceptItems(entry.getKey(), entry.getValue());
  }

  private boolean acceptItems(IRequestItems router, List<ItemIdentifierStack> stacks) {
    if (parentPipe.getSatelliteRouterByUUID(router.getRouter().getId()) == null) return false;
    PipeItemsSatelliteLogistics sat = (PipeItemsSatelliteLogistics) router.getRouter().getPipe();
    if (sat == null) return false;

    IInventoryUtil inv = ((ISatellitePipe) sat).getAvailableAdjacent().inventories()
        .stream().map(LPNeighborTileEntityKt::getInventoryUtil).findFirst().orElse(null);
    if (inv != null) {
      return ((TB_IInventoryUtil) inv).roomForItem(stacks.stream().map(ItemIdentifierStack::makeNormalStack).collect(Collectors.toList()));
    }
    return true;
  }

  public void addToCraftList(@Nonnull HashMap<IRequestItems, List<ItemIdentifierStack>> bufferList) {
    if (this.craftingList == null) {
      this.craftingList = new LinkedList<>();
    }

    this.craftingList.add(bufferList);
  }

  public boolean hasItemsToCraft() {
    return this.craftingList != null && !this.craftingList.isEmpty();
  }

  public void addToWaitingList(IRequestItems request, ItemIdentifierStack stack) {
    if (this.waitingToSend == null) {
      this.waitingToSend = new HashMap<>();
    }

    this.waitingToSend.computeIfAbsent(request, k -> new ArrayList<>());

    this.waitingToSend.get(request).add(stack);
  }

  private boolean hasItemsToSend() {
    if (this.waitingToSend != null) {
      for (IRequestItems router : this.waitingToSend.keySet()) {
        if (!this.waitingToSend.get(router).isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean hasAllItemInBuffer(List<ItemIdentifierStack> list) {
    IInventoryUtil util = getBufferInventory();
    if (util != null) {
      return list.stream().allMatch(it -> util.itemCount(it.getItem()) >= it.getStackSize());
    }
    return false;
  }

  private void pushItemsOut() {
    IInventoryUtil inv = getBufferInventory();
    if (inv == null || !parentPipe.canUseEnergy(neededEnergy)
      || !this.waitingToSend.values().stream().allMatch(this::hasAllItemInBuffer)) {
      this.parentPipe.spawnParticle(Particles.RedParticle, 1);
      return;
    }

    int maxDist = 0;
    boolean hasDoneSomething = false;

    for (Map.Entry<IRequestItems, List<ItemIdentifierStack>> entry : this.waitingToSend.entrySet()) {
      if (!this.acceptItems(entry)) {
        this.parentPipe.spawnParticle(Particles.RedParticle, 1);
        break;
      }
      Iterator<ItemIdentifierStack> i = entry.getValue().iterator();
      while (i.hasNext()) {
        ItemIdentifierStack aiden = i.next(); // Aiden Melt Down
        ItemStack removed = inv.getMultipleItems(aiden.getItem(), aiden.getStackSize());
        int destID = SimpleServiceLocator.routerManager.getIDforUUID(entry.getKey().getRouter().getId());
        try {
          if (destID != -1) {
            List<ExitRoute> exitRoutes = parentPipe.getRouter().getRouteTable().get(destID);
            if (exitRoutes != null && !exitRoutes.isEmpty() && !removed.isEmpty()) {
              parentPipe.sendStack(removed, destID, parentPipe.getItemSendMode(), null, parentPipe.getPointedOrientation());
              maxDist = Math.max(maxDist, (int) entry.getKey().getRouter().getPipe().getPos().distanceSq(parentPipe.getPos()));
              i.remove();
              hasDoneSomething = true;
            }
          }
        } catch (IndexOutOfBoundsException ignore) {
          this.parentPipe.spawnParticle(Particles.RedParticle, 1);
          break;
        }
      }
    }

    if (hasDoneSomething) {
      parentPipe.useEnergy(neededEnergy, true);
    }


    this.waitingToSend.values().removeIf(List::isEmpty);

    if (this.waitingToSend.isEmpty()) {
      waitingToSend = null;
      sendCooldown = Math.min(maxDist, sendCooldown == 0 ? 16 : sendCooldown);
    }
  }

  @SuppressWarnings("ConstantConditions") // LPNeighborTileEntityKt::getInventoryUtil won't give null since there is inv checker
  protected IInventoryUtil getBufferInventory() {
    final List<NeighborTileEntity<TileEntity>> parentNeighbors = parentPipe.getAvailableAdjacent().inventories();
    if (!parentNeighbors.isEmpty()) {
      return parentNeighbors.stream().map(LPNeighborTileEntityKt::getInventoryUtil).findFirst().get();
    }
    return null;
  }

  protected boolean isBlocking() {
    switch (blockingMode.getValue()) {
      case EMPTY_MAIN_SATELLITE:
        assert craftingList.peek() != null;
        return isBlockingBySet(craftingList.peek().keySet());

      case REDSTONE_HIGH:
        assert getWorld() != null;
        return !getWorld().isBlockPowered(parentPipe.getPos());

      case REDSTONE_LOW:
        assert getWorld() != null;
        return getWorld().isBlockPowered(parentPipe.getPos());

      case REDSTONE_PULSE:
        return true;

//      case WAIT_FOR_RESULT: { // TODO check if this work
//        ResultPipe defSat = (ResultPipe) getCMResultRouter().getPipe();
//        if (defSat == null) return false;
//        List<NeighborTileEntity<TileEntity>> adjacentInventories = ((ISatellitePipe) defSat).getAvailableAdjacent().inventories();
//        for (NeighborTileEntity<TileEntity> adjacentCrafter : adjacentInventories) {
//          IInventoryUtil inv = LPNeighborTileEntityKt.getInventoryUtil(adjacentCrafter);
//          if (inv != null) {
//            for (int i = 0; i < inv.getSizeInventory(); i++) {
//              ItemStack stackInSlot = inv.getStackInSlot(i);
//              if (!stackInSlot.isEmpty()) {
//                return false;
//              }
//            }
//          }
//        }
//        return true;
//      }

      default:
        return false;
    }
  }

  protected boolean isBlockingBySet(Set<IRequestItems> set) {
    // Check if the set of satellite routers is not null
    if (set != null) {
      // If it is not null, create a stream of the routers in the set
      return set.stream()
          // Filter out the routers that are not present in the parent pipe's satellite routers by checking the UUID
          .filter(router -> parentPipe.getSatelliteRouterByUUID(router.getRouter().getId()) != null)
          // Filter out any router with a null satellite pipe
          .filter(router -> (router.getRouter().getPipe()) != null)
          // Combine the stream of adjacent inventories from each router into a single stream
          .flatMap(router -> router.getRouter().getPipe().getAvailableAdjacent().inventories().stream())
          // Map the inventories to inventory utilities
          .map(LPNeighborTileEntityKt::getInventoryUtil)
          // Filter out any null inventory utilities
          .filter(Objects::nonNull)
          // Flatmap the stream of inventory utilities to a stream of stacks in the inventory
          .flatMap(inv -> IntStream.range(0, inv.getSizeInventory()).mapToObj(inv::getStackInSlot))
          // Filter out any empty stacks
          .filter(stack -> !stack.isEmpty())
          // Map the stacks to ItemIdentifiers
          .map(ItemIdentifier::get)
          // Check if there are any items that are not present in the provided excludedInventory
          .anyMatch(item -> !excludedInventory.getItemsAndCount().containsKey(item));
    }
    return true;
  }

  public enum BlockingMode {
    OFF,
    EMPTY_MAIN_SATELLITE,
    //    WAIT_FOR_RESULT,
    REDSTONE_LOW,
    REDSTONE_HIGH,
    REDSTONE_PULSE
    ;
    public static final BlockingMode[] VALUES = values();
  }
}
