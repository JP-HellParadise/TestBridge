package net.jp.hellparadise.testbridge.mixins.appliedenergistics2.accessor;

import appeng.util.prioritylist.IPartitionList;
import appeng.util.prioritylist.MergedPriorityList;
import java.util.Collection;
import net.jp.hellparadise.testbridge.helpers.interfaces.ae2.AccessorMergedPriorityList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MergedPriorityList.class, remap = false)
public abstract class TB_MergedPriorityList implements AccessorMergedPriorityList {
    @Final
    @Shadow(remap = false)
    private
    Collection<IPartitionList<?>> negative;

    @Override
    public Collection<IPartitionList<?>> getNegative() {
        return negative;
    }
}
