package testbridge.modules;

import java.util.*;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;

import lombok.Getter;

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

import network.rs485.logisticspipes.connection.LPNeighborTileEntityKt;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.module.Gui;
import network.rs485.logisticspipes.module.PipeServiceProviderUtilKt;
import network.rs485.logisticspipes.property.*;

import testbridge.helpers.TBText;
import testbridge.helpers.interfaces.ISatellitePipe;
import testbridge.interfaces.ITranslationKey;
import testbridge.network.guis.pipe.CMGuiProvider;
import testbridge.network.packets.pipe.CMPipeUpdatePacket;
import testbridge.pipes.PipeCraftingManager;
import testbridge.pipes.ResultPipe;

public class TB_ModuleCM extends LogisticsModule implements Gui, ITranslationKey {

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
  private final List<Property<?>> properties;
  private final List<List<org.apache.commons.lang3.tuple.Pair<IRequestItems, ItemIdentifierStack>>> bufferList = new ArrayList<>();
  private int sendCooldown = 0;
  private UpdateSatResultFromNames updateSatResultFromNames = null;
  private final PipeCraftingManager parentPipe;
  private final SlottedModuleListProperty modules;

  public TB_ModuleCM(int moduleCount, PipeCraftingManager parentPipe) {
    modules = new SlottedModuleListProperty(moduleCount, "modules");
    this.parentPipe = parentPipe;
    registerPosition(ModulePositionType.IN_PIPE, 0);
    properties = ImmutableList.<Property<?>>builder()
        .add(excludedInventory)
        .addAll(Collections.singletonList(modules))
        .add(satelliteUUID)
        .add(resultUUID)
        .add(blockingMode)
        .build();
  }

  public static String getName() {
    return "crafting_manager";
  }

  @Nonnull
  @Override
  public String getLPName() {
    return getName();
  }

  @Nonnull
  @Override
  public List<Property<?>> getProperties() {
    return properties;
  }

  public void installModule(int slot, LogisticsModule module) {
    modules.set(slot, module);
  }

  public void removeModule(int slot) {
    modules.clear(slot);
  }

  @Nullable
  public LogisticsModule getModule(int slot) {
    return modules.get(slot).getModule();
  }

  public boolean hasModule(int slot) {
    return !modules.get(slot).isEmpty();
  }

  public Stream<LogisticsModule> getModules() {
    return modules.stream()
        .filter(slottedModule -> !slottedModule.isEmpty())
        .map(SlottedModule::getModule);
  }

  public Stream<SlottedModule> slottedModules() {
    return modules.stream();
  }

  @Override
  public SinkReply sinksItem(@Nonnull ItemStack stack, ItemIdentifier item, int bestPriority, int bestCustomPriority,
                             boolean allowDefault, boolean includeInTransit, boolean forcePassive) {
    SinkReply bestResult = null;
    for (SlottedModule slottedModule : modules) {
      final LogisticsModule module = slottedModule.getModule();
      if (module != null) {
        if (!forcePassive || module.recievePassive()) {
          SinkReply result = module
              .sinksItem(stack, item, bestPriority, bestCustomPriority, allowDefault, includeInTransit,
                  forcePassive);
          if (result != null && result.maxNumberOfItems >= 0) {
            bestResult = result;
            bestPriority = result.fixedPriority.ordinal();
            bestCustomPriority = result.customPriority;
          }
        }
      }
    }

    if (bestResult == null) {
      return null;
    }
    //Always deny items when we can't put the item anywhere
    final ISlotUpgradeManager upgradeManager = parentPipe.getUpgradeManager(ModulePositionType.SLOT,
        ((PipeCraftingManager.CMTargetInformation) bestResult.addInfo).getModuleSlot());
    IInventoryUtil invUtil = PipeServiceProviderUtilKt.availableSneakyInventories(parentPipe, upgradeManager)
        .stream().findFirst().orElse(null);
    if (invUtil == null) {
      return null;
    }
    int roomForItem;
    if (includeInTransit) {
      int onRoute = parentPipe.countOnRoute(item);
      final ItemStack copy = stack.copy();
      copy.setCount(onRoute + item.getMaxStackSize());
      roomForItem = invUtil.roomForItem(copy);
      roomForItem -= onRoute;
    } else {
      roomForItem = invUtil.roomForItem(stack);
    }
    if (roomForItem < 1) {
      return null;
    }

    if (bestResult.maxNumberOfItems == 0) {
      return new SinkReply(bestResult, roomForItem);
    }
    return new SinkReply(bestResult, Math.min(bestResult.maxNumberOfItems, roomForItem));
  }

