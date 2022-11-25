package testbridge.mixins.logisticspipes.modules;

import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.request.resources.IResource;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.order.LogisticsItemOrder;
import logisticspipes.utils.item.ItemIdentifier;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.inventory.IItemIdentifierInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.lang.ref.WeakReference;

@Mixin(ModuleCrafter.class)
public interface TB_AccessorModuleCrafter {
  @Invoker(value = "getSatelliteRouter")
  IRouter getSatelliteRouter(int x);

  @Invoker(value = "getFluidSatelliteRouter")
  IRouter getFluidSatelliteRouter(int x);

  @Invoker(value = "extractFiltered")
  ItemStack extractFiltered(NeighborTileEntity<TileEntity> neighbor, IItemIdentifierInventory inv, boolean isExcluded, int filterInvLimit);

  @Invoker(value = "extract")
  ItemStack extract(NeighborTileEntity<TileEntity> adjacent, IResource item, int amount);

  @Invoker(value = "isExtractedMismatch")
  boolean isExtractedMismatch(LogisticsItemOrder nextOrder, ItemIdentifier extractedID);

  @Accessor
  WeakReference<TileEntity> getLastAccessedCrafter();

  @Accessor
  void setLastAccessedCrafter(WeakReference<TileEntity> lastAccessedCrafter);

  @Accessor
  void setCachedAreAllOrderesToBuffer(boolean input);

  @Accessor(value = "_invRequester")
  void setInvRequester(IRequestItems _invRequester);
}
