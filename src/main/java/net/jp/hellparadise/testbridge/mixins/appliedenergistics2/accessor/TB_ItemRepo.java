package net.jp.hellparadise.testbridge.mixins.appliedenergistics2.accessor;

import appeng.api.storage.data.IAEItemStack;
import appeng.client.me.ItemRepo;
import appeng.util.prioritylist.IPartitionList;
import net.jp.hellparadise.testbridge.helpers.interfaces.ae2.AccessorItemRepo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;


@Mixin(value = ItemRepo.class, remap = false)
public abstract class TB_ItemRepo implements AccessorItemRepo {

    @Shadow(remap = false)
    private IPartitionList<IAEItemStack> myPartitionList;

    @Shadow(remap = false)
    private boolean resort;

    @Override
    public IPartitionList<IAEItemStack> getMyPartitionList() {
        return myPartitionList;
    }

    @Override
    public void setMyPartitionList(IPartitionList<IAEItemStack> partitionList) {
        myPartitionList = partitionList;
    }

    public void setResort(boolean resort) {
        this.resort = resort;
    }
}