  @Override
  public void registerHandler(IWorldProvider world, IPipeServiceProvider service) {
    super.registerHandler(world, service);
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
    final IPipeServiceProvider service = _service;
    if (service == null) return;
    enabledUpdateEntity();
    if (updateSatResultFromNames != null && service.isNthTick(100)) {
      if (!updateSatResultFromNames.satelliteName.isEmpty()) {
        UUID uuid = getUUIDForSatelliteName(updateSatResultFromNames.satelliteName);
        if (uuid != null) {
          updateSatResultFromNames.satelliteName = "";
          satelliteUUID.setValue(uuid);
        }
      }
      if (!updateSatResultFromNames.resultName.isEmpty()) {
        UUID uuid = getUUIDForResultName(updateSatResultFromNames.resultName);
        if (uuid != null) {
          updateSatResultFromNames.resultName = "";
          resultUUID.setValue(uuid);
        }
      }
      if (updateSatResultFromNames.satelliteName.isEmpty()
          && updateSatResultFromNames.resultName.isEmpty()) {
        updateSatResultFromNames = null;
      }
    }
    for (SlottedModule slottedModule : modules) {
      final LogisticsModule module = slottedModule.getModule();
      if (module == null) {
        continue;
      }
      module.tick();
    }
  }

  @SuppressWarnings("ConstantConditions") // LPNeighborTileEntityKt::getInventoryUtil won't give null since there is inv checker
  private void enabledUpdateEntity() {
    if (!parentPipe.isNthTick(5)) {
      return;
    }

    if(parentPipe.hasBufferUpgrade()) {
      if(sendCooldown > 0) {
        parentPipe.spawnParticle(Particles.RedParticle, 1);
        sendCooldown--;
        return;
      }
      if(!bufferList.isEmpty()) {
        boolean allow = checkBlocking();
        if(!allow) {
          parentPipe.spawnParticle(Particles.RedParticle, 1);
          return;
        }
        final List<NeighborTileEntity<TileEntity>> neighborAdjacent = parentPipe.getAvailableAdjacent().inventories();
        if(parentPipe.canUseEnergy(neededEnergy()) && !neighborAdjacent.isEmpty()){
          IInventoryUtil util = neighborAdjacent.stream().map(LPNeighborTileEntityKt::getInventoryUtil).findFirst().orElse(null);
          for (List<Pair<IRequestItems, ItemIdentifierStack>> map : bufferList) {
            if(map.stream().map(Pair::getValue).allMatch(i -> util.itemCount(i.getItem()) >= i.getStackSize())){
              int maxDist = 0;
              for (Pair<IRequestItems, ItemIdentifierStack> en : map) {
                ItemIdentifierStack toSend = en.getValue();
                ItemStack removed = util.getMultipleItems(toSend.getItem(), toSend.getStackSize());
                if (!removed.isEmpty()) {
                  UUID moved;
                  if (parentPipe.getSatelliteRouterByUUID(en.getKey().getRouter().getId()) != null )
                    moved = en.getKey().getRouter().getId();
                  else
                    moved = parentPipe.getCMResultRouter().getId();
                  parentPipe.sendStack(removed, SimpleServiceLocator.routerManager.getIDforUUID(moved), CoreRoutedPipe.ItemSendMode.Fast, null, parentPipe.getPointedOrientation());
                  maxDist = Math.max(maxDist, (int) en.getKey().getRouter().getPipe().getPos().distanceSq(parentPipe.getPos()));
                }
              }
              parentPipe.useEnergy(neededEnergy(), true);
              bufferList.remove(map);
              if (blockingMode.getValue() == BlockingMode.EMPTY_MAIN_SATELLITE) {
                sendCooldown = Math.min(maxDist, 16);
              }
              break;
            }
          }
        }
      }
    }
  }

