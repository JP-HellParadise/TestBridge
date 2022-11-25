package testbridge.modules;

import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.interfaces.IWorldProvider;
import logisticspipes.interfaces.routing.IRequestFluid;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.logistics.LogisticsManager;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.logisticspipes.IRoutedItem.TransportMode;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.CoreRoutedPipe.ItemSendMode;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.request.DictCraftingTemplate;
import logisticspipes.request.ICraftingTemplate;
import logisticspipes.request.IReqCraftingTemplate;
import logisticspipes.request.ItemCraftingTemplate;
import logisticspipes.request.resources.DictResource;
import logisticspipes.request.resources.FluidResource;
import logisticspipes.request.resources.IResource;
import logisticspipes.request.resources.ItemResource;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.order.IOrderInfoProvider.ResourceType;
import logisticspipes.routing.order.LogisticsItemOrder;
import logisticspipes.utils.CacheHolder;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.SinkReply.BufferMode;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import testbridge.mixins.logisticspipes.modules.TB_AccessorModuleCrafter;
import testbridge.pipes.PipeCraftingManager;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class TB_ModuleCrafter extends ModuleCrafter {
  private PipeCraftingManager pipeCM;

  public TB_ModuleCrafter(){
    super();
  }

  public void registerHandler(IWorldProvider world, IPipeServiceProvider service) {
    super.registerHandler(world, service);
    pipeCM = (PipeCraftingManager) service;
  }

  /**
   * Now use Satellite and Result from Crafting Manager pipe
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

    IRequestItems defsat = getCMSatelliteRouter(pipeCM).getPipe();
    IRequestItems[] target = new IRequestItems[9];
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

  private IRouter getCMSatelliteRouter(PipeCraftingManager pipeCM) {
    final UUID satelliteUUID = pipeCM.getModules().satelliteUUID.getValue();
    final int satelliteRouterId = SimpleServiceLocator.routerManager.getIDforUUID(satelliteUUID);
    return SimpleServiceLocator.routerManager.getRouter(satelliteRouterId);
  }

  private IRouter getCMResultRouter(PipeCraftingManager pipeCM) {
    final UUID resultUUID = pipeCM.getModules().resultUUID.getValue();
    final int resultRouterId = SimpleServiceLocator.routerManager.getIDforUUID(resultUUID);
    return SimpleServiceLocator.routerManager.getRouter(resultRouterId);
  }

  private IRouter getFluidSatelliteRouter(int x) {
    UUID liquidSatelliteUUID = x == -1 ? (UUID)this.liquidSatelliteUUID.getValue() : (UUID)this.liquidSatelliteUUIDList.get(x);
    int satelliteRouterId = SimpleServiceLocator.routerManager.getIDforUUID(liquidSatelliteUUID);
    return SimpleServiceLocator.routerManager.getRouter(satelliteRouterId);
  }

  @Override
  public void enabledUpdateEntity() {
    final IPipeServiceProvider service = _service;
    if (service == null) return;

    if (service.getItemOrderManager().hasOrders(ResourceType.CRAFTING, ResourceType.EXTRA)) {
      if (service.isNthTick(6)) {
        cacheAreAllOrderesToBuffer();
      }
      if (service.getItemOrderManager().isFirstOrderWatched()) {
        TileEntity tile = ((TB_AccessorModuleCrafter) this).getLastAccessedCrafter().get();
        if (tile != null) {
          service.getItemOrderManager()
              .setMachineProgress(SimpleServiceLocator.machineProgressProvider.getProgressForTile(tile));
        } else {
          service.getItemOrderManager().setMachineProgress((byte) 0);
        }
      }
    } else {
      setCachedAreAllOrderesToBuffer(false);
    }

    if (!service.isNthTick(6)) {
      return;
    }

    final CoreRoutedPipe resultPipe;
    try {
      resultPipe = getCMResultRouter(pipeCM).getPipe();
    } catch (NullPointerException e) {return;}

    final List<NeighborTileEntity<TileEntity>> adjacentInventories = resultPipe.getAvailableAdjacent().inventories();

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

    service.spawnParticle(Particles.VioletParticle, 2);

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
      NeighborTileEntity<TileEntity> adjacent = null; // there has to be at least one adjacentCrafter at this point; adjacent wont stay null
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
      setLastAccessedCrafter(new WeakReference<>(adjacent.getTileEntity()));
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

            service.sendStack(stackToSend, -1, ItemSendMode.Normal, null, adjacent.getDirection());
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
          boolean defersend = (reply == null || reply.bufferMode != BufferMode.NONE
              || reply.maxNumberOfItems < 1);
          IRoutedItem item = SimpleServiceLocator.routedItemHelper.createNewTravelItem(stackToSend);
          item.setDestination(nextOrder.getDestination().getRouter().getSimpleID());
          item.setTransportMode(TransportMode.Active);
          item.setAdditionalTargetInformation(nextOrder.getInformation());
          service.queueRoutedItem(item, adjacent.getDirection());
          service.getItemOrderManager().sendSuccessfull(stackToSend.getCount(), defersend, item);
        } else {
          service.sendStack(stackToSend, -1, ItemSendMode.Normal, nextOrder.getInformation(),
              adjacent.getDirection());
          service.getItemOrderManager().sendSuccessfull(stackToSend.getCount(), false, null);
        }
        if (service.getItemOrderManager().hasOrders(ResourceType.CRAFTING, ResourceType.EXTRA)) {
          nextOrder = service.getItemOrderManager()
              .peekAtTopRequest(ResourceType.CRAFTING, ResourceType.EXTRA); // fetch but not remove.
        }
      }
    }

  }

  @Override
  public boolean isSatelliteConnected() {
    //final List<ExitRoute> routes = getRouter().getIRoutersByCost();
    if (!getUpgradeManager().isAdvancedSatelliteCrafter()) {
      if (satelliteUUID.isZero()) {
        if (pipeCM.getModules().satelliteUUID.isZero() || pipeCM.getModules().resultUUID.isZero()) {
          return true;
        }
        int satModuleRouterId = SimpleServiceLocator.routerManager.getIDforUUID(pipeCM.getModules().satelliteUUID.getValue());
        if (satModuleRouterId != -1) {
          List<ExitRoute> rt = getRouter().getRouteTable().get(satModuleRouterId);
          return rt != null && !rt.isEmpty();
        }
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


}
