package testbridge.mixins.logisticspipes.invutil;

import java.util.List;
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

  // Handle simple inv check for item list
  @Override
  public boolean roomForItem(@Nonnull List<ItemStack> list) {
    int slot = 0;

    for (ItemStack item : list) {
      if (item.isEmpty()) {
        continue;
      }

      while (!item.isEmpty()) {
        item = this.inventory.insertItem(slot, item, true);

        slot = (slot + 1) % this.inventory.getSlots();

        if (slot == 0) {
          return false;
        }
      }
    }

    return true;
  }
}