  private int neededEnergy() {
    return 20;
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
        .setSatelliteName(getSatelliteNameByUUID(satelliteUUID.getValue(), false))
        .setResultName(getSatelliteNameByUUID(resultUUID.getValue(), true))
        .setBlockingMode(blockingMode.getValue().ordinal())
        .setModulePos(this);
  }

  public String getSatelliteNameByUUID(UUID uuid, boolean isResult) {
    if (UUIDPropertyKt.isZero(uuid)) {
      return new TBText(top$cm_prefix + "none").getTranslated();
    }
    int routerId = SimpleServiceLocator.routerManager.getIDforUUID(uuid);
    try {
      List<ExitRoute> exitRoutes = parentPipe.getRouter().getRouteTable().get(routerId);
      if (exitRoutes != null && !exitRoutes.isEmpty()) {
        CoreRoutedPipe pipe = SimpleServiceLocator.routerManager.getRouter(routerId).getPipe();
        if (!isResult && pipe instanceof PipeItemsSatelliteLogistics) {
          String name = ((PipeItemsSatelliteLogistics) pipe).getSatellitePipeName();
          return new TBText(top$cm_prefix + "valid")
              .addArgument(name.isEmpty() ? new TBText(top$cm_prefix + "none").getTranslated() : name).getTranslated();
        } else if (isResult && pipe instanceof ResultPipe) {
          String name = ((ResultPipe) pipe).getSatellitePipeName();
          return new TBText(top$cm_prefix + "valid")
              .addArgument(name.isEmpty() ? new TBText(top$cm_prefix + "none").getTranslated() : name).getTranslated();
        }
      }
    } catch (IndexOutOfBoundsException ignore) {}
    return new TBText(top$cm_prefix + "router_error").getTranslated();
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
    MainProxy.sendToPlayerList(getCMPipePacket(), parentPipe.localModeWatchers);
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

  protected boolean checkBlocking() {
    switch (blockingMode.getValue()) {
      case EMPTY_MAIN_SATELLITE:
      {
        for (List<Pair<IRequestItems, ItemIdentifierStack>> map : bufferList) {
          for (Pair<IRequestItems, ItemIdentifierStack> en : map) {
            PipeItemsSatelliteLogistics sat;
            if (parentPipe.getSatelliteRouterByUUID(en.getKey().getRouter().getId()) != null )
              sat = (PipeItemsSatelliteLogistics) en.getKey().getRouter().getPipe();
            else
              sat = (PipeItemsSatelliteLogistics) parentPipe.getCMSatelliteRouter().getPipe();
            if (sat == null) return false;
            List<NeighborTileEntity<TileEntity>> adjacentInventories = ((ISatellitePipe) sat).getAvailableAdjacent().inventories();
            for (NeighborTileEntity<TileEntity> adjacentCrafter : adjacentInventories) {
              IInventoryUtil inv = LPNeighborTileEntityKt.getInventoryUtil(adjacentCrafter);
              if (inv != null) {
                for (int i = 0; i < inv.getSizeInventory(); i++) {
                  ItemStack stackInSlot = inv.getStackInSlot(i);
                  if (!stackInSlot.isEmpty()) {
                    if (excludedInventory.getItemsAndCount().containsKey(ItemIdentifier.get(stackInSlot))) continue;
                    return false;
                  }
                }
              }
            }
          }
          return true;
        }
      }

      case REDSTONE_HIGH:
        return getWorld().isBlockPowered(parentPipe.getPos());

      case REDSTONE_LOW:
        return !getWorld().isBlockPowered(parentPipe.getPos());

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
        return true;
    }
  }

  public void addBuffered(List<org.apache.commons.lang3.tuple.Pair<IRequestItems, ItemIdentifierStack>> record) {
    bufferList.add(record);
  }

  public enum BlockingMode {
    OFF,
    //    WAIT_FOR_RESULT,
    EMPTY_MAIN_SATELLITE,
    REDSTONE_LOW,
    REDSTONE_HIGH,
    ;
    public static final BlockingMode[] VALUES = values();
  }
}
