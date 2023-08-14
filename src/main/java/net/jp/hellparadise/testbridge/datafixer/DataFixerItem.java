package net.jp.hellparadise.testbridge.datafixer;

import javax.annotation.Nonnull;
import net.jp.hellparadise.testbridge.helpers.PackageHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.datafix.IFixableData;

public class DataFixerItem implements IFixableData {

    public static final FixTypes TYPE = FixTypes.ITEM_INSTANCE;
    public static final int VERSION = 1;

    @Override
    public int getFixVersion() {
        return VERSION;
    }

    @Nonnull
    @Override
    public NBTTagCompound fixTagCompound(NBTTagCompound compound) {
        // Package
        if (compound.getString("id")
            .contains("lb.package")) {
            NBTTagCompound oldNBT = compound.getCompoundTag("tag");
            NBTTagCompound newNBT = new NBTTagCompound();
            newNBT.setBoolean("__actContainer", oldNBT.getBoolean("__actStack"));
            newNBT.setString("__pkgDest", oldNBT.getString("__pkgDest"));
            if (!new ItemStack(oldNBT).isEmpty()) {
                NBTTagList list = new NBTTagList();
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setInteger("Slot", 0);
                new ItemStack(oldNBT).writeToNBT(itemTag);
                list.appendTag(itemTag);
                newNBT.setTag(PackageHelper.KEY_ITEMS, list);
            }
            compound.setTag("tag", newNBT);
        }
        return compound;
    }
}
