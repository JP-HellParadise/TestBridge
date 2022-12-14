package testbridge.helpers;

import java.util.*;

import com.google.common.collect.ImmutableSet;

import lombok.Getter;

import net.minecraft.block.Block;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.implementations.IUpgradeableHost;
import appeng.api.implementations.tiles.ISegmentedInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.parts.IPart;
import appeng.api.storage.*;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.*;
import appeng.core.settings.TickRates;
import appeng.helpers.MultiCraftingTracker;
import appeng.helpers.NonBlockingItems;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.MachineSource;
import appeng.me.storage.MEMonitorPassThrough;
import appeng.me.storage.NullInventory;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.ConfigManager;
import appeng.util.IConfigManagerHost;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.BlockingInventoryAdaptor;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.IInventoryDestination;
import appeng.util.inv.InvOperation;

import de.ellpeck.actuallyadditions.api.tile.IPhantomTile;

import network.rs485.logisticspipes.property.EnumProperty;

import testbridge.core.TB_ItemHandlers;
import testbridge.helpers.interfaces.ICraftingManagerHost;
import testbridge.items.VirtualPatternAE;
import testbridge.modules.TB_ModuleCM.BlockingMode;
import testbridge.part.PartSatelliteBus;

public class DualityCraftingManager
    implements IGridTickable, IStorageMonitorable, IInventoryDestination, IAEAppEngInventory, IConfigManagerHost, ICraftingProvider, IConfigurableObject, ISegmentedInventory {
  private static final IItemStorageChannel ITEMS = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
  public static final int NUMBER_OF_PATTERN_SLOTS = 27;
  private static final Collection<Block> BAD_BLOCKS = new HashSet<>(100);
  private final MultiCraftingTracker craftingTracker;
  @Getter
  private final AENetworkProxy gridProxy;
  private final ICraftingManagerHost iHost;
  private final ConfigManager cm = new ConfigManager(this);
  private final AppEngInternalInventory patterns = new AppEngInternalInventory(this, NUMBER_OF_PATTERN_SLOTS);
  private final MEMonitorPassThrough<IAEItemStack> items = new MEMonitorPassThrough<>(new NullInventory<IAEItemStack>(), ITEMS);
  public final EnumProperty<BlockingMode> blockingMode = new EnumProperty<>(BlockingMode.OFF, "blockingMode", BlockingMode.VALUES);
  final MachineSource actionSource;
  private int priority;
  private List<ICraftingPatternDetails> craftingList = null;
  private List<ItemStack> waitingToSend = null;
  private List<ItemStack> createPkgList;
  private HashMap<String, List<ItemStack>> waitingToSendOnSat = new HashMap<>();
  private ArrayList<String> satelliteList = new ArrayList<>();
  private String mainSatName = "";

  public DualityCraftingManager(final AENetworkProxy networkProxy, final ICraftingManagerHost cmHost) {
    this.gridProxy = networkProxy;
    this.gridProxy.setFlags(GridFlags.REQUIRE_CHANNEL);

    this.cm.registerSetting(Settings.INTERFACE_TERMINAL, YesNo.YES);

    this.iHost = cmHost;
    this.craftingTracker = new MultiCraftingTracker(this.iHost, 9);

    actionSource = new MachineSource(this.iHost);
    this.items.setChangeSource(actionSource);
  }

  private static boolean invIsCustomBlocking(BlockingInventoryAdaptor inv) {
    return (inv.containsBlockingItems());
  }

  @Override
  public void saveChanges() {
    this.iHost.saveChanges();
  }

  @Override
  public void onChangeInventory(final IItemHandler inv, final int slot, final InvOperation mc, final ItemStack removed, final ItemStack added) {
    if (inv == this.patterns && (!removed.isEmpty() || !added.isEmpty())) {
      this.updateCraftingList();
    }
  }

  public void writeToNBT(final NBTTagCompound data) {
    this.patterns.writeToNBT(data, "patterns");
    this.cm.writeToNBT(data);
    this.craftingTracker.writeToNBT(data);
    data.setString("mainSatName", this.mainSatName);
    data.setInteger("blockingMode", this.blockingMode.getValue().ordinal());
    data.setInteger("priority", this.priority);

    // This is for main Sat
    final NBTTagList waitingToSend = new NBTTagList();
    if (this.waitingToSend != null) {
      for (final ItemStack is : this.waitingToSend) {
        final NBTTagCompound item = new NBTTagCompound();
        is.writeToNBT(item);
        if (is.getCount() > Byte.MAX_VALUE) {
          item.setInteger("stackSize", is.getCount());
        }
        waitingToSend.appendTag(item);
      }
    }
    data.setTag("waitingToSend", waitingToSend);

    // For additional sat(s) used in Pattern contain package
    NBTTagList satList = new NBTTagList();
    for (int i = 0 ; i < this.satelliteList.size() ; i++) {
      NBTTagCompound satTag = new NBTTagCompound();
      satTag.setString("satId_" + (i + 1), this.satelliteList.get(i));
      satList.appendTag(satTag);
    }

    data.setTag("satList", satList);

    // List of items base on sat name
    NBTTagCompound satItemList = new NBTTagCompound();
    if (this.waitingToSendOnSat != null) {
      for (String satName : this.satelliteList) {
        // Store list of item to each satellite that is in used
        NBTTagList waitingListSided = new NBTTagList();
        if (this.waitingToSendOnSat.containsKey(satName)) {
          for (final ItemStack is : this.waitingToSendOnSat.get(satName)) {
            final NBTTagCompound item = new NBTTagCompound();
            is.writeToNBT(item);
            if (is.getCount() > Byte.MAX_VALUE) {
              item.setInteger("stackSize", is.getCount());
            }
            waitingListSided.appendTag(item);
          }
          satItemList.setTag(satName, waitingListSided);
        }
      }
    }

    data.setTag("satItemList", satItemList);

    NBTTagCompound pkgList = new NBTTagCompound();
    if (this.createPkgList != null) {
      for (final ItemStack is : this.createPkgList) {
        final NBTTagCompound item = new NBTTagCompound();
        is.writeToNBT(item);
        if (is.getCount() > Byte.MAX_VALUE) {
          item.setInteger("stackSize", is.getCount());
        }
        waitingToSend.appendTag(item);
      }
    }

    data.setTag("__pkgList", pkgList);
  }

  public void readFromNBT(final NBTTagCompound data) {
    // Just to be sure
    this.mainSatName = data.getString("mainSatName");
    this.blockingMode.setValue(BlockingMode.values()[data.getInteger("blockingMode")]);

    // Wait list on main sat
    this.waitingToSend = null;
    final NBTTagList waitingList = data.getTagList("waitingToSend", 10);
    if (waitingList != null) {
      for (int x = 0; x < waitingList.tagCount(); x++) {
        final NBTTagCompound c = waitingList.getCompoundTagAt(x);
        if (c != null) {
          final ItemStack is = new ItemStack(c);
          if (c.hasKey("stackSize")) {
            is.setCount(c.getInteger("stackSize"));
          }
          this.addToSendList(is);
        }
      }
    }

    // Retrieve sat(s) list
    this.satelliteList = new ArrayList<>();
    NBTTagList satList = data.getTagList("satList", 10);

    for(int i = 0; i < satList.tagCount(); i++) {
      NBTTagCompound tag = satList.getCompoundTagAt(i);
      String satName = tag.getString("satId_" + i);
      addToSatList(satName);
    }

    // Retrieve item list base on sat list
    this.waitingToSendOnSat = null;
    final NBTTagCompound satItemList = data.getCompoundTag("satItemList");

    for (String satName : this.satelliteList) {
      if (satItemList.hasKey(satName)) {
        NBTTagList w = satItemList.getTagList(satName, 10);
        for (int x = 0; x < w.tagCount(); x++) {
          final NBTTagCompound c = w.getCompoundTagAt(x);
          if (c != null) {
            final ItemStack is = new ItemStack(c);
            if (c.hasKey("stackSize")) {
              is.setCount(c.getInteger("stackSize"));
            }
            this.addToSendListOnSat(is, satName);
          }
        }
      }

      // Wait list on main sat
      this.createPkgList = null;
      final NBTTagList pkgList = data.getTagList("__pkgList", 10);
      if (pkgList != null) {
        for (int x = 0; x < pkgList.tagCount(); x++) {
          final NBTTagCompound c = pkgList.getCompoundTagAt(x);
          if (c != null) {
            final ItemStack is = new ItemStack(c);
            if (c.hasKey("stackSize")) {
              is.setCount(c.getInteger("stackSize"));
            }
            addToCreatePkgList(is);
          }
        }
      }
    }

    this.craftingTracker.readFromNBT(data);
    this.patterns.readFromNBT(data, "patterns");
    this.priority = data.getInteger("priority");
    this.cm.readFromNBT(data);
    this.updateCraftingList();
  }

  private void addToSendList(final ItemStack is) {
    if (is.isEmpty()) {
      return;
    }

    if (this.waitingToSend == null) {
      this.waitingToSend = new ArrayList<>();
    }

    this.waitingToSend.add(is);

    try {
      this.gridProxy.getTick().wakeDevice(this.gridProxy.getNode());
    } catch (final GridAccessException e) {
      // :P
    }
  }

  private void addToSatList(final String satName) {
    if (satName.isEmpty()) {
      return;
    }

    if (this.satelliteList == null) {
      this.satelliteList = new ArrayList<>();
    }

    this.satelliteList.add(satName);

    try {
      this.gridProxy.getTick().wakeDevice(this.gridProxy.getNode());
    } catch (final GridAccessException e) {
      // :P
    }
  }

  private void addToSendListOnSat(final ItemStack is, String satName) {
    if (is.isEmpty()) {
      return;
    }
    if (this.waitingToSendOnSat == null) {
      this.waitingToSendOnSat = new HashMap<>();
    }

    this.waitingToSendOnSat.computeIfAbsent(satName, k -> new ArrayList<>());

    this.waitingToSendOnSat.get(satName).add(is);

    this.satelliteList.add(satName);

    try {
      this.gridProxy.getTick().wakeDevice(this.gridProxy.getNode());
    } catch (final GridAccessException e) {
      // :P
    }
  }

  private void addToCreatePkgList(final ItemStack is) {
    if (is.isEmpty()) {
      return;
    }
    if (this.createPkgList == null) {
      this.createPkgList = new ArrayList<>();
    }

    this.createPkgList.add(is);

    try {
      this.gridProxy.getTick().wakeDevice(this.gridProxy.getNode());
    } catch (final GridAccessException e) {
      // :P
    }
  }

  private void updateCraftingList() {
    final Boolean[] accountedFor = new Boolean[this.patterns.getSlots()];
    Arrays.fill(accountedFor, false);

    if (!this.gridProxy.isReady()) {
      return;
    }

    boolean removed = false;

    if (this.craftingList != null) {
      List<ItemStack> packageList = new ArrayList<>();
      for (ICraftingPatternDetails details : craftingList) {
        visitArray(packageList, details.getOutputs(), true);
      }
      final Iterator<ICraftingPatternDetails> i = this.craftingList.iterator();
      while (i.hasNext()) {
        final ICraftingPatternDetails details = i.next();
        boolean found = false;

        if (!packageList.isEmpty()) {
          IAEItemStack[] out = details.getCondensedOutputs();
          for (IAEItemStack iaeItemStack : out) {
            ItemStack is = iaeItemStack == null ? ItemStack.EMPTY : iaeItemStack.asItemStackRepresentation();
            if (!is.isEmpty() && is.getItem() == TB_ItemHandlers.itemPackage && packageList.stream().anyMatch(s -> ItemStack.areItemStackTagsEqual(s, is))) {
              found = true;
              break;
            }
          }
        }

        if (!found) {
          for (int x = 0; x < accountedFor.length; x++) {
            final ItemStack pattern = this.patterns.getStackInSlot(x);
            if (details instanceof VirtualPatternHelper) {
              if (details.getPattern() == pattern) {
                accountedFor[x] = found = true; // Our pattern truly exist!!!
              }
            }
          }
        }

        if (!found) {
          removed = true;
          i.remove();
        }
      }
    }

    boolean newPattern = false;

    for (int x = 0; x < accountedFor.length; x++) {
      if (!accountedFor[x]) {
        newPattern = true;
        this.addToCraftingList(this.patterns.getStackInSlot(x));
      }
    }
    try {
      this.gridProxy.getGrid().postEvent(new MENetworkCraftingPatternChange(this, this.gridProxy.getNode()));
    } catch (GridAccessException e) {
      e.printStackTrace();
    }
  }

  private boolean hasWorkToDo() {
    return hasItemsToSend() || hasItemsToSendOnSat() || hasPkgToCreate();
  }

  public void notifyNeighbors() {
    if (this.gridProxy.isActive()) {
      try {
        this.gridProxy.getGrid().postEvent(new MENetworkCraftingPatternChange(this, this.gridProxy.getNode()));
        this.gridProxy.getTick().wakeDevice(this.gridProxy.getNode());
      } catch (final GridAccessException e) {
        // :P
      }
    }

    final TileEntity te = this.iHost.getTileEntity();
    if (te != null && te.getWorld() != null) {
      Platform.notifyBlocksOfNeighbors(te.getWorld(), te.getPos());
    }
  }

  private void addToCraftingList(final ItemStack is) {
    if (is.isEmpty()) {
      return;
    }

    if (is.getItem() instanceof ICraftingPatternItem) {
      final ICraftingPatternItem cpi = (ICraftingPatternItem) is.getItem();
      final ICraftingPatternDetails details = cpi.getPatternForItem(is, this.iHost.getTileEntity().getWorld());

      if (details != null) {
        if (this.craftingList == null) {
          this.craftingList = new ArrayList<>();
        }

        IAEItemStack[] in = details.getInputs();

        List<ItemStack> packageList = new ArrayList<>();
        visitArray(packageList, in, false);
        packageList.stream().map(pkg -> VirtualPatternAE.newPattern(new ItemStack(pkg.getTagCompound().getCompoundTag("__itemHold")), pkg)).forEach(craftingList::add);

        this.craftingList.add(VirtualPatternAE.newPattern(is));
      }
    }
  }

  private boolean hasPkgToCreate() {
    return this.createPkgList != null && !this.createPkgList.isEmpty();
  }

  private boolean hasItemsToSend() {
    return this.waitingToSend != null && !this.waitingToSend.isEmpty();
  }

  private boolean hasItemsToSendOnSat() {
    if (waitingToSendOnSat != null) {
      for (String satName : waitingToSendOnSat.keySet()) {
        if (!waitingToSendOnSat.get(satName).isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }

//  public void dropExcessPatterns() {
//    IItemHandler patterns = getPatterns();
//
//    List<ItemStack> dropList = new ArrayList<>();
//    for (int invSlot = 0; invSlot < patterns.getSlots(); invSlot++) {
//      if (invSlot > NUMBER_OF_PATTERN_SLOTS - 1) {
//        ItemStack is = patterns.getStackInSlot(invSlot);
//        if (is.isEmpty()) {
//          continue;
//        }
//        dropList.add(patterns.extractItem(invSlot, Integer.MAX_VALUE, false));
//      }
//    }
//    if (dropList.size() > 0) {
//      World world = this.getLocation().getWorld();
//      BlockPos blockPos = this.getLocation().getPos();
//      Platform.spawnDrops(world, blockPos, dropList);
//    }
//
//    this.gridProxy.setIdlePowerUsage(Math.pow(4, 4));
//  }

  @Override
  public boolean canInsert(final ItemStack stack) {
    return false;
  }

  public IItemHandler getPatterns() {
    return this.patterns;
  }

  public void gridChanged() {
    try {
      this.items.setInternal(this.gridProxy.getStorage().getInventory(ITEMS));
    } catch (final GridAccessException gae) {
      this.items.setInternal(new NullInventory<IAEItemStack>());
    }

    this.notifyNeighbors();
  }

  public AECableType getCableConnectionType(final AEPartLocation dir) {
    return AECableType.SMART;
  }

  public DimensionalCoord getLocation() {
    return new DimensionalCoord(this.iHost.getTileEntity());
  }

  public IItemHandler getInternalInventory() {
    return new ItemStackHandler(0);
  }

  @Override
  public TickingRequest getTickingRequest(final IGridNode node) {
    return new TickingRequest(2, TickRates.Interface.getMax(), !this.hasWorkToDo(), true);
  }

  @Override
  public TickRateModulation tickingRequest(final IGridNode node, final int ticksSinceLastCall) {
    if (!this.gridProxy.isActive()) {
      return TickRateModulation.SLEEP;
    }

    //Previous version might have items saved in this list
    //recover them
    if (this.hasItemsToSend()) {
      this.pushItemsOut(mainSatName);
    }

    if (this.hasItemsToSendOnSat()) {
        this.pushCustomItemsOut(waitingToSendOnSat.keySet());
    }

    if (this.hasPkgToCreate()) {
      this.createPkg();
      return TickRateModulation.URGENT;
    }


    return this.hasWorkToDo() ? TickRateModulation.SLOWER : TickRateModulation.SLEEP;
  }

  private void pushItemsOut(final String satName) {
    if (!this.hasItemsToSend()) {
      return;
    }

    final PartSatelliteBus part = findSatellite(satName);
    if (part == null) return;

    final World w = part.getTile().getWorld();
    final TileEntity te = w.getTileEntity(part.getTile().getPos().offset(part.getTargets()));
    final InventoryAdaptor ad = InventoryAdaptor.getAdaptor(te, part.getTargets().getOpposite());
    if (ad == null) return;

    final Iterator<ItemStack> i = this.waitingToSend.iterator();

    while (i.hasNext()) {
      ItemStack whatToSend = i.next();

      final ItemStack result = ad.addItems(whatToSend);

      if (result.isEmpty()) {
        whatToSend = ItemStack.EMPTY;
      } else {
        whatToSend.setCount(result.getCount());
        whatToSend.setTagCompound(result.getTagCompound());
      }

      if (whatToSend.isEmpty()) {
        i.remove();
      }
    }

    if (this.waitingToSend.isEmpty()) {
      this.waitingToSend = null;
    }
  }

  private void pushCustomItemsOut(final Set<String> satList) {
    if (!this.hasItemsToSendOnSat()) {
      return;
    }

    for (String satName : satList){
      final PartSatelliteBus part = findSatellite(satName);
      if (part == null) return;

      final World w = part.getTile().getWorld();
      final TileEntity te = w.getTileEntity(part.getTile().getPos().offset(part.getTargets()));
      final InventoryAdaptor ad = InventoryAdaptor.getAdaptor(te, part.getTargets().getOpposite());
      if (ad == null) return;

      Iterator<ItemStack> i = waitingToSendOnSat.get(satName).iterator();

      while (i.hasNext()) {
        ItemStack whatToSend = i.next();

        final ItemStack result = ad.addItems(whatToSend);

        if (result.isEmpty()) {
          whatToSend = ItemStack.EMPTY;
        } else {
          whatToSend.setCount(result.getCount());
          whatToSend.setTagCompound(result.getTagCompound());
        }

        if (whatToSend.isEmpty()) {
          i.remove();
          satelliteList.remove(satName);
        }
      }
    }

    if (this.waitingToSendOnSat.isEmpty()) {
      this.waitingToSendOnSat = null;
    }
  }

  public TileEntity getTile() {
    return (TileEntity) (this.iHost instanceof TileEntity ? this.iHost : null);
  }

  @Override
  public <T extends IAEStack<T>> IMEMonitor<T> getInventory(IStorageChannel<T> channel) {
    if (channel == ITEMS) {
      return (IMEMonitor<T>) this.items;
    }
    return null;
  }

  @Override
  public IItemHandler getInventoryByName(String name) {
    if (name.equals("patterns")) {
      return this.patterns;
    }

    return null;
  }

  @Override
  public IConfigManager getConfigManager() {
    return this.cm;
  }

  @Override
  public void updateSetting(final IConfigManager manager, final Enum settingName, final Enum newValue) {
    this.iHost.saveChanges();
  }

  private boolean invIsBlocked(InventoryAdaptor inv) {
    return (inv.containsItems());
  }

  @Override
  public boolean pushPattern(final ICraftingPatternDetails patternDetails, final InventoryCrafting table) {
    final PartSatelliteBus mainSat = this.findSatellite(mainSatName);

    if (this.hasItemsToSend() || this.hasItemsToSendOnSat() || !this.gridProxy.isActive() || !this.craftingList.contains(patternDetails) || mainSat == null) {
      return false;
    }

    final World w = mainSat.getTile().getWorld();

    if (patternDetails instanceof VirtualPatternHelper) {
      ItemStack first = patternDetails.getCondensedOutputs()[0].getDefinition();
      // If pattern has package, we will work here instead
      if (first.getItem() == TB_ItemHandlers.itemPackage && first.getTagCompound() != null && !first.getTagCompound().isEmpty()) {
        addToCreatePkgList(first);
        return true;
      }

      if (!blockingChecker(mainSatName, w)) {
        Set<String> satList = new HashSet<>();
        final TileEntity te = w.getTileEntity(mainSat.getTile().getPos().offset(mainSat.getTargets()));
        InventoryAdaptor ad = InventoryAdaptor.getAdaptor(te, mainSat.getTargets().getOpposite());
        if (this.acceptsItems(ad, table)) {
          for (int x = 0; x < table.getSizeInventory(); x++) {
            final ItemStack is = table.getStackInSlot(x);
            if (!is.isEmpty()) {
              if (is.getItem() == TB_ItemHandlers.itemPackage && is.getTagCompound() != null && !is.getTagCompound().isEmpty()) {
                String satName = is.getTagCompound().getString("__pkgDest");
                if (satName.isEmpty() || blockingChecker(satName, w)) return false;
                ItemStack result = new ItemStack(is.getTagCompound().getCompoundTag("__itemHold"));
                addToSendListOnSat(result, satName);
                satList.add(satName);
              } else addToSendList(is);
            }
          }
          pushItemsOut(mainSatName);
          pushCustomItemsOut(satList);
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public boolean isBusy() {
    boolean busy = false;

    PartSatelliteBus mainSat = this.findSatellite(mainSatName);

    if (this.hasItemsToSend() || this.hasItemsToSendOnSat() || mainSat == null) {
      return true;
    }

    if (this.isBlocking()) {
      final World w = mainSat.getTile().getWorld();

      boolean allAreBusy = true;

      if (!satelliteList.isEmpty()) {
        for (String satName : satelliteList){
          if (!blockingChecker(satName, w)){
            allAreBusy = false;
          }
        }
      }

      busy = allAreBusy || !blockingChecker(mainSatName, w);
    }

    return busy;
  }

  private boolean blockingChecker(String satName, World w) {
    final PartSatelliteBus sat = this.findSatellite(satName);
    if (sat == null) return false;
    final TileEntity te = w.getTileEntity(sat.getTile().getPos().offset(sat.getTargets()));
    InventoryAdaptor ad = InventoryAdaptor.getAdaptor(te, sat.getTargets().getOpposite());
    if (ad != null) {
      if (this.isBlocking()) {
        IPhantomTile phantomTE;
        if (Loader.isModLoaded("actuallyadditions") && te instanceof IPhantomTile) {
          phantomTE = ((IPhantomTile) te);
          if (phantomTE.hasBoundPosition()) {
            TileEntity phantom = w.getTileEntity(phantomTE.getBoundPosition());
            if (NonBlockingItems.INSTANCE.getMap().containsKey(w.getBlockState(phantomTE.getBoundPosition()).getBlock().getRegistryName().getNamespace())) {
              return isCustomInvBlocking(phantom, sat.getTargets());
            }
          }
        } else if (NonBlockingItems.INSTANCE.getMap().containsKey(w.getBlockState(sat.getTile().getPos().offset(sat.getTargets())).getBlock().getRegistryName().getNamespace())) {
          return isCustomInvBlocking(te, sat.getTargets());
        } else return invIsBlocked(ad);
      }
    }

    return false;
  }

  boolean isCustomInvBlocking(TileEntity te, EnumFacing s) {
    BlockingInventoryAdaptor blockingInventoryAdaptor = BlockingInventoryAdaptor.getAdaptor(te, s.getOpposite());
    return invIsCustomBlocking(blockingInventoryAdaptor);
  }

  private boolean isBlocking() {
    return false; // TODO
  }

  private boolean acceptsItems(final InventoryAdaptor ad, final InventoryCrafting table) {
    for (int x = 0; x < table.getSizeInventory(); x++) {
      final ItemStack is = table.getStackInSlot(x);
      if (is.isEmpty()) {
        continue;
      }

      if (!ad.simulateAdd(is).isEmpty()) {
        return false;
      }
    }

    return true;
  }

  @Override
  public void provideCrafting(final ICraftingProviderHelper craftingTracker) {
    if (this.gridProxy.isActive() && this.craftingList != null) {
      for (final ICraftingPatternDetails details : this.craftingList) {
        details.setPriority(this.priority);
        craftingTracker.addCraftingOption(this, details);
      }
    }
  }

  public void addDrops(final List<ItemStack> drops) {
    if (this.waitingToSend != null) {
      for (final ItemStack is : this.waitingToSend) {
        if (!is.isEmpty()) {
          drops.add(is);
        }
      }
    }

    if (this.waitingToSendOnSat != null) {
      for (List<ItemStack> itemList : waitingToSendOnSat.values()) {
        for (final ItemStack is : itemList) {
          if (!is.isEmpty()) {
            drops.add(is);
          }
        }
      }
    }

    for (final ItemStack is : this.patterns) {
      if (!is.isEmpty()) {
        drops.add(is);
      }
    }
  }

  public IUpgradeableHost getHost() {
    if (this.getPart() instanceof IUpgradeableHost) {
      return (IUpgradeableHost) this.getPart();
    }
    if (this.getTile() instanceof IUpgradeableHost) {
      return (IUpgradeableHost) this.getTile();
    }
    return null;
  }

  private IPart getPart() {
    return (IPart) (this.iHost instanceof IPart ? this.iHost : null);
  }

  public ImmutableSet<ICraftingLink> getRequestedJobs() {
    return this.craftingTracker.getRequestedJobs();
  }

  public IAEItemStack injectCraftedItems(final ICraftingLink link, final IAEItemStack acquired, final Actionable mode) {
    return acquired;
  }

  public void jobStateChange(final ICraftingLink link) {
    this.craftingTracker.jobStateChange(link);
  }

  public void initialize() {
    this.updateCraftingList();
  }

  public int getPriority() {
    return this.priority;
  }

  public void setPriority(final int newValue) {
    this.priority = newValue;
    this.iHost.saveChanges();

    try {
      this.gridProxy.getGrid().postEvent(new MENetworkCraftingPatternChange(this, this.gridProxy.getNode()));
    } catch (final GridAccessException e) {
      // :P
    }
  }

  public String getSatellite() {
    return this.mainSatName;
  }

  public void setSatellite(final String newValue) {
    this.mainSatName = newValue;
    this.iHost.saveChanges();
  }

  private void createPkg() {
    if (createPkgList == null) {
      return;
    }

    for (ItemStack is : createPkgList) {
      if (gridProxy.getNode() == null || is.isEmpty()) return;
      IMEInventoryHandler<IAEItemStack> i = ((IStorageGrid) gridProxy.getNode().getGrid().getCache(IStorageGrid.class)).getInventory(ITEMS);
      IAEItemStack st = ITEMS.createStack(is);
      IAEItemStack r = i.injectItems(st, Actionable.MODULATE, actionSource);
    }

    createPkgList = null;
  }

  private void visitArray(List<ItemStack> packageList, IAEItemStack[] inputArray, boolean isPlaceholder) {
    for (IAEItemStack input : inputArray) {
      if (input != null) {
        ItemStack is = input.getDefinition();
        if (is.getItem() == TB_ItemHandlers.itemPackage) {
          if(is.hasTagCompound() && is.getTagCompound().getBoolean("__actContainer") == isPlaceholder){
            if (!isPlaceholder) {
              is.getTagCompound().setBoolean("__actContainer", true);
              input = ITEMS.createStack(is);
            }
          }
          packageList.add(is);
        }
      }
    }
  }

  private PartSatelliteBus findSatellite(String name) {
    if (name.equals("")) {
      return null;
    }
    for (final IGridNode node : gridProxy.getNode().getGrid().getMachines(PartSatelliteBus.class)) {
      IGridHost h = node.getMachine();
      if(h instanceof PartSatelliteBus){
        PartSatelliteBus part = (PartSatelliteBus) h;
        if(part.getSatellitePipeName().equals(name)){
          return part;
        }
      }
    }
    return null;
  }
}
