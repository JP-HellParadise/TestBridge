package testbridge.mixins.logisticspipes.invutil;

import java.util.Iterator;
import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import logisticspipes.utils.InventoryUtil;

import testbridge.helpers.interfaces.TB_IInventoryUtil;

@Mixin(value = InventoryUtil.class, remap = false)
public abstract class TB_InventoryUtil implements TB_IInventoryUtil {
  @Shadow(remap = false)
  @Final
  protected IItemHandler inventory;

  @Override
  public boolean roomForItem(@Nonnull Iterator<ItemStack> iterator) {
    int slot = 0;
    while (iterator.hasNext()) {
      ItemStack toBeSimulated = iterator.next();
      if (!toBeSimulated.isEmpty()) {
        while (slot < this.inventory.getSlots()) {
          toBeSimulated = this.inventory.insertItem(slot, toBeSimulated, true);
          if (slot == this.inventory.getSlots() - 1) {
            return !iterator.hasNext() && toBeSimulated.isEmpty();
          }

          // Always increase slot by default
          slot++;

          if (toBeSimulated.isEmpty()) {
            break;
          }
        }
      }
    }

    return true;
  }
}
