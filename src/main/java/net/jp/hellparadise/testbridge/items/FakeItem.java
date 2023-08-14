package net.jp.hellparadise.testbridge.items;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.layout.CrossAxisAlignment;
import com.cleanroommc.modularui.manager.GuiCreationContext;
import com.cleanroommc.modularui.manager.GuiInfos;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.ItemStackItemHandler;
import com.cleanroommc.modularui.value.sync.GuiSyncManager;
import com.cleanroommc.modularui.value.sync.SyncHandlers;
import com.cleanroommc.modularui.widgets.ItemSlot;
import com.cleanroommc.modularui.widgets.layout.Column;
import com.cleanroommc.modularui.widgets.layout.Row;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import java.util.List;
import javax.annotation.Nonnull;
import net.jp.hellparadise.testbridge.core.TB_ItemHandlers;
import net.jp.hellparadise.testbridge.helpers.PackageHelper;
import net.jp.hellparadise.testbridge.utils.TextUtil;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
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

public class FakeItem extends Item implements IGuiHolder {
    public static final Item ITEM_HOLDER = TB_ItemHandlers.createItem(new FakeItem(false), "placeholder", "", null);
    public static final Item ITEM_PACKAGE = TB_ItemHandlers.createItem(new FakeItem(true), "package", "", CreativeTabs.MISC);

    private final boolean isPackage;

    public FakeItem(boolean isPackage) {
        this.isPackage = isPackage;
        this.setMaxStackSize(64);
    }

    @Nonnull
    @Override
    public ActionResult<ItemStack> onItemRightClick(@Nonnull World world, @Nonnull EntityPlayer player,
        @Nonnull EnumHand hand) {
        if (isPackage && !world.isRemote) {
            ItemStack is = player.getHeldItem(hand);
            if (this.openPackage(world, player, is)) {
                return new ActionResult<>(EnumActionResult.SUCCESS, is);
            } else if (this.clearPackage(is, player)) {
                return new ActionResult<>(EnumActionResult.SUCCESS, is);
            } else {
                GuiInfos.getForHand(hand)
                    .open(player);
                return new ActionResult<>(EnumActionResult.SUCCESS, is);
            }
        }
        return super.onItemRightClick(world, player, hand);
    }

    @Nonnull
    @Override
    public EnumActionResult onItemUseFirst(@Nonnull final EntityPlayer player, @Nonnull final World world,
        @Nonnull final BlockPos pos, @Nonnull final EnumFacing side, final float hitX, final float hitY,
        final float hitZ, @Nonnull final EnumHand hand) {
        return this.clearPackage(player.getHeldItem(hand), player) ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
    }

    private boolean clearPackage(ItemStack is, EntityPlayer player) {
        if (!player.isSneaking() ||
                (is.hasTagCompound() && is.getTagCompound().getBoolean("__actContainer"))) {
            return false;
        }

        final InventoryPlayer inv = player.inventory;

        ItemStack newIs = new ItemStack(FakeItem.ITEM_PACKAGE, is.getCount());
        if (!newIs.isEmpty()) {
            for (int s = 0; s < player.inventory.getSizeInventory(); s++) {
                if (inv.getStackInSlot(s) == newIs) {
                    inv.setInventorySlotContents(s, newIs);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean openPackage(@Nonnull final World w, @Nonnull EntityPlayer player, @Nonnull ItemStack is) {
        ItemStack heldItem = PackageHelper.getItemStack(is, true);
        if (heldItem.isEmpty()) {
            return false;
        }

        int openAmount = player.isSneaking() ? is.getCount() : 1;

        for (int i = 0; i < openAmount; i++) {
            w.spawnEntity(new EntityItem(w, player.posX, player.posY, player.posZ, heldItem.copy()));
        }

        is.shrink(openAmount);
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull final ItemStack itemStack, final World world,
        @Nonnull final List<String> tooltip, @Nonnull final ITooltipFlag advancedTooltips) {
        if (isPackage) {
            if (itemStack.hasTagCompound()) {
                String itemInfo = PackageHelper.getItemInfo(itemStack, PackageHelper.ItemInfo.FULL_INFO);
                if (!itemInfo.isEmpty()) {
                    if (itemStack.getTagCompound()
                        .getBoolean("__actContainer")) {
                        tooltip.add(I18n.format("tooltip.testbridge.placeholder", itemInfo));
                        tooltip.add(I18n.format("tooltip.testbridge.placeholder.rightclick"));
                    } else tooltip.add(I18n.format("tooltip.testbridge.package_content", itemInfo));
                }

                String dest = PackageHelper.getItemInfo(itemStack, PackageHelper.ItemInfo.DESTINATION);
                if (!dest.isEmpty()) tooltip.add(I18n.format("tooltip.testbridge.satName", dest));

                if (tooltip.size() < 2) tooltip.add(I18n.format("tooltip.testbridge.package_empty"));
            }
        } else {
            if (itemStack.hasTagCompound()) tooltip.add(
                I18n.format(
                    "tooltip.testbridge.request",
                    PackageHelper.getItemInfo(itemStack, PackageHelper.ItemInfo.FULL_INFO)));
            else tooltip.add(I18n.format("tooltip.testbridge.fakeItemNull"));
            tooltip.add(I18n.format("tooltip.testbridge.techItem"));
        }
    }

    @Nonnull
    @Override
    public String getItemStackDisplayName(@Nonnull ItemStack itemStack) {
        if (isPackage && itemStack.hasTagCompound()) {
            String satName = PackageHelper.getItemInfo(itemStack, PackageHelper.ItemInfo.DESTINATION);
            String itemInfo = PackageHelper.getItemInfo(itemStack, PackageHelper.ItemInfo.FULL_INFO);
            if (!satName.isEmpty() && !itemInfo.isEmpty()) {
                return TextUtil.translate("tooltip.testbridge.packageName", itemInfo, satName);
            }
        }
        return super.getItemStackDisplayName(itemStack);
    }

    @Override
    public ModularPanel buildUI(GuiCreationContext guiCreationContext, GuiSyncManager guiSyncManager, boolean b) {
        ItemStack thisPackage = guiCreationContext.getUsedItemStack();
        ItemStackItemHandler itemHandler = new ItemStackItemHandler(thisPackage, 1);

        ModularPanel panel = ModularPanel.defaultPanel("package");
        panel.height(130)
            .bindPlayerInventory()
            .child(
                new Column().coverChildrenHeight()
                    .padding(7)
                    .child(IKey.lang("item.testbridge.item_package.name").asWidget())
                    .child(new Row().topRel(1.0f)
                        .child(new Column().coverChildren().crossAxisAlignment(CrossAxisAlignment.START)
                            .child(new TextFieldWidget().size(140, 18).value(SyncHandlers.string(
                                    () -> PackageHelper.getItemInfo(thisPackage, PackageHelper.ItemInfo.DESTINATION),
                                    destination -> PackageHelper.setDestination(thisPackage, destination)))))
                        .child(new Column().coverChildrenHeight().width(22)
                            .crossAxisAlignment(CrossAxisAlignment.END)
                            .child(new ItemSlot().slot(
                                    SyncHandlers.phantomItemSlot(itemHandler, 0).ignoreMaxStackSize(true))))));
        return panel;
    }

    public final boolean isPackage() {
        return isPackage;
    }
}
