package testbridge.modules;

import java.lang.ref.WeakReference;
import java.util.*;
import javax.annotation.Nonnull;

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
import network.rs485.logisticspipes.property.UUIDProperty;

import testbridge.pipes.PipeCraftingManager;
import testbridge.pipes.PipeCraftingManager.CMTargetInformation;
import testbridge.helpers.IISHelper;

public class TB_ModuleCrafter extends ModuleCrafter {
  private WeakReference<TileEntity> lastAccessedCrafter = new WeakReference<>(null);
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
    this.slot = slot;
    this.positionInt = positionInt;
    _sinkReply = new SinkReply(SinkReply.FixedPriority.ItemSink, 0, true, false, 1, 0,
        new CMTargetInformation(getPositionInt()));
  }

  @Override
  public LogisticsItemOrder fullFill(LogisticsPromise promise, IRequestItems destination,
                                     IAdditionalTargetInformation info) {
    if (pipeCM == null) return null;
    ItemIdentifierStack result = getCraftedItem();
    if (result == null) return null;
    //
    int multiply = (int) Math.ceil(promise.numberOfItems / (float) result.getStackSize());
    if(pipeCM.hasBufferUpgrade()){
      boolean hasSatellite = isSatelliteConnected();
      if (!hasSatellite) {
        return null;
      }

      IRouter defSat = pipeCM.getMainSatelliteRouter();
      if (defSat == null) return null;
      IRequestItems[] target = new IRequestItems[9];

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

      for (int i = 0; i < 9; i++) {
        if (target[i] == null) {
          target[i] = defSat.getPipe();
        }
      }

      HashMap<IRequestItems, List<ItemIdentifierStack>> buffer = new HashMap<>();

      for (int i = 0; i < target.length; i++) {
        ItemIdentifierStack materials = dummyInventory.getIDStackInSlot(i);
        if (materials != null) {
          buffer.computeIfAbsent(target[i], k -> new ArrayList<>());
          buffer.get(target[i]).add(materials);
        }
      }

      // Applying the combine method to the materials
      for (IRequestItems router : buffer.keySet()) {
        buffer.computeIfPresent(router, (k, v) -> IISHelper.combine(buffer.get(k)));
      }

      for(int i = 0 ; i < multiply ; i++)
        pipeCM.getModules().addToCraftList(buffer);
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
      IRequestItems defsat = pipeCM.getMainSatelliteRouter().getPipe();
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
    final UUID satelliteUUID = x == -1 ? this.satelliteUUID.getValue() : this.advancedSatelliteUUIDList.get(x);
    int satelliteRouterId = SimpleServiceLocator.routerManager.getIDforUUID(satelliteUUID);
    return SimpleServiceLocator.routerManager.getRouter(satelliteRouterId);
  }

  private IRouter getFluidSatelliteRouter(int x) {
    final UUID liquidSatelliteUUID = x == -1 ? this.liquidSatelliteUUID.getValue() : this.liquidSatelliteUUIDList.get(x);
    int satelliteRouterId = SimpleServiceLocator.routerManager.getIDforUUID(liquidSatelliteUUID);
    return SimpleServiceLocator.routerManager.getRouter(satelliteRouterId);
  }

  @Override
  public boolean isSatelliteConnected() {
    if (!checkConnectionByUUID(pipeCM.getModules().getResultUUID())) {
      return false;
    }

    if (!getUpgradeManager().isAdvancedSatelliteCrafter()) {
      if (!checkConnectionByUUID(pipeCM.getModules().getSatelliteUUID())) {
        return false;
      }

      if (satelliteUUID.isZero()) {
        return true;
      }

      return checkConnectionByUUID(satelliteUUID);
    } else {
      boolean foundAll = true;

      if (!checkConnectionByUUID(pipeCM.getModules().getSatelliteUUID())) {
        return false;
      }

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
  }

  public void enabledUpdateEntity() {
    final IPipeServiceProvider service = _service;
    if (service == null) return;

    if (service.getItemOrderManager().hasOrders(ResourceType.CRAFTING, ResourceType.EXTRA)) {
      if (service.isNthTick(6)) {
        cacheAreAllOrderesToBuffer();
      }
      if (service.getItemOrderManager().isFirstOrderWatched()) {
        TileEntity tile = lastAccessedCrafter.get();
        if (tile != null) {
          service.getItemOrderManager()
              .setMachineProgress(SimpleServiceLocator.machineProgressProvider.getProgressForTile(tile));
        } else {
          service.getItemOrderManager().setMachineProgress((byte) 0);
        }
      }
    } else {
      cachedAreAllOrderesToBuffer = false;
    }

    if (!service.isNthTick(6)) {
      return;
    }

    try {
      // As our crafting manager is properly setup, we will get result pipe service for item collecting
      resultService = pipeCM.getMainResultRouter().getPipe();

      final List<NeighborTileEntity<TileEntity>> adjacentInventories = resultService.getAvailableAdjacent().inventories();

      if (!service.getItemOrderManager().hasOrders(ResourceType.CRAFTING, ResourceType.EXTRA)) {
        final ISlotUpgradeManager upgradeManager = Objects.requireNonNull(getUpgradeManager());
        if (upgradeManager.getCrafterCleanup() > 0) {
          adjacentInventories.stream()
              .map(neighbor -> extractFiltered(neighbor, cleanupInventory, cleanupModeIsExclude.getValue(),
                  upgradeManager.getCrafterCleanup() * 3)).filter(stack -> !stack.isEmpty()).findFirst()
              .ifPresent(extracted -> {
                service.queueRoutedItem(
                    SimpleServiceLocator.routedItemHelper.createNewTravelItem(extracted),
                    EnumFacing.UP);
                service.getCacheHolder().trigger(CacheHolder.CacheTypes.Inventory);
              });
        }
        return;
      }

      if (adjacentInventories.size() < 1) {
        if (service.getItemOrderManager().hasOrders(ResourceType.CRAFTING, ResourceType.EXTRA)) {
          service.getItemOrderManager().sendFailed();
        }
        return;
      }

      List<ItemIdentifierStack> wanteditem = getCraftedItems();
      if (wanteditem == null || wanteditem.isEmpty()) {
        return;
      }

      resultService.spawnParticle(Particles.VioletParticle, 2);

      int itemsleft = itemsToExtract();
      int stacksleft = stacksToExtract();
      while (itemsleft > 0 && stacksleft > 0 && (service.getItemOrderManager()
          .hasOrders(ResourceType.CRAFTING, ResourceType.EXTRA))) {
        LogisticsItemOrder nextOrder = service.getItemOrderManager()
            .peekAtTopRequest(ResourceType.CRAFTING, ResourceType.EXTRA); // fetch but not remove.
        int maxtosend = Math.min(itemsleft, nextOrder.getResource().stack.getStackSize());
        maxtosend = Math.min(nextOrder.getResource().getItem().getMaxStackSize(), maxtosend);
        // retrieve the new crafted items
        ItemStack extracted = ItemStack.EMPTY;
        NeighborTileEntity<TileEntity> adjacent = null; // there has to be at least one adjacentCrafter at this point; adjacent won't stay null
        for (NeighborTileEntity<TileEntity> adjacentCrafter : adjacentInventories) {
          adjacent = adjacentCrafter;
          extracted = extract(adjacent, nextOrder.getResource(), maxtosend);
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
        lastAccessedCrafter = new WeakReference<>(adjacent.getTileEntity());
        // send the new crafted items to the destination
        ItemIdentifier extractedID = ItemIdentifier.get(extracted);
        while (!extracted.isEmpty()) {
          if (isExtractedMismatch(nextOrder, extractedID)) {
            LogisticsItemOrder startOrder = nextOrder;
            if (service.getItemOrderManager().hasOrders(ResourceType.CRAFTING, ResourceType.EXTRA)) {
              do {
                service.getItemOrderManager().deferSend();
                nextOrder = service.getItemOrderManager()
                    .peekAtTopRequest(ResourceType.CRAFTING, ResourceType.EXTRA);
              } while (isExtractedMismatch(nextOrder, extractedID) && startOrder != nextOrder);
            }
            if (startOrder == nextOrder) {
              int numtosend = Math.min(extracted.getCount(), extractedID.getMaxStackSize());
              if (numtosend == 0) {
                break;
              }
              stacksleft -= 1;
              itemsleft -= numtosend;
              ItemStack stackToSend = extracted.splitStack(numtosend);
              //Route the unhandled item

              service.sendStack(stackToSend, -1, CoreRoutedPipe.ItemSendMode.Normal, null, adjacent.getDirection());
              continue;
            }
          }
          int numtosend = Math.min(extracted.getCount(), extractedID.getMaxStackSize());
          numtosend = Math.min(numtosend, nextOrder.getResource().stack.getStackSize());
          if (numtosend == 0) {
            break;
          }
          stacksleft -= 1;
          itemsleft -= numtosend;
          ItemStack stackToSend = extracted.splitStack(numtosend);
          if (nextOrder.getDestination() != null) {
            SinkReply reply = LogisticsManager
                .canSink(stackToSend, nextOrder.getDestination().getRouter(), null, true,
                    ItemIdentifier.get(stackToSend), null, true, false);
            boolean defersend = (reply == null || reply.bufferMode != SinkReply.BufferMode.NONE
                || reply.maxNumberOfItems < 1);
            IRoutedItem item = SimpleServiceLocator.routedItemHelper.createNewTravelItem(stackToSend);
            item.setDestination(nextOrder.getDestination().getRouter().getSimpleID());
            item.setTransportMode(IRoutedItem.TransportMode.Active);
            item.setAdditionalTargetInformation(nextOrder.getInformation());
            resultService.queueRoutedItem(item, adjacent.getDirection());
            service.getItemOrderManager().sendSuccessfull(stackToSend.getCount(), defersend, item);
          } else {
            resultService.sendStack(stackToSend, -1, CoreRoutedPipe.ItemSendMode.Normal, nextOrder.getInformation(),
                adjacent.getDirection());
            service.getItemOrderManager().sendSuccessfull(stackToSend.getCount(), false, null);
          }
          if (service.getItemOrderManager().hasOrders(ResourceType.CRAFTING, ResourceType.EXTRA)) {
            nextOrder = service.getItemOrderManager()
                .peekAtTopRequest(ResourceType.CRAFTING, ResourceType.EXTRA); // fetch but not remove.
          }
        }
      }
    } catch (NullPointerException ignore) {}
  }

  private boolean isExtractedMismatch(LogisticsItemOrder nextOrder, ItemIdentifier extractedID) {
    return !nextOrder.getResource().getItem().equals(extractedID) && (!getUpgradeManager().isFuzzyUpgrade() || (
        nextOrder.getResource().getBitSet().nextSetBit(0) == -1) || !nextOrder.getResource()
        .matches(extractedID, IResource.MatchSettings.NORMAL));
  }

  public boolean areAllOrderesToBuffer() {
    return this.cachedAreAllOrderesToBuffer;
  }

  public void cacheAreAllOrderesToBuffer() {
    final IPipeServiceProvider service = _service;
    if (service == null) return;
    boolean result = true;
    for (LogisticsItemOrder order : service.getItemOrderManager()) {
      if (order.getDestination() instanceof IItemSpaceControl) {
        SinkReply reply = LogisticsManager
            .canSink(order.getResource().stack.makeNormalStack(), order.getDestination().getRouter(), null,
                true, order.getResource().getItem(), null, true, false);
        if (reply != null && reply.bufferMode == SinkReply.BufferMode.NONE && reply.maxNumberOfItems >= 1) {
          result = false;
          break;
        }
      } else { // No Space control
        result = false;
        break;
      }
    }
    cachedAreAllOrderesToBuffer = result;
  }

  @Nonnull
  private ItemStack extract(NeighborTileEntity<TileEntity> adjacent, IResource item, int amount) {
    return LPNeighborTileEntityKt.optionalIs(adjacent, LogisticsCraftingTableTileEntity.class)
        .map(adjacentCraftingTable -> extractFromLogisticsCraftingTable(adjacentCraftingTable, item, amount))
        .orElseGet(() -> {
          final IInventoryUtil invUtil = LPNeighborTileEntityKt.getInventoryUtil(adjacent);
          if (invUtil == null) return ItemStack.EMPTY;
          return extractFromInventory(invUtil, item, amount);
        });
  }

  @Nonnull
  private ItemStack extractFiltered(NeighborTileEntity<TileEntity> neighbor, IItemIdentifierInventory inv, boolean isExcluded, int filterInvLimit) {
    final IInventoryUtil invUtil = LPNeighborTileEntityKt.getInventoryUtil(neighbor);
    if (invUtil == null) return ItemStack.EMPTY;
    return extractFromInventoryFiltered(invUtil, inv, isExcluded, filterInvLimit);
  }

  @Nonnull
  private ItemStack extractFromInventory(@Nonnull IInventoryUtil invUtil, IResource wanteditem, int count) {
    final IPipeServiceProvider service = _service;
    if (service == null) return ItemStack.EMPTY;
    ItemIdentifier itemToExtract = null;
    if (wanteditem instanceof ItemResource) {
      itemToExtract = ((ItemResource) wanteditem).getItem();
    } else if (wanteditem instanceof DictResource) {
      int max = Integer.MIN_VALUE;
      ItemIdentifier toExtract = null;
      for (Map.Entry<ItemIdentifier, Integer> content : invUtil.getItemsAndCount().entrySet()) {
        if (wanteditem.matches(content.getKey(), IResource.MatchSettings.NORMAL)) {
          if (content.getValue() > max) {
            max = content.getValue();
            toExtract = content.getKey();
          }
        }
      }
      if (toExtract == null) {
        return ItemStack.EMPTY;
      }
      itemToExtract = toExtract;
    }
    if (itemToExtract == null) return ItemStack.EMPTY;
    int available = invUtil.itemCount(itemToExtract);
    if (available == 0 || !service.canUseEnergy(neededEnergy() * Math.min(count, available))) {
      return ItemStack.EMPTY;
    }
    ItemStack extracted = invUtil.getMultipleItems(itemToExtract, Math.min(count, available));
    service.useEnergy(neededEnergy() * extracted.getCount());
    return extracted;
  }

  @Nonnull
  private ItemStack extractFromInventoryFiltered(@Nonnull IInventoryUtil invUtil, IItemIdentifierInventory filter, boolean isExcluded, int filterInvLimit) {
    final IPipeServiceProvider service = _service;
    if (service == null) return ItemStack.EMPTY;

    ItemIdentifier wanteditem = null;
    boolean found = false;
    for (ItemIdentifier item : invUtil.getItemsAndCount().keySet()) {
      found = isFiltered(filter, filterInvLimit, item, found);
      if (isExcluded != found) {
        wanteditem = item;
        break;
      }
    }
    if (wanteditem == null) {
      return ItemStack.EMPTY;
    }
    int available = invUtil.itemCount(wanteditem);
    if (available == 0 || !service.canUseEnergy(neededEnergy() * Math.min(64, available))) {
      return ItemStack.EMPTY;
    }
    ItemStack extracted = invUtil.getMultipleItems(wanteditem, Math.min(64, available));
    service.useEnergy(neededEnergy() * extracted.getCount());
    return extracted;
  }

  private boolean isFiltered(IItemIdentifierInventory filter, int filterInvLimit, ItemIdentifier item, boolean found) {
    for (int i = 0; i < filter.getSizeInventory() && i < filterInvLimit; i++) {
      ItemIdentifierStack identStack = filter.getIDStackInSlot(i);
      if (identStack == null) {
        continue;
      }
      if (identStack.getItem().equalsWithoutNBT(item)) {
        found = true;
        break;
      }
    }
    return found;
  }

  @Nonnull
  private ItemStack extractFromLogisticsCraftingTable(NeighborTileEntity<LogisticsCraftingTableTileEntity> adjacentCraftingTable, IResource wanteditem, int count) {
    final IPipeServiceProvider service = _service;
    if (service == null) return ItemStack.EMPTY;
    ItemStack extracted = extractFromInventory(
        Objects.requireNonNull(LPNeighborTileEntityKt.getInventoryUtil(adjacentCraftingTable)), wanteditem,
        count);
    if (!extracted.isEmpty()) {
      return extracted;
    }
    ItemStack retstack = ItemStack.EMPTY;
    while (count > 0) {
      ItemStack stack = adjacentCraftingTable.getTileEntity().getOutput(wanteditem, service);
      if (stack.isEmpty()) {
        break;
      }
      if (retstack.isEmpty()) {
        if (!wanteditem.matches(ItemIdentifier.get(stack), wanteditem instanceof ItemResource ?
            IResource.MatchSettings.WITHOUT_NBT :
            IResource.MatchSettings.NORMAL)) {
          break;
        }
      } else {
        if (!retstack.isItemEqual(stack)) {
          break;
        }
        if (!ItemStack.areItemStackTagsEqual(retstack, stack)) {
          break;
        }
      }
      if (!service.useEnergy(neededEnergy() * stack.getCount())) {
        break;
      }

      if (retstack.isEmpty()) {
        retstack = stack;
      } else {
        retstack.grow(stack.getCount());
      }
      count -= stack.getCount();
      if (Objects.requireNonNull(getUpgradeManager()).isFuzzyUpgrade()) {
        break;
      }
    }
    return retstack;
  }

  /**
   * Simple method to check if pipe is present on route table
   * @param UUID UUIDProperty
   * @return Connecting state
   */
  private boolean checkConnectionByUUID(UUIDProperty UUID) {
    try {
      if (!UUID.isZero()) {
        int routerId = SimpleServiceLocator.routerManager.getIDforUUID(UUID.getValue());
        if (routerId != -1) {
          List<ExitRoute> exitRoutes = getRouter().getRouteTable().get(routerId);
          return exitRoutes != null && !exitRoutes.isEmpty();
        }
      }
    } catch (IndexOutOfBoundsException ignore) {}
    return false;
  }

  @Override
  protected int neededEnergy() {
    return (int) (10 * Math.pow(1.1, getResultUM().getItemExtractionUpgrade()) * Math
        .pow(1.2, getResultUM().getItemStackExtractionUpgrade()));
  }

  @Override
  protected int itemsToExtract() {
    return (int) Math.pow(2, getResultUM().getItemExtractionUpgrade());
  }

  @Override
  protected int stacksToExtract() {
    return 1 + getResultUM().getItemStackExtractionUpgrade();
  }

  @Nonnull
  protected ISlotUpgradeManager getUpgradeManager() {
    return Objects.requireNonNull(_service, "service object was null in " + this)
        .getUpgradeManager(slot, positionInt);
  }

  @Nonnull
  protected ISlotUpgradeManager getResultUM() {
    return Objects.requireNonNull(resultService, "service object was null in " + this)
        .getUpgradeManager(slot, positionInt);
  }

}
