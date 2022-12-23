package testbridge.utils;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import appeng.util.Platform;

public class NBTItemHelper {
  public static NBTItemHelper NBTHelper = new NBTItemHelper();

  public ItemStack getItemStack(ItemStack is) {
    return is.hasTagCompound() && is.getTagCompound().hasKey("__itemHold") ? new ItemStack(is.getTagCompound().getCompoundTag("__itemHold")) : new ItemStack(Items.AIR);
  }

  public String getItemInfo(ItemStack is, String info) {
    if (getItemStack(is).getItem() == Items.AIR) return "";
    else {
      ItemStack item = getItemStack(is);
      switch (info) {
        case "destination":
          return getDestination(is);
        case "name":
          return Platform.getItemDisplayName(item);
        case "count":
          return Integer.toString(item.getCount());
        default:
          return item.getCount() + " " + Platform.getItemDisplayName(item);
      }
    }
  }

  public String getDestination(ItemStack is) {
    return is.hasTagCompound() && is.getTagCompound().hasKey("__pkgDest") ? is.getTagCompound().getString("__pkgDest") : "";
  }
}
