package net.jp.hellparadise.testbridge.helpers.inventory;

import javax.annotation.Nonnull;
import logisticspipes.LPItems;
import logisticspipes.items.ItemModule;
import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.utils.gui.RestrictedSlot;
import net.jp.hellparadise.testbridge.pipes.PipeCraftingManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class CrafterSlot extends RestrictedSlot {

    public final PipeCraftingManager parentPipe;
    public final int _moduleIndex;

    public CrafterSlot(IInventory iinventory, int i, int j, int k, PipeCraftingManager pipe) {
        super(iinventory, i, j, k, ItemModule.class);
        parentPipe = pipe;
        _moduleIndex = i;
    }

    @Nonnull
    @Override
    public ItemStack onTake(@Nonnull EntityPlayer player, @Nonnull ItemStack itemStack) {
        ItemModuleInformationManager.saveInformation(itemStack, parentPipe.getSubModule(_moduleIndex));
        return super.onTake(player, itemStack);
    }

    @Override
    public boolean isItemValid(@Nonnull ItemStack par1ItemStack) {
        return par1ItemStack.getItem() == Item.REGISTRY.getObject(LPItems.modules.get(ModuleCrafter.getName()));
    }
}
