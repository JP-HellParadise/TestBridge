package net.jp.hellparadise.testbridge.block.tile;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.Upgrades;
import appeng.api.definitions.IMaterials;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.api.util.IConfigManager;
import appeng.core.localization.PlayerMessages;
import appeng.core.sync.GuiBridge;
import appeng.helpers.IPriorityHost;
import appeng.items.misc.ItemEncodedPattern;
import appeng.tile.grid.AENetworkInvTile;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import appeng.util.inv.IInventoryDestination;
import appeng.util.inv.InvOperation;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import javax.annotation.Nonnull;
import net.jp.hellparadise.testbridge.client.gui.GuiSatelliteSelect;
import net.jp.hellparadise.testbridge.helpers.DualityCraftingManager;
import net.jp.hellparadise.testbridge.helpers.interfaces.ae2.AccessorApiBlocks;
import net.jp.hellparadise.testbridge.helpers.interfaces.ae2.ICraftingManagerHost;
import net.jp.hellparadise.testbridge.part.PartSatelliteBus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

public class TileEntityCraftingManager extends AENetworkInvTile
    implements IGridTickable, IInventoryDestination, ICraftingManagerHost, IPriorityHost, GuiSatelliteSelect {

    private final DualityCraftingManager duality = new DualityCraftingManager(this.getProxy(), this);

    @MENetworkEventSubscribe
    @SuppressWarnings("unused") // Used by AE2
    public void stateChange(final MENetworkChannelsChanged c) {
        this.duality.notifyNeighbors();
    }

    @MENetworkEventSubscribe
    @SuppressWarnings("unused") // Used by AE2
    public void stateChange(final MENetworkPowerStatusChange c) {
        this.duality.notifyNeighbors();
    }

    @Override
    public void getDrops(final World w, final BlockPos pos, final List<ItemStack> drops) {
        this.duality.addDrops(drops);
    }

    @Override
    public void gridChanged() {
        this.duality.gridChanged();
    }

    @Override
    public void onReady() {
        super.onReady();
        this.duality.initialize();
        this.getProxy()
            .setIdlePowerUsage(Math.pow(4, (this.getInstalledUpgrades(Upgrades.PATTERN_EXPANSION))));
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);
        this.duality.writeToNBT(data);
        return data;
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        super.readFromNBT(data);
        this.duality.readFromNBT(data);
    }

    @Nonnull
    @Override
    public AECableType getCableConnectionType(final AEPartLocation dir) {
        return this.duality.getCableConnectionType(dir);
    }

    @Override
    public DimensionalCoord getLocation() {
        return this.duality.getLocation();
    }

    @Override
    public boolean canInsert(final ItemStack stack) {
        return this.duality.canInsert(stack);
    }

    @Override
    public IItemHandler getInventoryByName(final String name) {
        return this.duality.getInventoryByName(name);
    }

    @Nonnull
    @Override
    public TickingRequest getTickingRequest(@Nonnull final IGridNode node) {
        return this.duality.getTickingRequest(node);
    }

    @Nonnull
    @Override
    public TickRateModulation tickingRequest(@Nonnull final IGridNode node, final int ticksSinceLastCall) {
        return this.duality.tickingRequest(node, ticksSinceLastCall);
    }

    @Nonnull
    @Override
    public IItemHandler getInternalInventory() {
        return this.duality.getInternalInventory();
    }

    @Override
    public void onChangeInventory(final IItemHandler inv, final int slot, final InvOperation mc,
        final ItemStack removed, final ItemStack added) {
        this.duality.onChangeInventory(inv, slot, mc, removed, added);
    }

    @Override
    public DualityCraftingManager getCMDuality() {
        return this.duality;
    }

    @Override
    public TileEntity getTileEntity() {
        return this;
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.duality.getConfigManager();
    }

    @Override
    public boolean pushPattern(final ICraftingPatternDetails patternDetails, final InventoryCrafting table) {
        return this.duality.pushPattern(patternDetails, table);
    }

    @Override
    public boolean isBusy() {
        return this.duality.isBusy();
    }

    @Override
    public void provideCrafting(final ICraftingProviderHelper craftingTracker) {
        this.duality.provideCrafting(craftingTracker);
    }

    @Override
    public int getInstalledUpgrades(final Upgrades u) {
        return 0;
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return this.duality.getRequestedJobs();
    }

    @Override
    public IAEItemStack injectCraftedItems(final ICraftingLink link, final IAEItemStack items, final Actionable mode) {
        return this.duality.injectCraftedItems(link, items, mode);
    }

    @Override
    public void jobStateChange(final ICraftingLink link) {
        this.duality.jobStateChange(link);
    }

    @Override
    public int getPriority() {
        return this.duality.getPriority();
    }

    @Override
    public void setPriority(final int newValue) {
        this.duality.setPriority(newValue);
    }

    @Override
    public String getSatelliteName() {
        return this.duality.getSatelliteName();
    }

    @Override
    public PartSatelliteBus getSatellitePart() {
        return this.duality.getSatellitePart();
    }

    @Override
    public void setSatelliteName(String satName) {
        this.duality.setSatelliteName(satName);
    }

    @Override
    public ItemStack getItemStackRepresentation() {
        return ((AccessorApiBlocks) AEApi.instance()
            .definitions()
            .blocks()).cmBlock()
                .maybeStack(1)
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public GuiBridge getGuiBridge() {
        return GuiBridge.valueOf("GUI_CRAFTING_MANAGER");
    }

    @Override
    public BlockPos getBlockPos() {
        return this.pos;
    }

    @Override
    public NBTTagCompound downloadSettings(SettingsFrom from) {
        NBTTagCompound output = super.downloadSettings(from);
        if (from == SettingsFrom.MEMORY_CARD) {
            final IItemHandler inv = this.getInventoryByName("patterns");
            ((AppEngInternalInventory) inv).writeToNBT(output, "patterns");
        }
        if (!getSatelliteName().isEmpty()) output.setString("__satSelect", getSatelliteName());
        return output;
    }

    @Override
    public void uploadSettings(SettingsFrom from, NBTTagCompound compound, EntityPlayer player) {
        super.uploadSettings(from, compound, player);
        final IItemHandler inv = this.getInventoryByName("patterns");
        final AppEngInternalInventory target = (AppEngInternalInventory) inv;
        AppEngInternalInventory tmp = new AppEngInternalInventory(null, target.getSlots());
        tmp.readFromNBT(compound, "patterns");
        PlayerMainInvWrapper playerInv = new PlayerMainInvWrapper(player.inventory);
        final IMaterials materials = AEApi.instance()
            .definitions()
            .materials();
        int missingPatternsToEncode = 0;

        for (int i = 0; i < inv.getSlots(); i++) {
            if (target.getStackInSlot(i)
                .getItem() instanceof ItemEncodedPattern) {
                ItemStack blank = materials.blankPattern()
                    .maybeStack(
                        target.getStackInSlot(i)
                            .getCount())
                    .orElse(ItemStack.EMPTY);
                if (!player.addItemStackToInventory(blank)) {
                    player.dropItem(blank, true);
                }
                target.setStackInSlot(i, ItemStack.EMPTY);
            }
        }

        for (int x = 0; x < tmp.getSlots(); x++) {
            if (!tmp.getStackInSlot(x)
                .isEmpty()) {
                boolean found = false;
                for (int i = 0; i < playerInv.getSlots(); i++) {
                    if (materials.blankPattern()
                        .isSameAs(playerInv.getStackInSlot(i))) {
                        target.setStackInSlot(x, tmp.getStackInSlot(x));
                        playerInv.getStackInSlot(i)
                            .shrink(1);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    missingPatternsToEncode++;
                }
            }
        }
        if (Platform.isServer() && missingPatternsToEncode > 0) {
            player.sendMessage(PlayerMessages.MissingPatternsToEncode.get());
        }

        if (compound.hasKey("__satSelect")) setSatelliteName(compound.getString("__satSelect"));
    }
}
