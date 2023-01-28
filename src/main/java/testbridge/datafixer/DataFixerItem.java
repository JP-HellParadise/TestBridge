package testbridge.datafixer;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.datafix.IFixableData;

import javax.annotation.Nonnull;

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
    if (compound.getString("id").contains("lb.package")) {
      NBTTagCompound oldNBT = compound.getCompoundTag("tag");
      NBTTagCompound newNBT = new NBTTagCompound();
      newNBT.setBoolean("__actContainer", oldNBT.getBoolean("__actStack"));
      newNBT.setString("__pkgDest", oldNBT.getString("__pkgDest"));
      if (!new ItemStack(oldNBT).isEmpty())
        newNBT.setTag("__itemHold", new ItemStack(oldNBT).writeToNBT(new NBTTagCompound()));
      compound.setTag("tag", newNBT);
    }
    return compound;
  }
}
