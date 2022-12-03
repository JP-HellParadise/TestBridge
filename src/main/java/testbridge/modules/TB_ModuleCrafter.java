package testbridge.modules;

import java.lang.ref.WeakReference;
import java.util.*;
import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import logisticspipes.blocks.crafting.LogisticsCraftingTableTileEntity;
import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.interfaces.IWorldProvider;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IItemSpaceControl;
import logisticspipes.interfaces.routing.IRequestFluid;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.logistics.LogisticsManager;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.request.DictCraftingTemplate;
import logisticspipes.request.ICraftingTemplate;
import logisticspipes.request.IReqCraftingTemplate;
import logisticspipes.request.ItemCraftingTemplate;
import logisticspipes.request.resources.DictResource;
import logisticspipes.request.resources.FluidResource;
import logisticspipes.request.resources.IResource;
import logisticspipes.request.resources.ItemResource;
import logisticspipes.routing.*;
import logisticspipes.routing.order.IOrderInfoProvider.ResourceType;
import logisticspipes.routing.order.LogisticsItemOrder;
import logisticspipes.utils.CacheHolder;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

import network.rs485.logisticspipes.connection.LPNeighborTileEntityKt;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.inventory.IItemIdentifierInventory;

import testbridge.pipes.PipeCraftingManager;
import testbridge.pipes.PipeCraftingManager.CMTargetInformation;

public class TB_ModuleCrafter extends ModuleCrafter {
  private WeakReference<TileEntity> lastAccessedCrafter;
  private boolean cachedAreAllOrderesToBuffer;
  private PipeCraftingManager pipeCM;
  private IPipeServiceProvider resultService;

  @Override
  public void registerHandler(IWorldProvider world, IPipeServiceProvider service) {
    super.registerHandler(world, service);
    pipeCM = (PipeCraftingManager) service;
  }

  @Override
  public void registerPosition(@Nonnull ModulePositionType slot, int positionInt) {
    super.registerPosition(slot, positionInt);
    _sinkReply = new SinkReply(SinkReply.FixedPriority.ItemSink, 0, true, false, 1, 0,
        new CMTargetInformation(getPositionInt()));
  }

  @Override
  public LogisticsItemOrder fullFill(LogisticsPromise promise, IRequestItems destination,
                                     IAdditionalTargetInformation info) {
    if (_service == null) return null;
    ItemIdentifierStack result = getCraftedItem();
    if (result == null) return null;
    //
    int multiply = (int) Math.ceil(promise.numberOfItems / (float) result.getStackSize());
    if(pipeCM.hasBufferUpgrade()){
      List<Pair<IRequestItems, ItemIdentifierStack>> rec = new ArrayList<>();
      IRouter defSat = pipeCM.getCMSatelliteRouter();
      if (defSat == null) return null;
      IRequestItems[] target = new IRequestItems[9];
      for (int i = 0; i < 9; i++) {
        target[i] = defSat.getPipe();
      }

      boolean hasSatellite = isSatelliteConnected();
      if (!hasSatellite) {
        return null;
      }

      if (!getUpgradeManager().isAdvancedSatelliteCrafter()) {
        IRouter r = getSatelliteRouter(-1);
        if (r != null) {
          IRequestItems sat = r.getPipe();
          for (int i = 6; i < 9; i++) {
            target[i] = sat;
          }
        }
      } else {
        for (int i = 0; i < 9; i++) {
          IRouter r = getSatelliteRouter(i);
          if (r != null) {
            target[i] = r.getPipe();
          }
        }
      }

      for (int i = 0; i < target.length; i++) {
        ItemIdentifierStack materials = dummyInventory.getIDStackInSlot(i);
        if (materials != null) rec.add(Pair.of(target[i], materials));
      }

      for(int i = 0;i<multiply;i++)
        pipeCM.getModules().addBuffered(rec);
    }
    return super.fullFill(promise, destination, info);
  }

