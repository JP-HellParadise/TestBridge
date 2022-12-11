package testbridge.part;

import javax.annotation.Nonnull;
import java.util.*;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartModel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.core.AELog;
import appeng.core.settings.TickRates;
import appeng.helpers.Reflected;
import appeng.items.parts.PartModels;
import appeng.me.GridAccessException;
import appeng.parts.PartModel;
import appeng.parts.automation.PartSharedItemBus;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifierStack;

import network.rs485.logisticspipes.SatellitePipe;

import testbridge.core.TestBridge;
import testbridge.items.FakeItem;
import testbridge.network.GuiIDs;
import testbridge.network.packets.resultpackethandler.TB_SyncNamePacket;

public class PartSatelliteBus extends PartSharedItemBus implements SatellitePipe {

  public static final ResourceLocation MODEL_BASE = new ResourceLocation( TestBridge.ID, "part/satellite_bus_base" );
  @PartModels
  public static final IPartModel MODELS_OFF = new PartModel( MODEL_BASE, new ResourceLocation( TestBridge.ID, "part/satellite_bus_off" ) );
  @PartModels
  public static final IPartModel MODELS_ON = new PartModel( MODEL_BASE, new ResourceLocation( TestBridge.ID, "part/satellite_bus_on" ) );
  @PartModels
  public static final IPartModel MODELS_HAS_CHANNEL = new PartModel( MODEL_BASE, new ResourceLocation( TestBridge.ID, "part/satellite_bus_has_channel" ) );

  public static final Set<PartSatelliteBus> AllSatellites = Collections.newSetFromMap(new WeakHashMap<>());
  public final PlayerCollectionList localModeWatchers = new PlayerCollectionList();
  private String satelliteBusName = "";

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
    extra.setString("satelliteBusName", this.satelliteBusName);
  }

  @Override
  public void readFromNBT(final NBTTagCompound extra) {
    super.readFromNBT(extra);
    this.satelliteBusName = extra.getString("satelliteBusName");

    if (MainProxy.isServer(getTile().getWorld())) {
      ensureAllSatelliteStatus();
    }
  }

  @Nonnull
  @Override
  public TickingRequest getTickingRequest(@Nonnull final IGridNode node ) {
    return new TickingRequest( TickRates.ExportBus.getMin(), TickRates.ExportBus.getMax(), this.isSleeping(), false );
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

  private IAEItemStack injectCraftedItems( final IAEItemStack items, final Actionable mode ) {
    final InventoryAdaptor d = this.getHandler();

    try
    {
      if( d != null && this.getProxy().isActive() )
      {
        final IEnergyGrid energy = this.getProxy().getEnergy();
        final double power = items.getStackSize();

        if( energy.extractAEPower( power, mode, PowerMultiplier.CONFIG ) > power - 0.01 )
        {
          if( mode == Actionable.MODULATE )
          {
            return AEItemStack.fromItemStack( d.addItems( items.createItemStack() ) );
          }
          return AEItemStack.fromItemStack( d.simulateAdd( items.createItemStack() ) );
        }
      }
    }
    catch( final GridAccessException e )
    {
      AELog.debug( e );
    }

    return items;
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
  public boolean onPartActivate(final EntityPlayer player, final EnumHand hand, final Vec3d posIn ) {
    if (Platform.isServer()) {
      if (player.getHeldItem(hand).getItem() instanceof FakeItem) {
        ItemStack is = player.getHeldItem(hand);
        if (!is.hasTagCompound()) {
          is.setTagCompound(new NBTTagCompound());
        }
        is.getTagCompound().setString("__pkgDest", satelliteBusName);
      } else {
        BlockPos pos = getTile().getPos();
        // Send the result id when opening gui
        final ModernPacket packet = PacketHandler.getPacket(TB_SyncNamePacket.class).setSide(getSide().ordinal()).setString(satelliteBusName).setTilePos(getTile());
        MainProxy.sendPacketToPlayer(packet, player);
        player.openGui(TestBridge.INSTANCE, GuiIDs.GUI_SatelliteBus_ID + getSide().ordinal(), getTile().getWorld(), pos.getX(), pos.getY(), pos.getZ());
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
    return satelliteBusName;
  }

  @Override
  public void setSatellitePipeName(String satelliteName) {
    this.satelliteBusName = satelliteName;
  }

  @Override
  public void updateWatchers() {
    CoordinatesPacket packet = PacketHandler.getPacket(TB_SyncNamePacket.class).setSide(getSide().ordinal()).setString(satelliteBusName).setTilePos(getTile());
    MainProxy.sendToPlayerList(packet, localModeWatchers);
    MainProxy.sendPacketToAllWatchingChunk(getTile(), packet);
  }

  @Override
  public void ensureAllSatelliteStatus() {
    if (satelliteBusName.isEmpty()) {
      PartSatelliteBus.AllSatellites.remove(this);
    }
    if (!satelliteBusName.isEmpty()) {
      PartSatelliteBus.AllSatellites.add(this);
    }
  }
}
