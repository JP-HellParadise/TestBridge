package net.jp.hellparadise.testbridge.part;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
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
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartModel;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.IStorageMonitorable;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.AECableType;
import appeng.api.util.IConfigManager;
import appeng.core.AppEng;
import appeng.core.localization.PlayerMessages;
import appeng.core.sync.GuiBridge;
import appeng.helpers.IPriorityHost;
import appeng.helpers.Reflected;
import appeng.items.misc.ItemEncodedPattern;
import appeng.items.parts.PartModels;
import appeng.parts.PartBasicState;
import appeng.parts.PartModel;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.IInventoryDestination;
import appeng.util.inv.InvOperation;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import javax.annotation.Nonnull;
import net.jp.hellparadise.testbridge.client.gui.GuiSatelliteSelect;
import net.jp.hellparadise.testbridge.core.Reference;
import net.jp.hellparadise.testbridge.helpers.DualityCraftingManager;
import net.jp.hellparadise.testbridge.helpers.interfaces.ae2.AccessorApiParts;
import net.jp.hellparadise.testbridge.helpers.interfaces.ae2.ICraftingManagerHost;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

public class PartCraftingManager extends PartBasicState implements IGridTickable, IStorageMonitorable,
    IInventoryDestination, ICraftingManagerHost, IAEAppEngInventory, IPriorityHost, GuiSatelliteSelect {

    public static final ResourceLocation MODEL_BASE = new ResourceLocation(
        Reference.MOD_ID,
        "part/crafting_manager_base");

    @PartModels
    public static final PartModel MODELS_OFF = new PartModel(
        MODEL_BASE,
        new ResourceLocation(AppEng.MOD_ID, "part/interface_off"));

    @PartModels
    public static final PartModel MODELS_ON = new PartModel(
        MODEL_BASE,
        new ResourceLocation(AppEng.MOD_ID, "part/interface_on"));

    @PartModels
    public static final PartModel MODELS_HAS_CHANNEL = new PartModel(
        MODEL_BASE,
        new ResourceLocation(AppEng.MOD_ID, "part/interface_has_channel"));

    private final DualityCraftingManager duality = new DualityCraftingManager(this.getProxy(), this);

    @Reflected
    public PartCraftingManager(final ItemStack is) {
        super(is);
    }

    @Override
    @MENetworkEventSubscribe
    public void chanRender(final MENetworkChannelsChanged c) {
        this.duality.notifyNeighbors();
    }

    @Override
    @MENetworkEventSubscribe
    public void powerRender(final MENetworkPowerStatusChange c) {
        this.duality.notifyNeighbors();
    }

    @Override
    public void getBoxes(final IPartCollisionHelper bch) {
        bch.addBox(2, 2, 14, 14, 14, 16);
        bch.addBox(5, 5, 12, 11, 11, 14);
    }

    @Override
    public void gridChanged() {
        this.duality.gridChanged();
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        super.readFromNBT(data);
        this.duality.readFromNBT(data);
    }

    @Override
    public void writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);
        this.duality.writeToNBT(data);
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        this.duality.initialize();
    }

    @Override
    public void getDrops(final List<ItemStack> drops, final boolean wrenched) {
        this.duality.addDrops(drops);
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 4;
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.duality.getConfigManager();
    }

    @Override
    public IItemHandler getInventoryByName(final String name) {
        return this.duality.getInventoryByName(name);
    }

    @Override
    public boolean onPartActivate(final EntityPlayer p, final EnumHand hand, final Vec3d posIn) {
        if (Platform.isServer()) {
            Platform.openGUI(p, this.getTile(), this.getSide(), this.getGuiBridge());
        }
        return true;
    }

    @Override
    public <T extends IAEStack<T>> IMEMonitor<T> getInventory(IStorageChannel<T> channel) {
        return this.duality.getInventory(channel);
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

    @Override
    public void onChangeInventory(final IItemHandler inv, final int slot, final InvOperation mc,
        final ItemStack removedStack, final ItemStack newStack) {
        this.duality.onChangeInventory(inv, slot, mc, removedStack, newStack);
    }

    @Override
    public DualityCraftingManager getCMDuality() {
        return this.duality;
    }

    @Override
    public TileEntity getTileEntity() {
        return super.getHost().getTile();
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

    @Nonnull
    @Override
    public IPartModel getStaticModels() {
        if (this.isActive() && this.isPowered()) {
            return MODELS_HAS_CHANNEL;
        } else if (this.isPowered()) {
            return MODELS_ON;
        } else {
            return MODELS_OFF;
        }
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
        return ((AccessorApiParts) AEApi.instance()
                .definitions()
                .parts()).craftingManager()
                .maybeStack(1)
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public GuiBridge getGuiBridge() {
        return GuiBridge.valueOf("GUI_CRAFTING_MANAGER");
    }

    @Override
    public BlockPos getBlockPos() {
        return this.getTile()
            .getPos();
    }

    @Override
    public NBTTagCompound downloadSettings(SettingsFrom from) {
        NBTTagCompound output = super.downloadSettings(from);
        if (from == SettingsFrom.MEMORY_CARD) {
            final IItemHandler inv = this.getInventoryByName("patterns");
            if (inv instanceof AppEngInternalInventory) {
                ((AppEngInternalInventory) inv).writeToNBT(output, "patterns");
            }
        }
        if (!getSatelliteName().isEmpty()) output.setString("__satSelect", getSatelliteName());
        return output;
    }

    @Override
    public void uploadSettings(SettingsFrom from, NBTTagCompound compound, EntityPlayer player) {
        super.uploadSettings(from, compound, player);
        final IItemHandler inv = this.getInventoryByName("patterns");
        if (inv instanceof AppEngInternalInventory) {
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
                        .get();
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
        }
        if (compound.hasKey("__satSelect")) setSatelliteName(compound.getString("__satSelect"));
    }

    @Override
    public boolean canInsert(ItemStack itemStack) {
        return this.duality.canInsert(itemStack);
    }

    @Override
    public int sideOrdinal() {
        return this.getSide().ordinal();
    }
}