  /**
   * Now use Satellite from Crafting Manager pipe
   */
  @Override
  public ICraftingTemplate addCrafting(IResource toCraft) {
    List<ItemIdentifierStack> stack = getCraftedItems();
    if (stack == null) {
      return null;
    }
    IReqCraftingTemplate template = null;
    if (getUpgradeManager().isFuzzyUpgrade() && outputFuzzy().nextSetBit(0) != -1) {
      for (ItemIdentifierStack craftable : stack) {
        DictResource dict = new DictResource(craftable, null);
        dict.loadFromBitSet(outputFuzzy().copyValue());
        if (toCraft.matches(dict, IResource.MatchSettings.NORMAL)) {
          template = new DictCraftingTemplate(dict, this, priority.getValue());
          break;
        }
      }
    } else {
      for (ItemIdentifierStack craftable : stack) {
        if (toCraft.matches(craftable.getItem(), IResource.MatchSettings.NORMAL)) {
          template = new ItemCraftingTemplate(craftable, this, priority.getValue());
          break;
        }
      }
    }
    if (template == null) {
      return null;
    }

    if (!isSatelliteConnected()) {
      // has a satellite and result configured, that one of two is unreachable
      return null;
    }

    IRequestItems[] target = new IRequestItems[9];
    if (pipeCM.hasBufferUpgrade()) {
      for (int i = 0; i < 9; i++)
        target[i] = this;
    } else {
      IRequestItems defsat = pipeCM.getCMSatelliteRouter().getPipe();
      for (int i = 0; i < 9; i++) {
        target[i] = defsat;
      }
      if (!getUpgradeManager().isAdvancedSatelliteCrafter()) {
        IRouter r = getSatelliteRouter(-1);
        if (r != null) {
          IRequestItems sat = r.getPipe();
          for (int i = 6; i < 9; i++) {
            target[i] = sat;
          }
        }
      } else {
        for (int i = 0; i < 9; i++) {
          IRouter r = getSatelliteRouter(i);
          if (r != null) {
            target[i] = r.getPipe();
          }
        }
      }
    }

    //Check all materials
    for (int i = 0; i < 9; i++) {
      ItemIdentifierStack resourceStack = dummyInventory.getIDStackInSlot(i);
      if (resourceStack == null || resourceStack.getStackSize() == 0) {
        continue;
      }
      IResource req;
      if (getUpgradeManager().isFuzzyUpgrade() && inputFuzzy(i).nextSetBit(0) != -1) {
        DictResource dict;
        req = dict = new DictResource(resourceStack, target[i]);
        dict.loadFromBitSet(inputFuzzy(i).copyValue());
      } else {
        req = new ItemResource(resourceStack, target[i]);
      }
      template.addRequirement(req, new CraftingChassisInformation(i, getPositionInt()));
    }

    int liquidCrafter = getUpgradeManager().getFluidCrafter();
    IRequestFluid[] liquidTarget = new IRequestFluid[liquidCrafter];

    if (!getUpgradeManager().isAdvancedSatelliteCrafter()) {
      IRouter r = getFluidSatelliteRouter(-1);
      if (r != null) {
        IRequestFluid sat = (IRequestFluid) r.getPipe();
        for (int i = 0; i < liquidCrafter; i++) {
          liquidTarget[i] = sat;
        }
      }
    } else {
      for (int i = 0; i < liquidCrafter; i++) {
        IRouter r = getFluidSatelliteRouter(i);
        if (r != null) {
          liquidTarget[i] = (IRequestFluid) r.getPipe();
        }
      }
    }

    for (int i = 0; i < liquidCrafter; i++) {
      FluidIdentifier liquid = getFluidMaterial(i);
      int amount = liquidAmounts.get(i);
      if (liquid == null || amount <= 0 || liquidTarget[i] == null) {
        continue;
      }
      template.addRequirement(new FluidResource(liquid, amount, liquidTarget[i]), null);
    }

    if (getUpgradeManager().hasByproductExtractor() && getByproductItem() != null) {
      template.addByproduct(getByproductItem());
    }

    return template;
  }

