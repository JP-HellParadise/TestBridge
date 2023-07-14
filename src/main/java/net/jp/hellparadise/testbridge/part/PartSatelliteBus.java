package net.jp.hellparadise.testbridge.part;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartModel;
import appeng.api.util.AECableType;
import appeng.core.settings.TickRates;
import appeng.helpers.Reflected;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;
import appeng.parts.automation.PartSharedItemBus;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import com.cleanroommc.modularui.screen.ModularScreen;
import java.util.*;
import javax.annotation.Nonnull;
import net.jp.hellparadise.testbridge.client.gui.SatelliteGuiHolder;
import net.jp.hellparadise.testbridge.core.Reference;
import net.jp.hellparadise.testbridge.core.TB_ItemHandlers;
import net.jp.hellparadise.testbridge.helpers.PackageHelper;
import net.jp.hellparadise.testbridge.helpers.interfaces.SatelliteInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

public class PartSatelliteBus extends PartSharedItemBus implements SatelliteGuiHolder {

    public static final ResourceLocation MODEL_BASE = new ResourceLocation(Reference.MODID, "part/satellite_bus_base");
    @PartModels
    public static final IPartModel MODELS_OFF = new PartModel(
        MODEL_BASE,
        new ResourceLocation(Reference.MODID, "part/satellite_bus_off"));
    @PartModels
    public static final IPartModel MODELS_ON = new PartModel(
        MODEL_BASE,
        new ResourceLocation(Reference.MODID, "part/satellite_bus_on"));
    @PartModels
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(
        MODEL_BASE,
        new ResourceLocation(Reference.MODID, "part/satellite_bus_has_channel"));

    public static final Set<PartSatelliteBus> AllSatellites = Collections.newSetFromMap(new WeakHashMap<>());
    private String satPartName = "";

    @Reflected
    public PartSatelliteBus(ItemStack is) {
        super(is);
    }

    // called only on server shutdown
    public static void cleanup() {
        PartSatelliteBus.AllSatellites.clear();
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
    public void writeToNBT(final NBTTagCompound extra) {
        super.writeToNBT(extra);
        extra.setString("__satName", this.satPartName);
    }

    @Override
    public void readFromNBT(final NBTTagCompound extra) {
        super.readFromNBT(extra);

        if (extra.hasKey("satName")) this.satPartName = extra.getString("satName");
        else this.satPartName = extra.getString("__satName");

        if (Platform.isServer()) {
            ensureAllSatelliteStatus();
        }
    }

    @Override
    public void removeFromWorld() {
        super.removeFromWorld();
        PartSatelliteBus.AllSatellites.remove(this);
    }

    @Nonnull
    @Override
    public TickingRequest getTickingRequest(@Nonnull final IGridNode node) {
        return new TickingRequest(TickRates.Interface.getMax(), TickRates.Interface.getMax(), this.isSleeping(), false);
    }

    @Nonnull
    @Override
    public TickRateModulation tickingRequest(@Nonnull final IGridNode node, final int ticksSinceLastCall) {
        return this.doBusWork();
    }

    @Override
    protected TickRateModulation doBusWork() {
        if (!this.getProxy()
            .isActive() || !this.canDoBusWork()) {
            return TickRateModulation.IDLE;
        }
        return TickRateModulation.SLOWER;
    }

    @Override
    public void getBoxes(final IPartCollisionHelper bch) {
        bch.addBox(5, 5, 12, 11, 11, 13);
        bch.addBox(3, 3, 13, 13, 13, 14);
        bch.addBox(2, 2, 14, 14, 14, 16);
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 1;
    }



    @Override
    public boolean onPartActivate(final EntityPlayer player, final EnumHand hand, final Vec3d posIn) {
        if (Platform.isServer()) {
            ItemStack itemStack = player.getHeldItem(hand);
            if (itemStack.getItem() == TB_ItemHandlers.itemPackage) {
                PackageHelper.setDestination(itemStack, satPartName);
            } else {
                openUI((EntityPlayerMP) player);
            }
        }
        return true;
    }

    @Nonnull
    @Override
    public TileEntity getContainer() {
        return this.getTile();
    }

    public EnumFacing getTargets() {
        return this.getSide()
            .getFacing();
    }

    @Nonnull
    @Override
    public Set<SatelliteInfo> getSatellitesOfType() {
        return Collections.unmodifiableSet(PartSatelliteBus.AllSatellites);
    }

    @Nonnull
    @Override
    public String getSatelliteName() {
        return this.satPartName;
    }

    @Override
    public void setSatelliteName(@Nonnull String __satName) {
        if (satPartName.equals(__satName)) return;
        this.satPartName = __satName;
        ensureAllSatelliteStatus();
        this.getTile().markDirty();
    }

    @Override
    public void ensureAllSatelliteStatus() {
        if (this.satPartName.isEmpty()) {
            PartSatelliteBus.AllSatellites.remove(this);
        } else {
            PartSatelliteBus.AllSatellites.add(this);
        }
    }

    public TileEntity getNeighborTE() {
        // Nested nightmare =))
        return this.getTile()
            .getWorld()
            .getTileEntity(
                this.getTile()
                    .getPos()
                    .offset(this.getTargets()));
    }

    public InventoryAdaptor getInvAdaptor() {
        return InventoryAdaptor.getAdaptor(
            this.getNeighborTE(),
            this.getTargets()
                .getOpposite());
    }

    @Override
    public NBTTagCompound downloadSettings(SettingsFrom from) {
        NBTTagCompound output = new NBTTagCompound();
        if (!satPartName.isEmpty()) output.setString("_satName", getSatelliteName());
        return output;
    }

    @Override
    public void uploadSettings(SettingsFrom from, NBTTagCompound compound, EntityPlayer player) {
        if (compound.hasKey("_satName")) {
            String newName = compound.getString("_satName");
            if (this.getSatellitesOfType()
                .stream()
                .anyMatch(
                    it -> it.getSatelliteName()
                        .equals(newName))) {
                sendStatus(player, Status.DUPLICATED);
            } else {
                sendStatus(player, Status.SUCCESS);
                this.setSatelliteName(newName);
            }
        }
    }

    private void sendStatus(@Nonnull EntityPlayer player, @Nonnull Status status) {
        if (Platform.isClient()) {
            return;
        }

        switch (status) {
            case SUCCESS:
                player.sendStatusMessage(
                    new TextComponentTranslation("chat.testbridge.satellite_bus.duplicated"),
                    true);
                break;
            case DUPLICATED:
                player.sendStatusMessage(
                    new TextComponentTranslation("chat.testbridge.satellite_bus.success"),
                    true);
                break;
            default:
                player.sendStatusMessage(
                    new TextComponentString("Something weird happen!! Report this to Korewa_Li"),
                    true);
                break;
        }
    }

    @Override
    public boolean isAE2Part() {
        return true;
    }

    @Override
    public ModularScreen createClientGui(EntityPlayer player) {
        return ModularScreen.simple("result", this::createPanel);
    }

    private enum Status {
        SUCCESS,
        DUPLICATED,
        FAILED
    }
}
