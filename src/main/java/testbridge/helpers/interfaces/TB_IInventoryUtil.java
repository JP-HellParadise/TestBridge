package testbridge.helpers.interfaces;

import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

public interface TB_IInventoryUtil {

  /**
   * Inventory space count which terminates when space for max items are
   * found.
   *
   * @return spaces found. If this is less than max, then there are no
   * spaces for that amount.
   * @param list ItemStack list, what you expect?
   */
  boolean roomForItem(@Nonnull List<ItemStack> list);
}
