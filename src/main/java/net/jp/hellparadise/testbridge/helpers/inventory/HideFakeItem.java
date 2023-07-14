package net.jp.hellparadise.testbridge.helpers.inventory;

import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.prioritylist.IPartitionList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import net.jp.hellparadise.testbridge.core.TB_ItemHandlers;
import net.jp.hellparadise.testbridge.integration.modules.appliedenergistics2.AE2Module;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

public class HideFakeItem implements IPartitionList<IAEItemStack> {

    private final List<IAEItemStack> ITEMS = Collections.singletonList(
        AE2Module.INSTANCE.api.storage()
            .getStorageChannel(IItemStorageChannel.class)
            .createStack(new ItemStack(TB_ItemHandlers.itemPackage, 1)));

    @Override
    public boolean isListed(IAEItemStack input) {
        return !GuiScreen.isCtrlKeyDown() && (input.getItem() == TB_ItemHandlers.itemHolder
            || (input.getItem() == TB_ItemHandlers.itemPackage && input.getStackSize() == 0));
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Iterable<IAEItemStack> getItems() {
        return ITEMS;
    }

    public Stream<IAEItemStack> getStreams() {
        return ITEMS.stream();
    }

}
