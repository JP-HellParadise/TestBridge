package testbridge.part;

import javax.annotation.Nonnull;
import java.util.*;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.ResourceLocation;

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

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifierStack;

import network.rs485.logisticspipes.SatellitePipe;
import network.rs485.logisticspipes.util.TextUtil;

import testbridge.core.TestBridge;
import testbridge.items.FakeItem;
import testbridge.network.GuiIDs;
import testbridge.network.packets.resultpackethandler.TB_SyncNamePacket;

public class PartSatelliteBus extends PartSharedItemBus implements SatellitePipe {

  public static final ResourceLocation MODEL_BASE = new ResourceLocation( TestBridge.MODID, "part/satellite_bus_base" );
  @PartModels
  public static final IPartModel MODELS_OFF = new PartModel( MODEL_BASE, new ResourceLocation( TestBridge.MODID, "part/satellite_bus_off" ) );
  @PartModels
  public static final IPartModel MODELS_ON = new PartModel( MODEL_BASE, new ResourceLocation( TestBridge.MODID, "part/satellite_bus_on" ) );
  @PartModels
  public static final IPartModel MODELS_HAS_CHANNEL = new PartModel( MODEL_BASE, new ResourceLocation( TestBridge.MODID, "part/satellite_bus_has_channel" ) );

  public static final Set<PartSatelliteBus> AllSatellites = Collections.newSetFromMap(new WeakHashMap<>());
  public final PlayerCollectionList localModeWatchers = new PlayerCollectionList();
  private String satPartName = "";

  @Reflected
  public PartSatelliteBus(ItemStack is) {
    super(is);
  }

  // called only on server shutdown
  public static void cleanup() {
    PartSatelliteBus.AllSatellites.clear();
  }

  @Override
  public void writeToNBT(final NBTTagCompound extra) {
    super.writeToNBT(extra);
    extra.setString("__satName", this.satPartName);
  }

  @Override
  public void readFromNBT(final NBTTagCompound extra) {
    super.readFromNBT(extra);

    if (extra.hasKey("satName"))
      this.satPartName = extra.getString("satName");
    else
      this.satPartName = extra.getString("__satName");

    if (MainProxy.isServer(getTile().getWorld())) {
      ensureAllSatelliteStatus();
    }
  }

  @Nonnull
  @Override
  public TickingRequest getTickingRequest(@Nonnull final IGridNode node ) {
    return new TickingRequest( TickRates.Interface.getMax(), TickRates.Interface.getMax(), this.isSleeping(), false );
  }

  @Nonnull
  @Override
  public TickRateModulation tickingRequest(@Nonnull final IGridNode node, final int ticksSinceLastCall ) {
    return this.doBusWork();
  }

  @Override
  protected TickRateModulation doBusWork() {
    if( !this.getProxy().isActive() || !this.canDoBusWork() )
    {
      return TickRateModulation.IDLE;
    }
    return TickRateModulation.SLOWER;
  }

  @Override
  public void getBoxes( final IPartCollisionHelper bch )
  {
    bch.addBox( 5, 5, 12, 11, 11, 13 );
    bch.addBox( 3, 3, 13, 13, 13, 14 );
    bch.addBox( 2, 2, 14, 14, 14, 16 );
  }

  @Override
  public float getCableConnectionLength( AECableType cable )
  {
    return 1;
  }

  @Nonnull
  @Override
  public IPartModel getStaticModels()
  {
    if( this.isActive() && this.isPowered() )
    {
      return MODELS_HAS_CHANNEL;
    }
    else if( this.isPowered() )
    {
      return MODELS_ON;
    }
    else
    {
      return MODELS_OFF;
    }
  }

  @Override
  @SuppressWarnings("null")
  public boolean onPartActivate(final EntityPlayer player, final EnumHand hand, final Vec3d posIn ) {
    if (Platform.isServer()) {
      if (player.getHeldItem(hand).getItem() instanceof FakeItem) {
        ItemStack is = player.getHeldItem(hand);
        if (!is.hasTagCompound()) {
          is.setTagCompound(new NBTTagCompound());
        }
        assert is.getTagCompound() != null; // Remove NPE warning
        is.getTagCompound().setString("__pkgDest", satPartName);
      } else {
        BlockPos pos = getTile().getPos();
        // Send the result id when opening gui
        final ModernPacket packet = PacketHandler.getPacket(TB_SyncNamePacket.class).setSide(this.getSide().ordinal()).setString(this.satPartName).setTilePos(this.getTile());
        MainProxy.sendPacketToPlayer(packet, player);
        player.openGui(TestBridge.INSTANCE, GuiIDs.GUI_SatelliteBus_ID + this.getSide().ordinal(), this.getTile().getWorld(), pos.getX(), pos.getY(), pos.getZ());
      }
    }
    return true;
  }

  @Override
  public TileEntity getContainer() {
    return this.getTile();
  }

  public EnumFacing getTargets() {
    return this.getSide().getFacing();
  }

  @Override
  public List<ItemIdentifierStack> getItemList() {
    return new LinkedList<>();
  }

  @Override
  public Set<SatellitePipe> getSatellitesOfType() {
    return Collections.unmodifiableSet(AllSatellites);
  }

  @Override
  public String getSatellitePipeName() {
    return this.satPartName;
  }

  @Override
  public void setSatellitePipeName(String __satName) {
    this.satPartName = __satName;
  }

  @Override
  public void updateWatchers() {
    CoordinatesPacket packet = PacketHandler.getPacket(TB_SyncNamePacket.class).setSide(this.getSide().ordinal()).setString(this.satPartName).setTilePos(this.getTile());
    MainProxy.sendToPlayerList(packet, this.localModeWatchers);
    MainProxy.sendPacketToAllWatchingChunk(this.getTile(), packet);
  }

  @Override
  public void ensureAllSatelliteStatus() {
    if (this.satPartName.isEmpty()) {
      PartSatelliteBus.AllSatellites.remove(this);
    }
    if (!this.satPartName.isEmpty()) {
      PartSatelliteBus.AllSatellites.add(this);
    }
  }

  public TileEntity getNeighborTE() {
    // Nested nightmare =))
    return this.getTile().getWorld().getTileEntity(this.getTile().getPos().offset(this.getTargets()));
  }

  public InventoryAdaptor getInvAdaptor() {
    return InventoryAdaptor.getAdaptor(this.getNeighborTE(), this.getTargets().getOpposite());
  }

  @Override
  public NBTTagCompound downloadSettings(SettingsFrom from) {
    NBTTagCompound output = new NBTTagCompound();
    if (!satPartName.isEmpty())
      output.setString("_satName", getSatellitePipeName());
    return output;
  }

  @Override
  public void uploadSettings(SettingsFrom from, NBTTagCompound compound, EntityPlayer player) {
    if (compound.hasKey("_satName")) {
      String newName = compound.getString("_satName");
      if (this.getSatellitesOfType().stream().anyMatch(it -> it.getSatellitePipeName().equals(newName))) {
        sendStatus(player, "Duplicated");
      } else {
        sendStatus(player, "Success");
        this.setSatellitePipeName(newName);
        this.getTile().markDirty();
      }
    }
  }

  private void sendStatus(EntityPlayer player, String result) {
    if (Platform.isClient()) {
      return;
    }

    switch (result){
      case "Duplicated":
        player.sendStatusMessage(new TextComponentString(TextUtil.translate("chat.testbridge.satellite_bus.duplicated")), true);
        break;
      case "Success":
        player.sendStatusMessage(new TextComponentString(TextUtil.translate("chat.testbridge.satellite_bus.success")), true);
        break;
      default:
    }
  }
}
