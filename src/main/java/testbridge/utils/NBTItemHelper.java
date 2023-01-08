package testbridge.utils;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import appeng.util.Platform;

public class NBTItemHelper {
  public static NBTItemHelper NBTHelper = new NBTItemHelper();

  /** Get ItemStack
   * @param is ItemStack contains "__itemHold" tag
   * @param checkContainer Check if Package is Item Holder
   * @param singleStack Get definition of ItemStack
   * @return Stored ItemStack with correct count
   */
  public final ItemStack getItemStack(final ItemStack is, final boolean checkContainer, final boolean singleStack) {
    if (is.getTagCompound() != null && is.getTagCompound().hasKey("__itemHold") && (!checkContainer || is.getTagCompound().getBoolean("__actContainer"))) {
      final ItemStack holder = new ItemStack(is.getTagCompound().getCompoundTag("__itemHold"));
      if (!singleStack) holder.setCount(holder.getCount() * is.getCount());
      return holder;
    }
    return new ItemStack(Items.AIR);
  }

  public final String getItemInfo(final ItemStack is, final ItemInfo info) {
    if (this.getItemStack(is, false, true).isEmpty()) return "";
    final ItemStack item = this.getItemStack(is, false, true);
    switch (info) {
      case DESTINATION:
        return this.getDestination(is);
      case DISPLAY_NAME:
        return Platform.getItemDisplayName(item);
      case ITEM_COUNT:
        return Integer.toString(item.getCount());
      default:
        return item.getCount() + " " + Platform.getItemDisplayName(item);
    }
  }

  public final String getDestination(ItemStack is) {
    return is.getTagCompound() != null ? is.getTagCompound().getString("__pkgDest") : "";
  }

  public enum ItemInfo {
    DESTINATION,
    DISPLAY_NAME,
    ITEM_COUNT,
    FULL_INFO
  }
}
