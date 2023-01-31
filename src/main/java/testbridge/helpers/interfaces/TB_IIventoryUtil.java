package testbridge.helpers.interfaces;

import java.util.Iterator;
import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

public interface TB_IIventoryUtil {

  /**
   * Inventory space count which terminates when space for max items are
   * found.
   *
   * @return spaces found. If this is less than max, then there are no
   * spaces for that amount.
   * @param iterator
   */
  boolean roomForItem(@Nonnull Iterator<ItemStack> iterator);
}
