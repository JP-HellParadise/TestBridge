package net.jp.hellparadise.testbridge.helpers;

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
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
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
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.ConfigManager;
import appeng.util.IConfigManagerHost;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.BlockingInventoryAdaptor;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.IInventoryDestination;
import appeng.util.inv.InvOperation;
import com.google.common.collect.ImmutableSet;
import de.ellpeck.actuallyadditions.api.tile.IPhantomTile;
import java.util.*;
import net.jp.hellparadise.testbridge.core.TB_ItemHandlers;
import net.jp.hellparadise.testbridge.helpers.interfaces.ICraftingManagerHost;
import net.jp.hellparadise.testbridge.items.VirtualPatternAE;
import net.jp.hellparadise.testbridge.part.PartSatelliteBus;
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

public class DualityCraftingManager implements IGridTickable, IStorageMonitorable, IInventoryDestination,
    IAEAppEngInventory, IConfigManagerHost, ICraftingProvider, IConfigurableObject, ISegmentedInventory {

    private static final IItemStorageChannel ITEMS = AEApi.instance()
        .storage()
        .getStorageChannel(IItemStorageChannel.class);
    public static final int NUMBER_OF_PATTERN_SLOTS = 27;
    private final MultiCraftingTracker craftingTracker;
    public final AENetworkProxy gridProxy;
    private final ICraftingManagerHost cmHost;
    private final ConfigManager cm = new ConfigManager(this);
    private final AppEngInternalInventory patterns = new AppEngInternalInventory(this, NUMBER_OF_PATTERN_SLOTS);
    private final TBActionSource actionSource;
    private int priority;
    private List<ICraftingPatternDetails> craftingList = null;
    private List<ItemStack> createPkgList = null;
    private HashMap<String, List<ItemStack>> waitingToSend = new HashMap<>();
    private Set<String> satelliteList = null;
    private String mainSatName = "";

    public DualityCraftingManager(final AENetworkProxy networkProxy, final ICraftingManagerHost cmHost) {
        this.gridProxy = networkProxy;
        this.gridProxy.setFlags(GridFlags.REQUIRE_CHANNEL);

        this.cm.registerSetting(Settings.BLOCK, YesNo.NO);

        this.cmHost = cmHost;
        this.craftingTracker = new MultiCraftingTracker(this.cmHost, 9);

        this.actionSource = new TBActionSource(this.cmHost);
    }

    private static boolean invIsCustomBlocking(BlockingInventoryAdaptor inv) {
        return (inv.containsBlockingItems());
    }

    @Override
    public void saveChanges() {
        this.cmHost.saveChanges();
    }

    @Override
    public void onChangeInventory(final IItemHandler inv, final int slot, final InvOperation mc,
        final ItemStack removed, final ItemStack added) {
        if (inv == this.patterns && (!removed.isEmpty() || !added.isEmpty())) {
            this.updateCraftingList();
        }
    }

    public void writeToNBT(final NBTTagCompound data) {
        this.patterns.writeToNBT(data, "patterns");
        this.cm.writeToNBT(data);
        this.craftingTracker.writeToNBT(data);
        data.setString("mainSatName", this.mainSatName);
        data.setInteger("priority", this.priority);

        // Saved list of Items of each satellite for next session
        NBTTagCompound satItemList = new NBTTagCompound();
        if (this.waitingToSend != null) {
            for (String satName : this.waitingToSend.keySet()) {
                // Store list of item to each satellite that is in used
                NBTTagList waitingList = new NBTTagList();
                for (final ItemStack is : this.waitingToSend.get(satName)) {
                    final NBTTagCompound item = new NBTTagCompound();
                    is.writeToNBT(item);
                    if (is.getCount() > Byte.MAX_VALUE) {
                        item.setInteger("stackSize", is.getCount());
                    }
                    waitingList.appendTag(item);
                }
                satItemList.setTag(satName, waitingList);
            }
        }

        data.setTag("satItemList", satItemList);

        NBTTagList pkgList = new NBTTagList();
        if (this.createPkgList != null) {
            for (final ItemStack is : this.createPkgList) {
                final NBTTagCompound item = new NBTTagCompound();
                is.writeToNBT(item);
                if (is.getCount() > Byte.MAX_VALUE) {
                    item.setInteger("stackSize", is.getCount());
                }
                pkgList.appendTag(item);
            }
        }

        data.setTag("__pkgList", pkgList);
    }

    public void readFromNBT(final NBTTagCompound data) {
        // Just to be sure
        this.mainSatName = data.getString("mainSatName");

        // Convert old data to new if player update
        final NBTTagList waitingList = data.getTagList("waitingToSend", 10);
        for (int x = 0; x < waitingList.tagCount(); x++) {
            final NBTTagCompound c = waitingList.getCompoundTagAt(x);
            final ItemStack is = new ItemStack(c);
            if (c.hasKey("stackSize")) {
                is.setCount(c.getInteger("stackSize"));
            }
            this.addToSendListOnSat(this.mainSatName, is);
        }

        // Retrieve item list base on sat list
        this.waitingToSend = null;

        final NBTTagCompound satItemList = data.getCompoundTag("satItemList");
        for (String satName : satItemList.getKeySet()) {
            NBTTagList w = satItemList.getTagList(satName, 10);
            for (int x = 0; x < w.tagCount(); x++) {
                final NBTTagCompound c = w.getCompoundTagAt(x);
                final ItemStack is = new ItemStack(c);
                if (c.hasKey("stackSize")) {
                    is.setCount(c.getInteger("stackSize"));
                }
                this.addToSendListOnSat(satName, is);
            }
        }

        // Wait list on main sat
        this.createPkgList = null;
        final NBTTagList pkgList = data.getTagList("__pkgList", 10);
        for (int x = 0; x < pkgList.tagCount(); x++) {
            final NBTTagCompound c = pkgList.getCompoundTagAt(x);
            final ItemStack is = new ItemStack(c);
            if (c.hasKey("stackSize")) {
                is.setCount(c.getInteger("stackSize"));
            }
            addToCreatePkgList(is);
        }

        this.craftingTracker.readFromNBT(data);
        this.patterns.readFromNBT(data, "patterns");
        this.priority = data.getInteger("priority");
        this.cm.readFromNBT(data);
        this.updateCraftingList();
    }

    private void addToSatList(final String satName) {
        if (satName.isEmpty()) {
            return;
        }

        if (this.satelliteList == null) {
            this.satelliteList = new HashSet<>();
        }

        this.satelliteList.add(satName);
    }

    private void addToSendListOnSat(final String satName, final ItemStack is) {
        if (is.isEmpty()) {
            return;
        }
        if (this.waitingToSend == null) {
            this.waitingToSend = new HashMap<>();
        }

        this.waitingToSend.computeIfAbsent(satName, k -> new ArrayList<>());

        this.waitingToSend.get(satName)
            .add(is);

        try {
            this.gridProxy.getTick()
                .wakeDevice(this.gridProxy.getNode());
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
            this.gridProxy.getTick()
                .wakeDevice(this.gridProxy.getNode());
        } catch (final GridAccessException e) {
            // :P
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private void updateCraftingList() {
        final Boolean[] accountedFor = new Boolean[this.patterns.getSlots()];
        Arrays.fill(accountedFor, false);

        if (!this.gridProxy.isReady()) {
            return;
        }

        if (this.craftingList != null) {
            this.craftingList.removeIf(details -> {
                for (int x = 0; x < accountedFor.length; x++) {
                    final ItemStack is = this.patterns.getStackInSlot(x);
                    if (details.getPattern() == is) {
                        accountedFor[x] = true; // Our pattern truly exist!!!
                        return false;
                    }
                }
                return !(details instanceof VirtualPatternHelper);
            });

            List<ItemStack> packageList = new ArrayList<>();
            for (ICraftingPatternDetails details : this.craftingList) {
                visitArray(packageList, details.getInputs(), true);
                visitArray(packageList, details.getCondensedInputs(), true);
            }

            this.craftingList.removeIf(details -> {
                for (int x = 0; x < accountedFor.length; x++) {
                    if (details instanceof VirtualPatternHelper) {
                        if (!packageList.isEmpty()) {
                            IAEItemStack[] in = details.getCondensedOutputs();
                            for (IAEItemStack iaeItemStack : in) {
                                ItemStack is = iaeItemStack == null ? ItemStack.EMPTY
                                    : iaeItemStack.asItemStackRepresentation();
                                if (!is.isEmpty() && is.getItem() == TB_ItemHandlers.itemPackage
                                    && packageList.stream()
                                        .anyMatch(s -> ItemStack.areItemStackTagsEqual(s, is))) {
                                    return false;
                                }
                            }
                        }
                    } else return false;
                }
                return true;
            });
        }

        for (int x = 0; x < accountedFor.length; x++) {
            if (!accountedFor[x]) {
                this.addToCraftingList(this.patterns.getStackInSlot(x));
            }
        }

        try {
            this.gridProxy.getGrid()
                .postEvent(new MENetworkCraftingPatternChange(this, this.gridProxy.getNode()));
        } catch (GridAccessException e) {
            e.printStackTrace();
        }
    }

    private boolean hasWorkToDo() {
        return this.hasItemsToSend() || this.hasPkgToCreate();
    }

    public void notifyNeighbors() {
        if (this.gridProxy.isActive()) {
            try {
                this.gridProxy.getGrid()
                    .postEvent(new MENetworkCraftingPatternChange(this, this.gridProxy.getNode()));
                this.gridProxy.getTick()
                    .wakeDevice(this.gridProxy.getNode());
            } catch (final GridAccessException e) {
                // :P
            }
        }

        final TileEntity te = this.cmHost.getTileEntity();
        if (te != null) {
            Platform.notifyBlocksOfNeighbors(te.getWorld(), te.getPos());
        }
    }

    private void addToCraftingList(final ItemStack is) {
        if (is.isEmpty()) {
            return;
        }

        if (is.getItem() instanceof ICraftingPatternItem) {
            final ICraftingPatternItem cpi = (ICraftingPatternItem) is.getItem();
            final ICraftingPatternDetails details = cpi.getPatternForItem(
                is,
                this.cmHost.getTileEntity()
                    .getWorld());

            if (details != null) {
                if (this.craftingList == null) {
                    this.craftingList = new ArrayList<>();
                }

                List<ItemStack> packageList = new ArrayList<>();

                final IAEItemStack[] in = details.getInputs();
                final IAEItemStack[] cin = details.getCondensedInputs();

                visitArray(packageList, in, false);
                visitArray(packageList, cin, false);

                packageList.stream()
                    .map(pkg -> VirtualPatternAE.newPattern(PackageHelper.getItemStack(pkg, true, true), pkg))
                    .forEach(this.craftingList::add);

                this.craftingList.add(details);
            }
        }
    }

    private boolean hasPkgToCreate() {
        return this.createPkgList != null && !this.createPkgList.isEmpty();
    }

    private boolean hasItemsToSend() {
        if (this.waitingToSend != null) {
            for (String satName : this.waitingToSend.keySet()) {
                if (!this.waitingToSend.get(satName)
                    .isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    // TODO: Upgradable
    // public void dropExcessPatterns() {
    // IItemHandler patterns = getPatterns();
    //
    // List<ItemStack> dropList = new ArrayList<>();
    // for (int invSlot = 0; invSlot < patterns.getSlots(); invSlot++) {
    // if (invSlot > NUMBER_OF_PATTERN_SLOTS - 1) {
    // ItemStack is = patterns.getStackInSlot(invSlot);
    // if (is.isEmpty()) {
    // continue;
    // }
    // dropList.add(patterns.extractItem(invSlot, Integer.MAX_VALUE, false));
    // }
    // }
    // if (dropList.size() > 0) {
    // World world = this.getLocation().getWorld();
    // BlockPos blockPos = this.getLocation().getPos();
    // Platform.spawnDrops(world, blockPos, dropList);
    // }
    //
    // this.gridProxy.setIdlePowerUsage(Math.pow(4, 4));
    // }

    @Override
    public boolean canInsert(final ItemStack stack) {
        return false;
    }

    public IItemHandler getPatterns() {
        return this.patterns;
    }

    public void gridChanged() {
        this.notifyNeighbors();
    }

    @SuppressWarnings("UnusedDeclaration")
    public AECableType getCableConnectionType(final AEPartLocation dir) {
        return AECableType.SMART;
    }

    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this.cmHost.getTileEntity());
    }

    public IItemHandler getInternalInventory() {
        return new ItemStackHandler(0);
    }

    @Override
    public TickingRequest getTickingRequest(final IGridNode node) {
        return new TickingRequest(
            TickRates.Inscriber.getMin(),
            TickRates.Interface.getMax(),
            !this.hasWorkToDo(),
            true);
    }

    @Override
    public TickRateModulation tickingRequest(final IGridNode node, final int ticksSinceLastCall) {
        if (!this.gridProxy.isActive()) {
            return TickRateModulation.SLEEP;
        }

        if (this.hasPkgToCreate()) {
            this.createPkg();
            return TickRateModulation.URGENT;
        }

        if (this.hasItemsToSend()) {
            this.pushItemsOut();
        }

        return this.hasWorkToDo() ? TickRateModulation.SLOWER : TickRateModulation.SLEEP;
    }

    private void pushItemsOut() {
        if (!this.hasItemsToSend()) {
            return;
        }

        for (String satName : this.waitingToSend.keySet()) {
            final PartSatelliteBus part = findSatellite(satName);
            if (part == null) return;

            final World w = part.getTile()
                .getWorld();
            final TileEntity te = w.getTileEntity(
                part.getTile()
                    .getPos()
                    .offset(part.getTargets()));
            final InventoryAdaptor ad = InventoryAdaptor.getAdaptor(
                te,
                part.getTargets()
                    .getOpposite());
            if (ad == null) return;

            Iterator<ItemStack> i = this.waitingToSend.get(satName)
                .iterator();

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
        }

        this.waitingToSend.values()
            .removeIf(List::isEmpty);

        if (this.waitingToSend.isEmpty()) {
            this.waitingToSend = null;
        }
    }

    public TileEntity getTile() {
        return (TileEntity) (this.cmHost instanceof TileEntity ? this.cmHost : null);
    }

    @Override
    public <T extends IAEStack<T>> IMEMonitor<T> getInventory(IStorageChannel<T> channel) {
        return null;
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        if (name.equals("patterns")) {
            return this.patterns;
        }

        return new ItemStackHandler(0);
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.cm;
    }

    @Override
    public void updateSetting(final IConfigManager manager, final Enum settingName, final Enum newValue) {
        this.cmHost.saveChanges();
    }

    private boolean invIsBlocked(InventoryAdaptor inv) {
        return (inv.containsItems());
    }

    @Override
    public boolean pushPattern(final ICraftingPatternDetails patternDetails, final InventoryCrafting table) {
        if (patternDetails instanceof VirtualPatternHelper) {
            ItemStack output = patternDetails.getCondensedOutputs()[0].getDefinition();
            // If pattern has output contains package, we will work here first instead
            if (output.getItem() == TB_ItemHandlers.itemPackage && !PackageHelper.getItemStack(output, true, true)
                .isEmpty()) {
                this.addToCreatePkgList(output);
                return true;
            }
        }

        if (this.hasItemsToSend() || this.hasItemsToSend()
            || !this.gridProxy.isActive()
            || !this.craftingList.contains(patternDetails)
            || this.satelliteList != null) {
            return false;
        }

        for (int x = 0; x < table.getSizeInventory(); x++) {
            final ItemStack is = table.getStackInSlot(x);
            if (!is.isEmpty()) {
                if (is.getItem() == TB_ItemHandlers.itemPackage && !PackageHelper.getItemStack(is, true, true)
                    .isEmpty()) {
                    String satName = PackageHelper.getItemInfo(is, PackageHelper.ItemInfo.DESTINATION);
                    if (!satName.isEmpty()) this.addToSatList(satName);
                    else return this.cleanCrafting();
                } else {
                    if (!mainSatName.isEmpty()) this.addToSatList(mainSatName);
                    else return this.cleanCrafting();
                }
            }
        }

        if (!this.blockCheckAll()) {
            for (String satName : this.satelliteList) {
                final PartSatelliteBus sat = this.findSatellite(satName);
                if (sat == null) return this.cleanCrafting();
                final World w = sat.getTile()
                    .getWorld();
                final TileEntity te = w.getTileEntity(
                    sat.getTile()
                        .getPos()
                        .offset(sat.getTargets()));
                InventoryAdaptor ad = InventoryAdaptor.getAdaptor(
                    te,
                    sat.getTargets()
                        .getOpposite());
                if (this.acceptsItems(ad, table)) {
                    for (int x = 0; x < table.getSizeInventory(); x++) {
                        final ItemStack is = table.getStackInSlot(x);
                        if (!is.isEmpty()) {
                            if (is.getItem() == TB_ItemHandlers.itemPackage) {
                                String name = PackageHelper.getItemInfo(is, PackageHelper.ItemInfo.DESTINATION);
                                ItemStack holder = PackageHelper.getItemStack(is, true, false);
                                if (satName.equals(name) && !holder.isEmpty()) {
                                    this.addToSendListOnSat(satName, holder);
                                    table.removeStackFromSlot(x);
                                }
                            } else {
                                this.addToSendListOnSat(mainSatName, is);
                                table.removeStackFromSlot(x);
                            }
                        }
                    }
                }
            }
            if (isTableEmpty(table)) {
                this.satelliteList = null;
                this.pushItemsOut();
                return true;
            }
        }
        return this.cleanCrafting();
    }

    @Override
    public boolean isBusy() {
        boolean allBusy = true;

        if (this.waitingToSend != null && !this.waitingToSend.isEmpty()) {
            for (String satName : this.waitingToSend.keySet()) {
                if (!this.blockCheckByName(satName)) {
                    allBusy = false;
                }
            }
        } else allBusy = false;

        return this.hasItemsToSend() || this.hasItemsToSend() || allBusy;
    }

    /**
     * Using satName to check if sat is available or not.
     */
    @SuppressWarnings("ConstantConditions") // Remove NPE warning
    private boolean blockCheckByName(String satName) {
        if (this.isBlocking()) {
            final PartSatelliteBus sat = this.findSatellite(satName);
            if (sat == null) return true;
            InventoryAdaptor ad = sat.getInvAdaptor();
            if (ad == null) return true;
            final TileEntity te = sat.getNeighborTE();
            final World w = te.getWorld();
            IPhantomTile phantomTE;
            if (Loader.isModLoaded("actuallyadditions") && te instanceof IPhantomTile) {
                phantomTE = ((IPhantomTile) te);
                if (phantomTE.hasBoundPosition()) {
                    TileEntity phantom = w.getTileEntity(phantomTE.getBoundPosition());
                    if (NonBlockingItems.INSTANCE.getMap()
                        .containsKey(
                            w.getBlockState(phantomTE.getBoundPosition())
                                .getBlock()
                                .getRegistryName()
                                .getNamespace())) {
                        return this.isCustomInvBlocking(phantom, sat.getTargets());
                    }
                }
            } else if (NonBlockingItems.INSTANCE.getMap()
                .containsKey(
                    w.getBlockState(
                        sat.getTile()
                            .getPos()
                            .offset(sat.getTargets()))
                        .getBlock()
                        .getRegistryName()
                        .getNamespace())) {
                            return this.isCustomInvBlocking(te, sat.getTargets());
                        } else {
                            return this.invIsBlocked(ad);
                        }
        }

        return false;
    }

    /**
     * Check any sat in satelliteList if is available or not.
     */
    @SuppressWarnings("ConstantConditions") // Remove NPE warning
    private boolean blockCheckAll() {
        boolean isBusy = false;
        if (this.isBlocking() && this.satelliteList != null) {
            for (String satName : this.satelliteList) {
                final PartSatelliteBus sat = this.findSatellite(satName);
                if (sat == null) return true;
                InventoryAdaptor ad = sat.getInvAdaptor();
                if (ad == null) return true;
                final TileEntity te = sat.getNeighborTE();
                final World w = te.getWorld();
                IPhantomTile phantomTE;
                if (Loader.isModLoaded("actuallyadditions") && te instanceof IPhantomTile) {
                    phantomTE = ((IPhantomTile) te);
                    if (phantomTE.hasBoundPosition()) {
                        TileEntity phantom = w.getTileEntity(phantomTE.getBoundPosition());
                        if (NonBlockingItems.INSTANCE.getMap()
                            .containsKey(
                                w.getBlockState(phantomTE.getBoundPosition())
                                    .getBlock()
                                    .getRegistryName()
                                    .getNamespace())) {
                            isBusy = this.isCustomInvBlocking(phantom, sat.getTargets());
                        }
                    }
                } else if (NonBlockingItems.INSTANCE.getMap()
                    .containsKey(
                        w.getBlockState(
                            sat.getTile()
                                .getPos()
                                .offset(sat.getTargets()))
                            .getBlock()
                            .getRegistryName()
                            .getNamespace())) {
                                isBusy = this.isCustomInvBlocking(te, sat.getTargets());
                            } else {
                                isBusy = this.invIsBlocked(ad);
                            }
                if (isBusy) break;
            }
        }

        return isBusy;
    }

    private boolean isTableEmpty(final InventoryCrafting table) {
        for (int x = 0; x < table.getSizeInventory(); x++) {
            if (!table.getStackInSlot(x)
                .isEmpty()) return false;
        }
        return true;
    }

    boolean isCustomInvBlocking(TileEntity te, EnumFacing s) {
        BlockingInventoryAdaptor blockingInventoryAdaptor = BlockingInventoryAdaptor.getAdaptor(te, s.getOpposite());
        return invIsCustomBlocking(blockingInventoryAdaptor);
    }

    private boolean isBlocking() {
        return this.cm.getSetting(Settings.BLOCK) == YesNo.YES; // TODO
    }

    private boolean acceptsItems(final InventoryAdaptor ad, final InventoryCrafting table) {
        for (int x = 0; x < table.getSizeInventory(); x++) {
            final ItemStack is = table.getStackInSlot(x);
            if (is.isEmpty()) {
                continue;
            }

            if (!ad.simulateAdd(is)
                .isEmpty()) {
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
            for (List<ItemStack> itemList : this.waitingToSend.values()) {
                for (final ItemStack is : itemList) {
                    if (!is.isEmpty()) {
                        drops.add(is);
                    }
                }
            }
        }

        if (this.createPkgList != null) {
            for (final ItemStack is : this.createPkgList) {
                if (!is.isEmpty()) {
                    // Return items instead of not useful placeholder
                    drops.add(PackageHelper.getItemStack(is, true, false));
                }
            }
        }

        for (final ItemStack is : this.patterns) {
            if (!is.isEmpty()) {
                drops.add(is);
            }
        }
    }

    @SuppressWarnings("Redundant")
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
        return (IPart) (this.cmHost instanceof IPart ? this.cmHost : null);
    }

    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return this.craftingTracker.getRequestedJobs();
    }

    @SuppressWarnings("UnusedDeclaration")
    public IAEItemStack injectCraftedItems(final ICraftingLink link, final IAEItemStack acquired,
        final Actionable mode) {
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
        this.cmHost.saveChanges();

        try {
            this.gridProxy.getGrid()
                .postEvent(new MENetworkCraftingPatternChange(this, this.gridProxy.getNode()));
        } catch (final GridAccessException e) {
            // :P
        }
    }

    public String getSatelliteName() {
        return this.mainSatName;
    }

    public PartSatelliteBus getSatellitePart() {
        return this.findSatellite(this.mainSatName);
    }

    public void setSatellite(final String newValue) {
        this.mainSatName = newValue;
        this.cmHost.saveChanges();
    }

    @SuppressWarnings("UnusedDeclaration")
    private void createPkg() {
        if (this.createPkgList == null) {
            return;
        }

        for (ItemStack is : this.createPkgList) {
            if (this.gridProxy.getNode() == null || is.isEmpty()) return;
            IMEInventoryHandler<IAEItemStack> i = ((IStorageGrid) this.gridProxy.getNode()
                .getGrid()
                .getCache(IStorageGrid.class)).getInventory(ITEMS);
            IAEItemStack st = ITEMS.createStack(is);
            IAEItemStack r = i.injectItems(st, Actionable.MODULATE, this.actionSource);
        }

        this.createPkgList = null;
    }

    private void visitArray(List<ItemStack> output, IAEItemStack[] input, boolean isPlaceholder) {
        for (int i = 0; i < input.length; i++) {
            if (input[i] != null) {
                ItemStack is = input[i].createItemStack();
                if (is.getItem() == TB_ItemHandlers.itemPackage) {
                    if (is.hasTagCompound() && is.getTagCompound()
                        .getBoolean("__actContainer") == isPlaceholder) {
                        if (!isPlaceholder) {
                            is = is.copy();
                            is.getTagCompound()
                                .setBoolean("__actContainer", true);
                            input[i] = ITEMS.createStack(is);
                        }
                    }
                    is.setCount(1);
                    output.add(is);
                }
            }
        }
    }

    private boolean cleanCrafting() {
        this.satelliteList = null;
        this.waitingToSend = null;
        return false;
    }

    public PartSatelliteBus findSatellite(String name) {
        if (!name.isEmpty()) {
            for (final IGridNode node : this.gridProxy.getNode()
                .getGrid()
                .getMachines(PartSatelliteBus.class)) {
                IGridHost h = node.getMachine();
                if (h instanceof PartSatelliteBus) {
                    PartSatelliteBus part = (PartSatelliteBus) h;
                    if (part.getSatelliteName()
                        .equals(name)) {
                        return part;
                    }
                }
            }
        }
        return null;
    }
}