  private IRouter getSatelliteRouter(int x) {
    UUID satelliteUUID = x == -1 ? (UUID)this.satelliteUUID.getValue() : (UUID)this.advancedSatelliteUUIDList.get(x);
    int satelliteRouterId = SimpleServiceLocator.routerManager.getIDforUUID(satelliteUUID);
    return SimpleServiceLocator.routerManager.getRouter(satelliteRouterId);
  }

  private IRouter getFluidSatelliteRouter(int x) {
    UUID liquidSatelliteUUID = x == -1 ? (UUID)this.liquidSatelliteUUID.getValue() : (UUID)this.liquidSatelliteUUIDList.get(x);
    int satelliteRouterId = SimpleServiceLocator.routerManager.getIDforUUID(liquidSatelliteUUID);
    return SimpleServiceLocator.routerManager.getRouter(satelliteRouterId);
  }

  @Override
  public boolean isSatelliteConnected() {
    //final List<ExitRoute> routes = getRouter().getIRoutersByCost();
    if (!getUpgradeManager().isAdvancedSatelliteCrafter()) {
      // Make sure own CM doesn't idiot screw up
      if (!(pipeCM.getModules().resultUUID.isZero() || pipeCM.getModules().satelliteUUID.isZero())) {
        int satModuleRouterId = SimpleServiceLocator.routerManager.getIDforUUID(pipeCM.getModules().satelliteUUID.getValue());
        int resultModuleRouterId = SimpleServiceLocator.routerManager.getIDforUUID(pipeCM.getModules().resultUUID.getValue());
        if (satModuleRouterId != -1 && resultModuleRouterId != -1) {
          List<ExitRoute> sat_rt = getRouter().getRouteTable().get(satModuleRouterId);
          List<ExitRoute> result_rt = getRouter().getRouteTable().get(satModuleRouterId);
          return sat_rt != null && !sat_rt.isEmpty() && result_rt != null && !result_rt.isEmpty();
        }
      } else {
        return false;
      }
      if (satelliteUUID.isZero()) {
        return true;
      }
      int satModuleRouterId = SimpleServiceLocator.routerManager.getIDforUUID(satelliteUUID.getValue());
      if (satModuleRouterId != -1) {
        List<ExitRoute> rt = getRouter().getRouteTable().get(satModuleRouterId);
        return rt != null && !rt.isEmpty();
      }
    } else {
      boolean foundAll = true;
      for (int i = 0; i < 9; i++) {
        boolean foundOne = false;
        if (advancedSatelliteUUIDList.isZero(i)) {
          continue;
        }

        int satelliteRouterId = SimpleServiceLocator.routerManager
            .getIDforUUID(advancedSatelliteUUIDList.get(i));
        if (satelliteRouterId != -1) {
          List<ExitRoute> rt = getRouter().getRouteTable().get(satelliteRouterId);
          if (rt != null && !rt.isEmpty()) {
            foundOne = true;
          }
        }

        foundAll &= foundOne;
      }
      return foundAll;
    }
    //TODO check for FluidCrafter
    return false;
  }

