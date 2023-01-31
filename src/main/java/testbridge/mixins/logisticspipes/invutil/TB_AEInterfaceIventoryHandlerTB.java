package testbridge.mixins.logisticspipes.invutil;

import java.util.Iterator;
import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import appeng.api.AEApi;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IStorageMonitorable;
import appeng.api.storage.IStorageMonitorableAccessor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;

import testbridge.helpers.interfaces.TB_IIventoryUtil;

@Mixin(targets = "logisticspipes/proxy/specialinventoryhandler/AEInterfaceInventoryHandler", remap = false)
public abstract class TB_AEInterfaceIventoryHandlerTB implements TB_IIventoryUtil {
  @Shadow(aliases = "source", remap = false)
  private Object source;

  @Shadow(remap = false)
  private IStorageMonitorableAccessor acc;

  @Override
  public boolean roomForItem(@Nonnull Iterator<ItemStack> iterator) {
    while (iterator.hasNext()){
      ItemStack itemStack = iterator.next();
      IItemStorageChannel channel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
      IStorageMonitorable tmp = acc.getInventory((IActionSource) source);
      if (tmp == null || tmp.getInventory(channel) == null) {
        return false;
      }
      IAEItemStack stack = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createStack(itemStack);
      if (stack == null) return false;
      while (stack.getStackSize() > 0) {
        if (tmp.getInventory(channel).canAccept(stack)) {
          return true;
        }
        stack.decStackSize(1);
      }
    }
    return false;
  }

}
