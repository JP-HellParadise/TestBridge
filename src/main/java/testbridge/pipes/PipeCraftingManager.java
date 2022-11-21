package testbridge.pipes;

import logisticspipes.LPItems;
import logisticspipes.LogisticsPipes;
import logisticspipes.config.Configs;
import logisticspipes.interfaces.*;
import logisticspipes.interfaces.routing.*;
import logisticspipes.items.ItemModule;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.logisticspipes.TransportLayer;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.hud.HUDStartWatchingPacket;
import logisticspipes.network.packets.hud.HUDStopWatchingPacket;
import logisticspipes.network.packets.orderer.OrdererManagerContent;
import logisticspipes.network.packets.pipe.SendQueueContent;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.request.*;
import logisticspipes.request.resources.DictResource;
import logisticspipes.request.resources.IResource;
import logisticspipes.routing.LogisticsPromise;
import logisticspipes.routing.order.IOrderInfoProvider;
import logisticspipes.routing.order.LogisticsItemOrder;
import logisticspipes.routing.order.LogisticsItemOrderManager;
import logisticspipes.routing.order.LogisticsOrder;
import logisticspipes.security.SecuritySettings;
import logisticspipes.ticks.HudUpdateTick;
import logisticspipes.utils.EnumFacingUtil;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.tuples.Pair;

import lombok.Getter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.client.FMLClientHandler;

import network.rs485.logisticspipes.connection.*;
import network.rs485.logisticspipes.module.PipeServiceProviderUtilKt;
import network.rs485.logisticspipes.pipes.IChassisPipe;
import network.rs485.logisticspipes.property.SlottedModule;