  public void enabledUpdateEntity() {
    IPipeServiceProvider service = this._service;
    if (service != null) {
      if (service.getItemOrderManager().hasOrders(new ResourceType[]{ResourceType.CRAFTING, ResourceType.EXTRA})) {
        if (service.isNthTick(6)) {
          this.cacheAreAllOrderesToBuffer();
        }

        if (service.getItemOrderManager().isFirstOrderWatched()) {
          TileEntity tile = (TileEntity) this.lastAccessedCrafter.get();
          if (tile != null) {
            service.getItemOrderManager().setMachineProgress(SimpleServiceLocator.machineProgressProvider.getProgressForTile(tile));
          } else {
            service.getItemOrderManager().setMachineProgress((byte) 0);
          }
        }
      } else {
        this.cachedAreAllOrderesToBuffer = false;
      }

      if (service.isNthTick(6)) {
        try {
          // As our crafting manager is properly setup, we will get result pipe service for item collecting
          resultService = pipeCM.getCMResultRouter().getPipe();
          List<NeighborTileEntity<TileEntity>> adjacentInventories = resultService.getAvailableAdjacent().inventories();
          if (!service.getItemOrderManager().hasOrders(new ResourceType[]{ResourceType.CRAFTING, ResourceType.EXTRA})) {
            ISlotUpgradeManager upgradeManager = (ISlotUpgradeManager) Objects.requireNonNull(this.getUpgradeManager());
            if (upgradeManager.getCrafterCleanup() > 0) {
              adjacentInventories.stream().map((neighbor) -> {
                return this.extractFiltered(neighbor, this.cleanupInventory, (Boolean) this.cleanupModeIsExclude.getValue(), upgradeManager.getCrafterCleanup() * 3);
              }).filter((stack) -> {
                return !stack.isEmpty();
              }).findFirst().ifPresent((extractedx) -> {
                service.queueRoutedItem(SimpleServiceLocator.routedItemHelper.createNewTravelItem(extractedx), EnumFacing.UP);
                service.getCacheHolder().trigger(CacheHolder.CacheTypes.Inventory);
              });
            }

          } else if (adjacentInventories.size() < 1) {
            if (service.getItemOrderManager().hasOrders(new ResourceType[]{ResourceType.CRAFTING, ResourceType.EXTRA})) {
              service.getItemOrderManager().sendFailed();
            }

          } else {
            List<ItemIdentifierStack> wanteditem = this.getCraftedItems();
            if (wanteditem != null && !wanteditem.isEmpty()) {
              resultService.spawnParticle(Particles.VioletParticle, 2);
              int itemsleft = this.itemsToExtract();
              int stacksleft = this.stacksToExtract();

              label123:
              while (itemsleft > 0 && stacksleft > 0 && service.getItemOrderManager().hasOrders(new ResourceType[]{ResourceType.CRAFTING, ResourceType.EXTRA})) {
                LogisticsItemOrder nextOrder = (LogisticsItemOrder) service.getItemOrderManager().peekAtTopRequest(new ResourceType[]{ResourceType.CRAFTING, ResourceType.EXTRA});
                int maxtosend = Math.min(itemsleft, nextOrder.getResource().stack.getStackSize());
                maxtosend = Math.min(nextOrder.getResource().getItem().getMaxStackSize(), maxtosend);
                ItemStack extracted = ItemStack.EMPTY;
                NeighborTileEntity<TileEntity> adjacent = null;
                Iterator var10 = adjacentInventories.iterator();

                while (var10.hasNext()) {
                  NeighborTileEntity<TileEntity> adjacentCrafter = (NeighborTileEntity) var10.next();
                  adjacent = adjacentCrafter;
                  extracted = this.extract(adjacentCrafter, nextOrder.getResource(), maxtosend);
                  if (!extracted.isEmpty()) {
                    break;
                  }
                }

                if (extracted.isEmpty()) {
                  service.getItemOrderManager().deferSend();
                  break;
                }

                service.getCacheHolder().trigger(CacheHolder.CacheTypes.Inventory);
                Objects.requireNonNull(adjacent);
                this.lastAccessedCrafter = new WeakReference(adjacent.getTileEntity());
                ItemIdentifier extractedID = ItemIdentifier.get(extracted);

                while (true) {
                  while (true) {
                    if (extracted.isEmpty()) {
                      continue label123;
                    }

                    if (this.isExtractedMismatch(nextOrder, extractedID)) {
                      LogisticsItemOrder startOrder = nextOrder;
                      if (service.getItemOrderManager().hasOrders(new ResourceType[]{ResourceType.CRAFTING, ResourceType.EXTRA})) {
                        do {
                          service.getItemOrderManager().deferSend();
                          nextOrder = (LogisticsItemOrder) service.getItemOrderManager().peekAtTopRequest(new ResourceType[]{ResourceType.CRAFTING, ResourceType.EXTRA});
                        } while (this.isExtractedMismatch(nextOrder, extractedID) && startOrder != nextOrder);
                      }

                      if (startOrder == nextOrder) {
                        int numtosend = Math.min(extracted.getCount(), extractedID.getMaxStackSize());
                        if (numtosend == 0) {
                          continue label123;
                        }

                        --stacksleft;
                        itemsleft -= numtosend;
                        ItemStack stackToSend = extracted.splitStack(numtosend);
                        service.sendStack(stackToSend, -1, CoreRoutedPipe.ItemSendMode.Normal, (IAdditionalTargetInformation) null, adjacent.getDirection());
                        continue;
                      }
                    }

                    int numtosend = Math.min(extracted.getCount(), extractedID.getMaxStackSize());
                    numtosend = Math.min(numtosend, nextOrder.getResource().stack.getStackSize());
                    if (numtosend == 0) {
                      continue label123;
                    }

                    --stacksleft;
                    itemsleft -= numtosend;
                    ItemStack stackToSend = extracted.splitStack(numtosend);
                    if (nextOrder.getDestination() != null) {
                      SinkReply reply = LogisticsManager.canSink(stackToSend, nextOrder.getDestination().getRouter(), (IRouter) null, true, ItemIdentifier.get(stackToSend), (SinkReply) null, true, false);
                      boolean defersend = reply == null || reply.bufferMode != SinkReply.BufferMode.NONE || reply.maxNumberOfItems < 1;
                      IRoutedItem item = SimpleServiceLocator.routedItemHelper.createNewTravelItem(stackToSend);
                      item.setDestination(nextOrder.getDestination().getRouter().getSimpleID());
                      item.setTransportMode(IRoutedItem.TransportMode.Active);
                      item.setAdditionalTargetInformation(nextOrder.getInformation());
                      resultService.queueRoutedItem(item, adjacent.getDirection());
                      service.getItemOrderManager().sendSuccessfull(stackToSend.getCount(), defersend, item);
                    } else {
                      resultService.sendStack(stackToSend, -1, CoreRoutedPipe.ItemSendMode.Normal, nextOrder.getInformation(), adjacent.getDirection());
                      service.getItemOrderManager().sendSuccessfull(stackToSend.getCount(), false, (IRoutedItem) null);
                    }

                    if (service.getItemOrderManager().hasOrders(new ResourceType[]{ResourceType.CRAFTING, ResourceType.EXTRA})) {
                      nextOrder = (LogisticsItemOrder) service.getItemOrderManager().peekAtTopRequest(new ResourceType[]{ResourceType.CRAFTING, ResourceType.EXTRA});
                    }
                  }
                }
              }

            }
          }
        } catch (NullPointerException ignored) {}
      }
    }
  }

