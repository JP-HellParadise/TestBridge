package net.jp.hellparadise.testbridge.helpers;

import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

public class PackageHelper {

    public static final String KEY_ITEMS = "Items";

    /**
     * Get ItemStack
     *
     * @param itemStack    Storage container (Ex: Package)
     * @param isHolder     Check if the original stack is a Placeholder
     * @return ItemStack that stored inside original
     */
    public static ItemStack getItemStack(@Nonnull ItemStack itemStack, boolean isHolder) {
        NBTBase item = getItemsNbt(itemStack).get(0);
        return (item instanceof NBTTagCompound && !item.isEmpty() && (!isHolder || (itemStack.hasTagCompound() && itemStack.getTagCompound()
                .getBoolean("__actContainer")))) ? new ItemStack((NBTTagCompound) item) : ItemStack.EMPTY;
    }

    public static NBTTagList getItemsNbt(@Nonnull ItemStack itemStack) {
        NBTTagCompound nbt = itemStack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            itemStack.setTagCompound(nbt);
        }
        if (!nbt.hasKey(KEY_ITEMS)) {
            NBTTagList list = new NBTTagList();
            list.appendTag(new NBTTagCompound());
            nbt.setTag(KEY_ITEMS, list);
        }
        return nbt.getTagList(KEY_ITEMS, Constants.NBT.TAG_COMPOUND);
    }

    public static String getItemInfo(@Nonnull ItemStack itemStack, @Nonnull ItemInfo info) {
        if (info == ItemInfo.DESTINATION) {
            return getDestination(itemStack);
        }

        final ItemStack heldItem = getItemStack(itemStack, false);
        if (heldItem.isEmpty()) return "";
        switch (info) {
            case DISPLAY_NAME:
                return getItemDisplayName(heldItem);
            case ITEM_COUNT:
                return Integer.toString(heldItem.getCount());
            default:
                return getItemDisplayInfo(heldItem);
        }
    }

    private static String getItemDisplayName(@Nonnull ItemStack itemStack) {
        try {
            String displayName = itemStack.getDisplayName();
            return !displayName.isEmpty() ? displayName
                : itemStack.getItem()
                    .getTranslationKey(itemStack);
        } catch (Exception var5) {
            try {
                return itemStack.getTranslationKey();
            } catch (Exception var4) {
                return "** Exception";
            }
        }
    }

    private static String getItemDisplayInfo(@Nonnull ItemStack itemStack) {
        return itemStack.getCount() + " " + getItemDisplayName(itemStack);
    }

    private static String getDestination(ItemStack itemStack) {
        return itemStack.getTagCompound() != null ? itemStack.getTagCompound()
            .getString("__pkgDest") : "";
    }

    public static void setDestination(ItemStack itemStack, String destination) {
        if (!itemStack.hasTagCompound()) itemStack.setTagCompound(new NBTTagCompound());
        itemStack.getTagCompound()
            .setString("__pkgDest", destination);
    }

    public enum ItemInfo {
        DESTINATION,
        DISPLAY_NAME,
        ITEM_COUNT,
        FULL_INFO
    }
}
