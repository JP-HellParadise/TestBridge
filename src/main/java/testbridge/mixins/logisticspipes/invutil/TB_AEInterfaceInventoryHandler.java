package testbridge.mixins.logisticspipes.invutil;

import java.util.Iterator;
import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.IGridHost;
import appeng.api.storage.IStorageMonitorable;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.IStorageMonitorableAccessor;

import logisticspipes.proxy.specialinventoryhandler.AEInterfaceInventoryHandler;
import network.rs485.logisticspipes.inventory.ProviderMode;

import testbridge.helpers.TBActionSource;
import testbridge.helpers.interfaces.TB_IInventoryUtil;

@Mixin(value = AEInterfaceInventoryHandler.class, remap = false)
public abstract class TB_AEInterfaceInventoryHandler implements TB_IInventoryUtil {
  @Shadow(remap = false)
  private IStorageMonitorableAccessor acc;

  @Shadow(remap = false)
  IGridHost host;

  @Unique
  private TBActionSource tb$source;

  @Inject(
      method = "<init>(Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/EnumFacing;Lnetwork/rs485/logisticspipes/inventory/ProviderMode;)V",
      at = @At(value = "RETURN"),
      remap = false)
  private void newTBActionSource(TileEntity tile, EnumFacing dir, ProviderMode mode, CallbackInfo ci) {
    this.tb$source = new TBActionSource(host);
  }

  @Override
  public boolean roomForItem(@Nonnull Iterator<ItemStack> iterator) {
    while (iterator.hasNext()){
      ItemStack itemStack = iterator.next();
      IItemStorageChannel channel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
      IStorageMonitorable tmp = acc.getInventory(tb$source);
      if (tmp == null || tmp.getInventory(channel) == null) {
        return false;
      }
      IAEItemStack stack = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createStack(itemStack);
      if (stack == null) return false;
      while (stack.getStackSize() > 0) {
        if (tmp.getInventory(channel).injectItems(stack, Actionable.SIMULATE, tb$source) != null) {
          return false;
        }
        stack.decStackSize(1);
      }
    }
    return true;
  }

}