  private boolean isExtractedMismatch(LogisticsItemOrder nextOrder, ItemIdentifier extractedID) {
    return !nextOrder.getResource().getItem().equals(extractedID) && (!this.getUpgradeManager().isFuzzyUpgrade() || nextOrder.getResource().getBitSet().nextSetBit(0) == -1 || !nextOrder.getResource().matches(extractedID, IResource.MatchSettings.NORMAL));
  }

  public boolean areAllOrderesToBuffer() {
    return this.cachedAreAllOrderesToBuffer;
  }

  public void cacheAreAllOrderesToBuffer() {
    IPipeServiceProvider service = this._service;
    if (service != null) {
      boolean result = true;
      Iterator var3 = service.getItemOrderManager().iterator();

      while(var3.hasNext()) {
        LogisticsItemOrder order = (LogisticsItemOrder)var3.next();
        if (order.getDestination() instanceof IItemSpaceControl) {
          SinkReply reply = LogisticsManager.canSink(order.getResource().stack.makeNormalStack(), order.getDestination().getRouter(), (IRouter)null, true, order.getResource().getItem(), (SinkReply)null, true, false);
          if (reply == null || reply.bufferMode != SinkReply.BufferMode.NONE || reply.maxNumberOfItems < 1) {
            continue;
          }

          result = false;
          break;
        }

        result = false;
        break;
      }

      this.cachedAreAllOrderesToBuffer = result;
    }
  }

