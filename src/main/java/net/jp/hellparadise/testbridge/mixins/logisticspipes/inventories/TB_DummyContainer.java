package net.jp.hellparadise.testbridge.mixins.logisticspipes.inventories;

import java.util.List;
import javax.annotation.Nonnull;
import net.jp.hellparadise.testbridge.helpers.interfaces.lp.ExtendedDummyContainer;
import net.jp.hellparadise.testbridge.helpers.inventory.CrafterSlot;
import net.jp.hellparadise.testbridge.pipes.PipeCraftingManager;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = logisticspipes.utils.gui.DummyContainer.class, remap = false)
public abstract class TB_DummyContainer implements ExtendedDummyContainer {
    @Shadow(remap = false)
    private List<Slot> transferTop;

    @Shadow(remap = false)
    protected abstract Slot addSlotToContainer(@Nonnull Slot slotIn);

    @Unique public void addCMModuleSlot(int slotId, IInventory inventory, int xCoord, int yCoord, PipeCraftingManager pipe) {
        transferTop.add(addSlotToContainer(new CrafterSlot(inventory, slotId, xCoord, yCoord, pipe)));
    }
}
