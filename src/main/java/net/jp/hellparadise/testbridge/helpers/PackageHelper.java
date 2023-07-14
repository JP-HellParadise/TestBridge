package net.jp.hellparadise.testbridge.helpers;

import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.CapabilityItemHandler;

public class PackageHelper {

    /**
     * Get ItemStack
     *
     * @param itemStack    Storage container (Ex: Package)
     * @param isHolder     Check if the original stack is a Placeholder
     * @param isDefinition Get ItemStack as definition or not
     * @return ItemStack that stored inside original
     */
    public static ItemStack getItemStack(@Nonnull ItemStack itemStack, boolean isHolder, boolean isDefinition) {
        final ItemStack heldStack = itemStack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)
            .getStackInSlot(0)
            .copy();
        if (!heldStack.isEmpty() && (!isHolder || (itemStack.hasTagCompound() && itemStack.getTagCompound()
            .getBoolean("__actContainer")))) {
            if (isDefinition) heldStack.setCount(1);
            return heldStack;
        }
        return ItemStack.EMPTY;
    }

    public static String getItemInfo(@Nonnull ItemStack itemStack, @Nonnull ItemInfo info) {
        if (info == ItemInfo.DESTINATION) {
            return getDestination(itemStack);
        }

        final ItemStack heldItem = getItemStack(itemStack, false, false);
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