  @Nonnull
  private ItemStack extract(NeighborTileEntity<TileEntity> adjacent, IResource item, int amount) {
    return (ItemStack) LPNeighborTileEntityKt.optionalIs(adjacent, LogisticsCraftingTableTileEntity.class).map((adjacentCraftingTable) -> {
      return this.extractFromLogisticsCraftingTable(adjacentCraftingTable, item, amount);
    }).orElseGet(() -> {
      IInventoryUtil invUtil = LPNeighborTileEntityKt.getInventoryUtil(adjacent);
      return invUtil == null ? ItemStack.EMPTY : this.extractFromInventory(invUtil, item, amount);
    });
  }

  @Nonnull
  private ItemStack extractFiltered(NeighborTileEntity<TileEntity> neighbor, IItemIdentifierInventory inv, boolean isExcluded, int filterInvLimit) {
    IInventoryUtil invUtil = LPNeighborTileEntityKt.getInventoryUtil(neighbor);
    return invUtil == null ? ItemStack.EMPTY : this.extractFromInventoryFiltered(invUtil, inv, isExcluded, filterInvLimit);
  }

  @Nonnull
  private ItemStack extractFromInventory(@Nonnull IInventoryUtil invUtil, IResource wanteditem, int count) {
    IPipeServiceProvider service = this._service;
    if (service == null) {
      return ItemStack.EMPTY;
    } else {
      ItemIdentifier itemToExtract = null;
      int max;
      if (wanteditem instanceof ItemResource) {
        itemToExtract = ((ItemResource)wanteditem).getItem();
      } else if (wanteditem instanceof DictResource) {
        max = Integer.MIN_VALUE;
        ItemIdentifier toExtract = null;
        Iterator var8 = invUtil.getItemsAndCount().entrySet().iterator();

        while(var8.hasNext()) {
          Map.Entry<ItemIdentifier, Integer> content = (Map.Entry)var8.next();
          if (wanteditem.matches((ItemIdentifier)content.getKey(), IResource.MatchSettings.NORMAL) && (Integer)content.getValue() > max) {
            max = (Integer)content.getValue();
            toExtract = (ItemIdentifier)content.getKey();
          }
        }

        if (toExtract == null) {
          return ItemStack.EMPTY;
        }

        itemToExtract = toExtract;
      }

      if (itemToExtract == null) {
        return ItemStack.EMPTY;
      } else {
        max = invUtil.itemCount(itemToExtract);
        if (max != 0 && service.canUseEnergy(this.neededEnergy() * Math.min(count, max))) {
          ItemStack extracted = invUtil.getMultipleItems(itemToExtract, Math.min(count, max));
          service.useEnergy(this.neededEnergy() * extracted.getCount());
          return extracted;
        } else {
          return ItemStack.EMPTY;
        }
      }
    }
  }

