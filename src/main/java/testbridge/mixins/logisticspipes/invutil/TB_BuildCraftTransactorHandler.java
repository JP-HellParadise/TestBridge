package testbridge.mixins.logisticspipes.invutil;

import java.util.Iterator;
import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import buildcraft.api.inventory.IItemTransactor;

import testbridge.helpers.interfaces.TB_IIventoryUtil;

@Mixin(targets = "logisticspipes/proxy/specialinventoryhandler/BuildCraftTransactorHandler", remap = false)
public abstract class TB_BuildCraftTransactorHandler implements TB_IIventoryUtil {
  @Shadow(remap = false)
  private IItemTransactor cap;

  @Override
  public boolean roomForItem(@Nonnull Iterator<ItemStack> iterator) {
    while (iterator.hasNext()) {
      ItemStack stack = iterator.next();
      if (cap.insert(stack, false, true).getCount() != 0) return false;
    }
    return true;
  }
}
