package net.jp.hellparadise.testbridge.helpers.interfaces.lp;

import net.jp.hellparadise.testbridge.pipes.PipeCraftingManager;
import net.minecraft.inventory.IInventory;

public interface ExtendedDummyContainer {
    void addCMModuleSlot(int slotId, IInventory inventory, int xCoord, int yCoord, PipeCraftingManager pipe);
}