  @Nonnull
  private ItemStack extractFromInventoryFiltered(@Nonnull IInventoryUtil invUtil, IItemIdentifierInventory filter, boolean isExcluded, int filterInvLimit) {
    IPipeServiceProvider service = this._service;
    if (service == null) {
      return ItemStack.EMPTY;
    } else {
      ItemIdentifier wanteditem = null;
      boolean found = false;
      Iterator var8 = invUtil.getItemsAndCount().keySet().iterator();

      while(var8.hasNext()) {
        ItemIdentifier item = (ItemIdentifier)var8.next();
        found = this.isFiltered(filter, filterInvLimit, item, found);
        if (isExcluded != found) {
          wanteditem = item;
          break;
        }
      }

      if (wanteditem == null) {
        return ItemStack.EMPTY;
      } else {
        int available = invUtil.itemCount(wanteditem);
        if (available != 0 && service.canUseEnergy(this.neededEnergy() * Math.min(64, available))) {
          ItemStack extracted = invUtil.getMultipleItems(wanteditem, Math.min(64, available));
          service.useEnergy(this.neededEnergy() * extracted.getCount());
          return extracted;
        } else {
          return ItemStack.EMPTY;
        }
      }
    }
  }

  private boolean isFiltered(IItemIdentifierInventory filter, int filterInvLimit, ItemIdentifier item, boolean found) {
    for(int i = 0; i < filter.getSizeInventory() && i < filterInvLimit; ++i) {
      ItemIdentifierStack identStack = filter.getIDStackInSlot(i);
      if (identStack != null && identStack.getItem().equalsWithoutNBT(item)) {
        found = true;
        break;
      }
    }

    return found;
  }

  @Nonnull
  private ItemStack extractFromLogisticsCraftingTable(NeighborTileEntity<LogisticsCraftingTableTileEntity> adjacentCraftingTable, IResource wanteditem, int count) {
    IPipeServiceProvider service = this._service;
    if (service == null) {
      return ItemStack.EMPTY;
    } else {
      ItemStack extracted = this.extractFromInventory((IInventoryUtil)Objects.requireNonNull(LPNeighborTileEntityKt.getInventoryUtil(adjacentCraftingTable)), wanteditem, count);
      if (!extracted.isEmpty()) {
        return extracted;
      } else {
        ItemStack retstack = ItemStack.EMPTY;

        while(count > 0) {
          ItemStack stack = ((LogisticsCraftingTableTileEntity)adjacentCraftingTable.getTileEntity()).getOutput(wanteditem, service);
          if (stack.isEmpty()) {
            break;
          }

          if (retstack.isEmpty()) {
            if (!wanteditem.matches(ItemIdentifier.get(stack), wanteditem instanceof ItemResource ? IResource.MatchSettings.WITHOUT_NBT : IResource.MatchSettings.NORMAL)) {
              break;
            }
          } else if (!retstack.isItemEqual(stack) || !ItemStack.areItemStackTagsEqual(retstack, stack)) {
            break;
          }

          if (!service.useEnergy(this.neededEnergy() * stack.getCount())) {
            break;
          }

          if (retstack.isEmpty()) {
            retstack = stack;
          } else {
            retstack.grow(stack.getCount());
          }

          count -= stack.getCount();
          if (((ISlotUpgradeManager)Objects.requireNonNull(this.getUpgradeManager())).isFuzzyUpgrade()) {
            break;
          }
        }

        return retstack;
      }
    }
  }

  @Override
  protected int neededEnergy() {
    return (int) (10 * Math.pow(1.1, getUpgradeManager().getItemExtractionUpgrade()) * Math
        .pow(1.2, getUpgradeManager().getItemStackExtractionUpgrade()));
  }

  @Override
  protected int itemsToExtract() {
    return (int) Math.pow(2, getUpgradeManager().getItemExtractionUpgrade());
  }

  @Override
  protected int stacksToExtract() {
    return 1 + getUpgradeManager().getItemStackExtractionUpgrade();
  }

  @Nonnull
  protected ISlotUpgradeManager getUpgradeManager() {
    return Objects.requireNonNull(resultService, "service object was null in " + this)
        .getUpgradeManager(slot, positionInt);
  }

}
