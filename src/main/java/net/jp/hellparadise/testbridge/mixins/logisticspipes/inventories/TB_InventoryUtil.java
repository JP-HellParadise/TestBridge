package net.jp.hellparadise.testbridge.mixins.logisticspipes.inventories;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import javax.annotation.Nonnull;
import logisticspipes.utils.InventoryUtil;
import net.jp.hellparadise.testbridge.helpers.interfaces.TB_IInventoryUtil;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = InventoryUtil.class, remap = false)
public abstract class TB_InventoryUtil implements TB_IInventoryUtil {

    @Shadow(remap = false)
    @Final
    protected IItemHandler inventory;

    // Handle simple inv check for ItemStack list
    @Override
    public boolean roomForItem(@Nonnull List<ItemStack> list) {
        IntList blockedSlot = new IntArrayList();

        for (ItemStack stack : list) {
            if (stack.isEmpty()) {
                continue;
            }

            IntList emptySlots = new IntArrayList();
            int slots = this.inventory.getSlots();

            for (int i = 0; i < slots; i++) {
                ItemStack slotStack = this.inventory.getStackInSlot(i);
                if (slotStack.isEmpty() && !blockedSlot.contains(i)) {
                    emptySlots.add(i);
                }

                if (ItemHandlerHelper.canItemStacksStack(stack, slotStack)) {
                    int originCount = stack.getCount();
                    stack = this.inventory.insertItem(i, stack, true);
                    if (originCount > stack.getCount()) {
                        blockedSlot.add(i);
                    }
                    if (stack.isEmpty()) {
                        break;
                    }
                }
            }

            for (int slot : emptySlots) {
                stack = this.inventory.insertItem(slot, stack, true);
                blockedSlot.add(slot);
                if (stack.isEmpty()) {
                    break;
                }
            }

            if (!stack.isEmpty()) {
                return false; // Stack count > 0, hence forbid from transferring
            }
        }
        return true;
    }
}
