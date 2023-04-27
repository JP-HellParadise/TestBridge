package net.jp.hellparadise.testbridge.items;

import java.util.List;

import javax.annotation.Nonnull;

import net.jp.hellparadise.testbridge.core.TB_ItemHandlers;
import net.jp.hellparadise.testbridge.core.TestBridge;
import net.jp.hellparadise.testbridge.helpers.NBTItemHelper;
import net.jp.hellparadise.testbridge.network.GuiIDs;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.util.Platform;

public class FakeItem extends Item {

    private final boolean isPackage;

    public FakeItem(boolean isPackage) {
        this.isPackage = isPackage;
        this.setMaxStackSize(64);
    }

    @Nonnull
    @Override
    public ActionResult<ItemStack> onItemRightClick(@Nonnull final World w, @Nonnull EntityPlayer player,
        @Nonnull EnumHand hand) {
        if (isPackage) {
            ItemStack is = player.getHeldItem(hand);
            final boolean isHolder = is.hasTagCompound() && is.getTagCompound()
                .getBoolean("__actContainer");
            if (isHolder && !w.isRemote
                && !NBTItemHelper.NBTHelper.getItemStack(is, true, true)
                    .isEmpty()) {
                if (player.isSneaking()) {
                    for (int i = 0; i < is.getCount(); i++) {
                        w.spawnEntity(
                            new EntityItem(
                                w,
                                player.posX,
                                player.posY,
                                player.posZ,
                                NBTItemHelper.NBTHelper.getItemStack(is, true, true)
                                    .copy()));
                    }
                    is.shrink(is.getCount());
                } else {
                    w.spawnEntity(
                        new EntityItem(
                            w,
                            player.posX,
                            player.posY,
                            player.posZ,
                            NBTItemHelper.NBTHelper.getItemStack(is, true, true)
                                .copy()));
                    is.shrink(1);
                }
                return new ActionResult<>(EnumActionResult.SUCCESS, is);
            } else if (player.isSneaking()) {
                this.clearPackage(is, player);
                return new ActionResult<>(EnumActionResult.SUCCESS, is);
            } else if (!isHolder) {
                player.openGui(TestBridge.INSTANCE, GuiIDs.TEMPLATE_PKG.ordinal(), w, hand.ordinal(), 0, 0);
                return new ActionResult<>(EnumActionResult.SUCCESS, is);
            }
        }
        return super.onItemRightClick(w, player, hand);
    }

    @Nonnull
    @Override
    public EnumActionResult onItemUseFirst(final EntityPlayer player, final World world, final BlockPos pos,
        final EnumFacing side, final float hitX, final float hitY, final float hitZ, final EnumHand hand) {
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

        ItemStack is = new ItemStack(TB_ItemHandlers.itemPackage, item.getCount());
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

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(final ItemStack stack, final World world, final List<String> tooltip,
        final ITooltipFlag advancedTooltips) {
        if (isPackage) {
            if (stack.hasTagCompound()) {
                if (!NBTItemHelper.NBTHelper.getItemStack(stack, false, true)
                    .isEmpty()) {
                    if (stack.getTagCompound()
                        .getBoolean("__actContainer")) {
                        tooltip.add(
                            I18n.format(
                                "tooltip.testbridge.placeholder",
                                NBTItemHelper.NBTHelper.getItemInfo(stack, NBTItemHelper.ItemInfo.FULL_INFO)));
                        tooltip.add(I18n.format("tooltip.testbridge.placeholder.rightclick"));
                    } else tooltip.add(
                        I18n.format(
                            "tooltip.testbridge.package_content",
                            NBTItemHelper.NBTHelper.getItemInfo(stack, NBTItemHelper.ItemInfo.FULL_INFO)));
                }
                String name = NBTItemHelper.NBTHelper.getItemInfo(stack, NBTItemHelper.ItemInfo.DESTINATION);
                if (!name.isEmpty()) tooltip.add(I18n.format("tooltip.testbridge.satName", name));
                if (tooltip.size() < 2) tooltip.add(I18n.format("tooltip.testbridge.package_empty"));
            }
        } else {
            if (stack.hasTagCompound()) tooltip.add(
                I18n.format(
                    "tooltip.testbridge.request",
                    NBTItemHelper.NBTHelper.getItemInfo(stack, NBTItemHelper.ItemInfo.FULL_INFO)));
            else tooltip.add(I18n.format("tooltip.testbridge.fakeItemNull"));
            tooltip.add(I18n.format("tooltip.testbridge.techItem"));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (isPackage && stack.hasTagCompound()) {
            String satName = stack.getTagCompound()
                .getString("__pkgDest");
            if (!satName.equals("") && !(NBTItemHelper.NBTHelper.getItemInfo(stack, NBTItemHelper.ItemInfo.FULL_INFO)
                .isEmpty())) {
                return net.minecraft.util.text.translation.I18n.translateToLocalFormatted(
                    "tooltip.testbridge.packageName",
                    NBTItemHelper.NBTHelper.getItemInfo(stack, NBTItemHelper.ItemInfo.FULL_INFO),
                    satName);
            }
        }
        return super.getItemStackDisplayName(stack);
    }
}
