package net.jp.hellparadise.testbridge.datafixer;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.datafix.IFixableData;

public class DataFixerTE implements IFixableData {

    public static final FixTypes TYPE = FixTypes.BLOCK_ENTITY;
    public static final int VERSION = 0;

    @Override
    public int getFixVersion() {
        return VERSION;
    }

    @Nonnull
    @Override
    public NBTTagCompound fixTagCompound(NBTTagCompound compound) {
        if (compound.getString("id")
            .equalsIgnoreCase("logisticspipes:pipe")) {
            return fixPipe(compound);
        }

        if (compound.getString("id")
            .equalsIgnoreCase("logisticsbridge:craftingmanagerae")) {
            return fixBlockCraftingMng(compound);
        }

        return compound;
    }

    private NBTTagCompound fixPipe(final NBTTagCompound compound) {
        if (compound.hasKey("chassiitems") && compound.hasKey("satellitename") && compound.hasKey("resultname")) { // Crafting
                                                                                                                   // Manager
                                                                                                                   // pipe
            // Blocking mode
            final byte blockingMode = compound.getByte("blockingMode");
            compound.removeTag("blockingMode");
            compound.setInteger("blockingMode", blockingMode < 2 ? 0 : blockingMode - 1);

            compound.setTag("craftingmanageritems", compound.getTagList("chassiitems", 10));

            compound.removeTag("chassiitems");
            compound.removeTag("chassiitemsCount");
        } else if (compound.hasKey("resultname")) { // Result pipe
            compound.setString("resultPipeName", compound.getString("resultname"));
            compound.removeTag("resultname");
        }
        return compound;
    }

    private NBTTagCompound fixBlockCraftingMng(final NBTTagCompound compound) {
        // Convert blocking mode to new setting
        if (compound.getByte("blockingMode") > 1) {
            compound.setString("BLOCK", "YES");
        } else compound.setString("BLOCK", "NO");
        compound.removeTag("blockingMode");

        // Convert satellite select
        compound.setString("mainSatName", compound.getString("supplyName"));
        compound.removeTag("supplyName");

        // Convert inventory
        final NBTTagCompound oldInv = compound.getCompoundTag("inv");
        compound.removeTag("int");

        final NBTTagList nbtTagList = new NBTTagList();
        for (int i = 0; i < oldInv.getSize(); ++i) {
            final ItemStack is = new ItemStack(oldInv.getCompoundTag("item" + i));
            if (!is.isEmpty()) {
                final NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setInteger("Slot", i);
                is.writeToNBT(itemTag);
                nbtTagList.appendTag(itemTag);
            }
        }

        final NBTTagCompound nbt = new NBTTagCompound();
        nbt.setTag("Items", nbtTagList);
        nbt.setInteger("Size", oldInv.getSize());

        compound.setTag("pattern", nbt);

        return compound;
    }
}