import testbridge.gui.GuiCMPipe;
import testbridge.gui.hud.HudCMPipe;
import testbridge.logicrecon.CMTransportLayer;
import testbridge.modules.TB_ModuleCM;
import testbridge.network.packets.pipe.CMOrientationPacket;
import testbridge.network.packets.pipe.CMPipeModuleContent;
import testbridge.network.packets.pipe.RequestCMOrientationPacket;
import testbridge.pipes.upgrades.ModuleUpgradeManager;
import testbridge.textures.Textures;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CCType(name = "TBCraftingManagerPipe")
public class PipeCraftingManager extends CoreRoutedPipe
    implements ICraftItems, ISimpleInventoryEventHandler, ISendRoutedItem,
    IHeadUpDisplayRendererProvider, IChassisPipe, IChangeListener, ISendQueueContentRecieiver {

  private final TB_ModuleCM _module;
  private final ItemIdentifierInventory _moduleInventory;

  private final NonNullList<ModuleUpgradeManager> slotUpgradeManagers = NonNullList.create();

  private boolean init = false;
  public final LinkedList<ItemIdentifierStack> oldList = new LinkedList<>();
  // HUD
  public final LinkedList<ItemIdentifierStack> displayList = new LinkedList<>();
  public final PlayerCollectionList localModeWatchers = new PlayerCollectionList();
  private final HudCMPipe hud;

  private boolean doContentUpdate = true;

  @Nullable
  private SingleAdjacent pointedAdjacent = null;

  @Override
  @CCCommand(description = "Returns the size of this Chassie pipe")
  public int getChassisSize() {
    return 27;
  }

  public PipeCraftingManager(Item item) {
    super(item);
    _moduleInventory = new ItemIdentifierInventory(getChassisSize(), "Crafting Manager pipe", 1);
    _moduleInventory.addListener(this);

    _module = new TB_ModuleCM(getChassisSize(), this);
    _module.registerHandler(this, this);
    hud = new HudCMPipe(this, _moduleInventory);
    throttleTime = 40;
    _orderItemManager = new LogisticsItemOrderManager(this, this); // null by default when not needed
  }

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
   * Returns just the adjacent this chassis points at or no adjacent.
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
    MainProxy.sendPacketToAllWatchingChunk(_module, packet.setTilePos(container));
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

  private void updateModuleInventory() {
    _module.slottedModules().forEach(slottedModule -> {
      if (slottedModule.isEmpty()) {
        _moduleInventory.clearInventorySlotContents(slottedModule.getSlot());
        return;
      }
      final LogisticsModule module = Objects.requireNonNull(slottedModule.getModule());
      final ItemIdentifierStack idStack = _moduleInventory.getIDStackInSlot(slottedModule.getSlot());
      ItemStack moduleStack;
      if (idStack != null) {
        moduleStack = idStack.getItem().makeNormalStack(1);
      } else {
        ResourceLocation resourceLocation = LPItems.modules.get(module.getLPName());
        Item item = Item.REGISTRY.getObject(resourceLocation);
        if (item == null) return;
        moduleStack = new ItemStack(item);
      }
      ItemModuleInformationManager.saveInformation(moduleStack, module);
      _moduleInventory.setInventorySlotContents(slottedModule.getSlot(), moduleStack);
    });
  }

  @Override
  @Nonnull
  public IInventory getModuleInventory() {
    updateModuleInventory();
    return _moduleInventory;
  }

  public ModuleUpgradeManager getModuleUpgradeManager(int slot) {
    return slotUpgradeManagers.get(slot);
  }

  @Override
  public TextureType getCenterTexture() {
    return Textures.TESTBRIDGE_TEXTURE;
  }

  @Override
  public TextureType getRoutedTexture(EnumFacing connection) {
    if (getRouter().isSubPoweredExit(connection)) {
      return Textures.TESTBRIDGE_TEXTURE;
    }
    return Textures.TESTBRIDGE_TEXTURE;
  }

  @Override
  public TextureType getNonRoutedTexture(EnumFacing connection) {
    if (pointedAdjacent != null && connection.equals(pointedAdjacent.getDir())) {
      return Textures.TESTBRIDGE_TEXTURE;
    }
    if (isPowerProvider(connection)) {
      return Textures.TESTBRIDGE_TEXTURE;
    }
    return Textures.TESTBRIDGE_TEXTURE;
  }

  @Override
  public void readFromNBT(@Nonnull NBTTagCompound nbttagcompound) {
    super.readFromNBT(nbttagcompound);
    _moduleInventory.readFromNBT(nbttagcompound, "crafting_manager");
    _module.readFromNBT(nbttagcompound);
    int tmp = nbttagcompound.getInteger("Orientation");
    if (tmp != -1) {
      setPointedOrientation(EnumFacingUtil.getOrientation(tmp % 6));
    }

    for (int i = 0; i < getChassisSize(); i++) {
      // TODO: remove after 1.12.2 update, backwards compatibility
      final ItemIdentifierStack idStack = _moduleInventory.getIDStackInSlot(i);
      if (idStack != null && !_module.hasModule(i)) {
        final Item stackItem = idStack.getItem().item;
        if (stackItem instanceof ItemModule) {
          final ItemModule moduleItem = (ItemModule) stackItem;
          LogisticsModule module = moduleItem.getModule(null, this, this);
          if (module != null) {
            _module.installModule(i, module);
          }
        }
      }

      if (i >= slotUpgradeManagers.size()) {
        addModuleUpgradeManager();
      }
      slotUpgradeManagers.get(i).readFromNBT(nbttagcompound, Integer.toString(i));
    }
    // register slotted modules
    _module.slottedModules()
           .filter(slottedModule -> !slottedModule.isEmpty())
           .forEach(slottedModule -> {
            LogisticsModule logisticsModule = Objects.requireNonNull(slottedModule.getModule());
            // FIXME: rely on getModuleForItem instead
           logisticsModule.registerHandler(this, this);
           slottedModule.registerPosition();
           });
  }

  private void addModuleUpgradeManager() {
    slotUpgradeManagers.add(new ModuleUpgradeManager(this, upgradeManager));
  }

  @Override
  public void writeToNBT(@Nonnull NBTTagCompound nbttagcompound) {
    super.writeToNBT(nbttagcompound);
    updateModuleInventory();
    _moduleInventory.writeToNBT(nbttagcompound, "crafting_manager");
    _module.writeToNBT(nbttagcompound);
    nbttagcompound.setInteger("Orientation", pointedAdjacent == null ? -1 : pointedAdjacent.getDir().ordinal());
//    for (int i = 0; i < getChassisSize(); i++) {
//      slotUpgradeManagers.get(i).writeToNBT(nbttagcompound, Integer.toString(i));
//    }
  }

  @Override
  public void onAllowedRemoval() {
    _moduleInventory.removeListener(this);
    if (MainProxy.isServer(getWorld())) {
      for (int i = 0; i < getChassisSize(); i++) {
        LogisticsModule x = getSubModule(i);
        if (x instanceof ILegacyActiveModule) {
          ILegacyActiveModule y = (ILegacyActiveModule) x;
          y.onBlockRemoval();
        }
      }
      updateModuleInventory();
      _moduleInventory.dropContents(getWorld(), getX(), getY(), getZ());

//      for (int i = 0; i < getChassisSize(); i++) {
//        getModuleUpgradeManager(i).dropUpgrades();
//      }
    }
  }

  @Override
  public void enabledUpdateEntity() {
    super.enabledUpdateEntity();
    if (doContentUpdate) {
      checkContentUpdate();
    }
  }

  @Override
  public void itemArrived(ItemIdentifierStack item, IAdditionalTargetInformation info) {
    if (MainProxy.isServer(getWorld())) {
      if (info instanceof PipeCraftingManager.CMTargetInformation) {
        PipeCraftingManager.CMTargetInformation target = (PipeCraftingManager.CMTargetInformation) info;
        LogisticsModule module = getSubModule(target.moduleSlot);
        if (module instanceof IRequireReliableTransport) {
          ((IRequireReliableTransport) module).itemArrived(item, info);
        }
      } else {
        if (LogisticsPipes.isDEBUG() && info != null) {
          System.out.println(item);
          new RuntimeException("[ItemArrived] Information weren't ment for a chassi pipe").printStackTrace();
        }
      }
    }
  }

  @Override
  public void itemLost(ItemIdentifierStack item, IAdditionalTargetInformation info) {
    if (MainProxy.isServer(getWorld())) {
      if (info instanceof PipeCraftingManager.CMTargetInformation) {
        PipeCraftingManager.CMTargetInformation target = (PipeCraftingManager.CMTargetInformation) info;
        LogisticsModule module = getSubModule(target.moduleSlot);
        if (module instanceof IRequireReliableTransport) {
          ((IRequireReliableTransport) module).itemLost(item, info);
        }
      } else {
        if (LogisticsPipes.isDEBUG()) {
          System.out.println(item);
          new RuntimeException("[ItemLost] Information weren't ment for a chassi pipe").printStackTrace();
        }
      }
    }
  }

  @Override
  public IRoutedItem sendStack(@Nonnull ItemStack stack, int destRouterId, @Nonnull SinkReply sinkReply, @Nonnull ItemSendMode itemSendMode, EnumFacing direction) {
    return super.sendStack(stack, destRouterId, sinkReply, itemSendMode, direction);
  }

  @Override
  public void InventoryChanged(IInventory inventory) {
    boolean reInitGui = false;
    for (int i = 0; i < inventory.getSizeInventory(); i++) {
      ItemStack stack = inventory.getStackInSlot(i);
      if (stack.isEmpty()) {
        if (_module.hasModule(i)) {
          _module.removeModule(i);
          reInitGui = true;
        }
        continue;
      }

      final Item stackItem = stack.getItem();
      if (stackItem instanceof ItemModule && isCraftingModule(stack)) {
        final ItemModule moduleItem = (ItemModule) stackItem;
        LogisticsModule current = _module.getModule(i);
        LogisticsModule next = moduleItem.getModuleForItem(stack, current, this, this);
        Objects.requireNonNull(next, "getModuleForItem returned null for " + stack);
        next.registerPosition(LogisticsModule.ModulePositionType.SLOT, i);
        if (current != next) {
          _module.installModule(i, next);
          if (!MainProxy.isClient(getWorld())) {
            ItemModuleInformationManager.readInformation(stack, next);
          }
          next.finishInit();
        }
        inventory.setInventorySlotContents(i, stack);
      }
    }
    if (reInitGui) {
      if (MainProxy.isClient(getWorld())) {
        if (FMLClientHandler.instance().getClient().currentScreen instanceof GuiCMPipe) {
          FMLClientHandler.instance().getClient().currentScreen.initGui();
        }
      }
    }
    if (MainProxy.isServer(getWorld())) {
      if (!localModeWatchers.isEmpty()) {
        MainProxy.sendToPlayerList(PacketHandler.getPacket(CMPipeModuleContent.class)
                .setIdentList(ItemIdentifierStack.getListFromInventory(_moduleInventory))
                .setPosX(getX()).setPosY(getY()).setPosZ(getZ()),
            localModeWatchers);
      }
    }
  }

  @Override
  public void ignoreDisableUpdateEntity() {
    if (!init) {
      init = true;
      if (MainProxy.isClient(getWorld())) {
        MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestCMOrientationPacket.class).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
      }
    }
  }

  @Override
  public final @Nullable LogisticsModule getLogisticsModule() {
    return _module;
  }

  @Nonnull
  @Override
  public TransportLayer getTransportLayer() {
    if (_transportLayer == null) {
      _transportLayer = new CMTransportLayer(this);
    }
    return _transportLayer;
  }

  private boolean tryInsertingModule(EntityPlayer entityplayer) {
    if (!isCraftingModule(entityplayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND))) return false;
    updateModuleInventory();
    for (int i = 0; i < _moduleInventory.getSizeInventory(); i++) {
      if (_moduleInventory.getIDStackInSlot(i) == null) {
        _moduleInventory.setInventorySlotContents(i, entityplayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).splitStack(1));
        InventoryChanged(_moduleInventory);
        return true;
      }
    }
    return false;
  }

  public static boolean isCraftingModule(ItemStack itemStack){
    return itemStack.getItem() == Item.REGISTRY.getObject(LPItems.modules.get(ModuleCrafter.getName()));
  }

  @Override
  public boolean handleClick(EntityPlayer entityplayer, SecuritySettings settings) {
    if (entityplayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).isEmpty()) {
      return false;
    }

    if (entityplayer.isSneaking() && SimpleServiceLocator.configToolHandler.canWrench(entityplayer, entityplayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), container)) {
      if (MainProxy.isServer(getWorld())) {
        if (settings == null || settings.openGui) {
          ((PipeCraftingManager) container.pipe).nextOrientation();
        } else {
          entityplayer.sendMessage(new TextComponentTranslation("lp.chat.permissiondenied"));
        }
      }
      SimpleServiceLocator.configToolHandler.wrenchUsed(entityplayer, entityplayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), container);
      return true;
    }

    if (!entityplayer.isSneaking() && entityplayer.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).getItem() instanceof ItemModule) {
      if (MainProxy.isServer(getWorld())) {
        if (settings == null || settings.openGui) {
          return tryInsertingModule(entityplayer);
        } else {
          entityplayer.sendMessage(new TextComponentTranslation("lp.chat.permissiondenied"));
        }
      }
      return true;
    }

    return false;
  }


  /*** IProvideItems ***/
  @Override
  public void canProvide(RequestTreeNode tree, RequestTree root, List<IFilter> filters) {
    if (!isEnabled()) {
      return;
    }
    for (IFilter filter : filters) {
      if (filter.isBlocked() == filter.isFilteredItem(tree.getRequestType()) || filter.blockProvider()) {
        return;
      }
    }
    for (int i = 0; i < getChassisSize(); i++) {
      LogisticsModule x = getSubModule(i);
      if (x instanceof ILegacyActiveModule) {
        ILegacyActiveModule y = (ILegacyActiveModule) x;
        y.canProvide(tree, root, filters);
      }
    }
  }

  @Override
  public LogisticsOrder fullFill(LogisticsPromise promise, IRequestItems destination, IAdditionalTargetInformation info) {
    if (!isEnabled()) {
      return null;
    }
    for (int i = 0; i < getChassisSize(); i++) {
      LogisticsModule x = getSubModule(i);
      if (x instanceof ILegacyActiveModule) {
        ILegacyActiveModule y = (ILegacyActiveModule) x;
        LogisticsOrder result = y.fullFill(promise, destination, info);
        if (result != null) {
          spawnParticle(Particles.WhiteParticle, 2);
          return result;
        }
      }
    }
    return null;
  }

  @Override
  public void getAllItems(Map<ItemIdentifier, Integer> list, List<IFilter> filter) {
    if (!isEnabled()) {
      return;
    }
    for (int i = 0; i < getChassisSize(); i++) {
      LogisticsModule x = getSubModule(i);
      if (x instanceof ILegacyActiveModule) {
        ILegacyActiveModule y = (ILegacyActiveModule) x;
        y.getAllItems(list, filter);
      }
    }
  }

  @Override
  public ItemSendMode getItemSendMode() {
    return ItemSendMode.Normal;
  }

  @Override
  public IHeadUpDisplayRenderer getRenderer() {
    return hud;
  }

  @Override
  public void startWatching() {
    MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStartWatchingPacket.class).setInteger(1).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
  }

  @Override
  public void stopWatching() {
    MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStopWatchingPacket.class).setInteger(1).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
    hud.stopWatching();
  }

  @Override
  public void playerStartWatching(EntityPlayer player, int mode) {
    if (mode == 1) {
      updateModuleInventory();
      localModeWatchers.add(player);
      MainProxy.sendPacketToPlayer(PacketHandler.getPacket(CMPipeModuleContent.class).setIdentList(ItemIdentifierStack.getListFromInventory(_moduleInventory)).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), player);
      MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OrdererManagerContent.class).setIdentList(oldList).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), player);
      MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SendQueueContent.class).setIdentList(ItemIdentifierStack.getListSendQueue(_sendQueue)).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), player);
      _module.startWatching(player);
    } else {
      super.playerStartWatching(player, mode);
    }
  }

  @Override
  public void playerStopWatching(EntityPlayer player, int mode) {
    super.playerStopWatching(player, mode);
    localModeWatchers.remove(player);
    _module.stopWatching(player);
  }

  @Override
  public void listenedChanged() {
    doContentUpdate = true;
  }

  private void checkContentUpdate() {
    doContentUpdate = false;
    LinkedList<ItemIdentifierStack> all = _orderItemManager.getContentList(getWorld());
    if (!oldList.equals(all)) {
      oldList.clear();
      oldList.addAll(all);
      MainProxy.sendToPlayerList(PacketHandler.getPacket(OrdererManagerContent.class).setIdentList(all).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), localModeWatchers);
    }
  }

  @Override
  public void finishInit() {
    super.finishInit();
    _module.finishInit();
  }

  public void handleModuleItemIdentifierList(Collection<ItemIdentifierStack> _allItems) {
    _moduleInventory.handleItemIdentifierList(_allItems);
  }

  @Override
  public int sendQueueChanged(boolean force) {
    if (MainProxy.isServer(getWorld())) {
      if (Configs.MULTI_THREAD_NUMBER > 0 && !force) {
        HudUpdateTick.add(getRouter());
      } else {
        if (localModeWatchers.size() > 0) {
          LinkedList<ItemIdentifierStack> items = ItemIdentifierStack.getListSendQueue(_sendQueue);
          MainProxy.sendToPlayerList(PacketHandler.getPacket(SendQueueContent.class).setIdentList(items).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), localModeWatchers);
          return items.size();
        }
      }
    }
    return 0;
  }

  @Override
  public void handleSendQueueItemIdentifierList(Collection<ItemIdentifierStack> _allItems) {
    displayList.clear();
    displayList.addAll(_allItems);
  }

  public TB_ModuleCM getModules() {
    return _module;
  }

  @Override
  public void setTile(TileEntity tile) {
    super.setTile(tile);
    _module.slottedModules().forEach(SlottedModule::registerPosition);
  }

  @Override
  public int getSourceID() {
    return getRouterId();
  }

  @Override
  public void collectSpecificInterests(@Nonnull Collection<ItemIdentifier> itemidCollection) {
    _module.collectSpecificInterests(itemidCollection);
    // if we don't have a pointed inventory we can't be interested in anything
    if (getPointedAdjacentOrNoAdjacent().inventories().isEmpty()) {
      return;
    }

    for (int moduleIndex = 0; moduleIndex < getChassisSize(); moduleIndex++) {
      LogisticsModule module = getSubModule(moduleIndex);
      if (module != null && module.interestedInAttachedInventory()) {
        final ISlotUpgradeManager upgradeManager = getUpgradeManager(module.getSlot(), module.getPositionInt());
        IInventoryUtil inv = PipeServiceProviderUtilKt.availableSneakyInventories(this, upgradeManager).stream().findFirst().orElse(null);
        if (inv == null) {
          continue;
        }
        Set<ItemIdentifier> items = inv.getItems();
        itemidCollection.addAll(items);

        //also add tag-less variants ... we should probably add a module.interestedIgnoringNBT at some point
        items.stream().map(ItemIdentifier::getIgnoringNBT).forEach(itemidCollection::add);

        boolean modulesInterestedInUndamged = false;
        for (int i = 0; i < getChassisSize(); i++) {
          if (getSubModule(moduleIndex).interestedInUndamagedID()) {
            modulesInterestedInUndamged = true;
            break;
          }
        }
        if (modulesInterestedInUndamged) {
          items.stream().map(ItemIdentifier::getUndamaged).forEach(itemidCollection::add);
        }
        break; // no need to check other modules for interest in the inventory, when we know that 1 already is.
      }
    }
    for (int i = 0; i < getChassisSize(); i++) {
      LogisticsModule module = getSubModule(i);
      if (module != null) {
        module.collectSpecificInterests(itemidCollection);
      }
    }
  }

  @Override
  public boolean hasGenericInterests() {
    if (getPointedAdjacentOrNoAdjacent().inventories().isEmpty()) {
      return false;
    }
    for (int i = 0; i < getChassisSize(); i++) {
      LogisticsModule x = getSubModule(i);

      if (x != null && x.hasGenericInterests()) {
        return true;
      }
    }
    return false;
  }

  /** ICraftItems */
  public final LinkedList<LogisticsOrder> _extras = new LinkedList<>();

  @Override
  public void registerExtras(IPromise promise) {
    if (!(promise instanceof LogisticsPromise)) {
      throw new UnsupportedOperationException("Extra has to be an item for a chassis pipe");
    }
    ItemIdentifierStack stack = new ItemIdentifierStack(((LogisticsPromise) promise).item, ((LogisticsPromise) promise).numberOfItems);
    _extras.add(new LogisticsItemOrder(new DictResource(stack, null), null, IOrderInfoProvider.ResourceType.EXTRA, null));
  }

  @Override
  public ICraftingTemplate addCrafting(IResource toCraft) {
    for (int i = 0; i < getChassisSize(); i++) {
      LogisticsModule x = getSubModule(i);

      if (x instanceof ICraftItems) {
        if (((ICraftItems) x).canCraft(toCraft)) {
          return ((ICraftItems) x).addCrafting(toCraft);
        }
      }
    }
    return null;

    // trixy code goes here to ensure the right crafter answers the right request
  }

  @Override
  public List<ItemIdentifierStack> getCraftedItems() {
    List<ItemIdentifierStack> craftables = null;
    for (int i = 0; i < getChassisSize(); i++) {
      LogisticsModule x = getSubModule(i);

      if (x instanceof ICraftItems) {
        if (craftables == null) {
          craftables = new LinkedList<>();
        }
        craftables.addAll(((ICraftItems) x).getCraftedItems());
      }
    }
    return craftables;
  }

  @Override
  public boolean canCraft(IResource toCraft) {
    for (int i = 0; i < getChassisSize(); i++) {
      LogisticsModule x = getSubModule(i);

      if (x instanceof ICraftItems) {
        if (((ICraftItems) x).canCraft(toCraft)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public int getTodo() {
    // TODO Auto-generated method stub
    // probably not needed, the chasi order manager handles the count, would need to store origin to specifically know this.
    return 0;
  }

  @Nullable
  public LogisticsModule getSubModule(int slot) {
    return _module.getModule(slot);
  }

  public static class CMTargetInformation implements IAdditionalTargetInformation {

    @Getter
    private final int moduleSlot;

    public CMTargetInformation(int slot) {
      moduleSlot = slot;
    }
  }

  @Override
  public void setCCType(Object type) {
    super.setCCType(type);
  }

  @Override
  public Object getCCType() {
    return super.getCCType();
  }
}
