package testbridge.items;

import java.util.List;

import lombok.Getter;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemAir;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.items.AEBaseItem;
import appeng.util.Platform;

import testbridge.core.TBItems;
import testbridge.core.TestBridge;
import testbridge.network.GuiIDs;

public class FakeItem extends AEBaseItem {
  @Getter
  private final boolean isPackage;

  private boolean displayOverride;

  public FakeItem(boolean isPackage) {
    this.isPackage = isPackage;
    this.setMaxStackSize(64);
  }

  @Override
  public ActionResult<ItemStack> onItemRightClick(final World w, EntityPlayer player, EnumHand hand) {
    ItemStack is = player.getHeldItem(hand);
    if (player.isSneaking()) {
      this.clearPackage(is, player);
      return new ActionResult<>(EnumActionResult.SUCCESS, is);
    }
    if (isPackage) {
      if(is.hasTagCompound() && is.getTagCompound().getBoolean("__actContainer")){
        if (!w.isRemote) {
          if (getItemStack(is) != null && !getItemStack(is).isEmpty()) {
            w.spawnEntity(new EntityItem(w, player.posX, player.posY, player.posZ, getItemStack(is).copy()));
          }
        }
        is.shrink(1);
        return new ActionResult<>(EnumActionResult.SUCCESS, is);
      } else {
        player.openGui(TestBridge.INSTANCE, GuiIDs.ENUM.TEMPLATE_PKG.ordinal(), w, hand.ordinal(), 0, 0);
        return new ActionResult<>(EnumActionResult.SUCCESS, is);
      }
    }
    return super.onItemRightClick(w, player, hand);
  }

  @Override
  public EnumActionResult onItemUseFirst(final EntityPlayer player, final World world, final BlockPos pos, final EnumFacing side, final float hitX, final float hitY, final float hitZ, final EnumHand hand) {
    if (!player.isSneaking()) {
      return EnumActionResult.PASS;
    }
    return this.clearPackage(player.getHeldItem(hand), player) ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
  }

  private boolean clearPackage(ItemStack item, EntityPlayer player) {
    if (Platform.isClient()) {
      return false;
    }

    final InventoryPlayer inv = player.inventory;

    ItemStack is = new ItemStack(TBItems.itemPackage, item.getCount());
    if (!is.isEmpty()) {
      for (int s = 0; s < player.inventory.getSizeInventory(); s++) {
        if (inv.getStackInSlot(s) == item) {
          inv.setInventorySlotContents(s, is);
          return true;
        }
      }
    }
    return false;
  }

  public ItemStack getItemStack(ItemStack is) {
    return is.hasTagCompound() && is.getTagCompound().hasKey("__itemHold") ? new ItemStack(is.getTagCompound().getCompoundTag("__itemHold")) : new ItemStack(Items.AIR);
  }

  public String getItemInfo(ItemStack is) {
    return getItemCount(is) + " " + getItemName(is);
  }

  private int getItemCount(ItemStack is) {
    return getItemStack(is).isEmpty() ? 0 : getItemStack(is).getCount();
  }

  private String getItemName(ItemStack is) {
    return getItemCount(is) == 0 ? "" : getItemStack(is).getItem().getItemStackDisplayName(is);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addCheckedInformation(final ItemStack stack, final World world, final List<String> tooltip, final ITooltipFlag advancedTooltips) {
    try {
      if (isPackage) {
        if (!stack.hasTagCompound())
          tooltip.add(I18n.format("tooltip.testbridge.package_empty"));
        else {
          displayOverride = true;
          if (!getItemName(stack).isEmpty()){
            if (stack.getTagCompound().getBoolean("__actContainer"))
              tooltip.add(I18n.format("tooltip.testbridge.placeholder", getItemInfo(stack)));
            else
              tooltip.add(I18n.format("tooltip.testbridge.package_content", getItemInfo(stack)));
          }
          displayOverride = false;
          String name = stack.getTagCompound().getString("__pkgDest");
          if (!name.isEmpty())
            tooltip.add(I18n.format("tooltip.testbridge.satName", name));
        }
      } else {
        if (stack.hasTagCompound())
          tooltip.add(I18n.format("tooltip.testbridge.request", getItemInfo(stack)));
        else
          tooltip.add(I18n.format("tooltip.testbridge.fakeItemNull"));
        tooltip.add(I18n.format("tooltip.testbridge.techItem"));
      }
    } catch (NullPointerException e) {
      tooltip.add(I18n.format("tooltip.testbridge.package_empty"));
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public String getItemStackDisplayName(ItemStack stack) {
    if(!displayOverride && isPackage && stack.hasTagCompound()){
      String name = stack.getTagCompound().getString("__pkgDest");
      if(!name.equals("") && !(getItemName(stack).isEmpty())){
        return net.minecraft.util.text.translation.I18n.translateToLocalFormatted("tooltip.testbridge.packageName",
            getItemInfo(stack), name);
      }
    }
    return super.getItemStackDisplayName(stack);
  }
}
