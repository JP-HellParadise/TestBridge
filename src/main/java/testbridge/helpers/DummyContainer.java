package testbridge.helpers;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import logisticspipes.interfaces.ISlotCheck;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.utils.gui.*;
import logisticspipes.utils.ReflectionHelper;

import testbridge.core.TB_ItemHandlers;
import testbridge.modules.TB_ModuleCM;
import testbridge.pipes.PipeCraftingManager;

public class DummyContainer extends logisticspipes.utils.gui.DummyContainer {

  private final List<Slot> transferTop = new ArrayList<>();
  private final List<Slot> transferBottom = new ArrayList<>();

  public DummyContainer(IInventory playerInventory, IInventory dummyInventory) {
    super(playerInventory, dummyInventory);
  }

  /***
   * Adds all slots for the player inventory and hotbar
   */
  public void addNormalSlotsForPlayerInventory(int xOffset, int yOffset) {
    if (_playerInventory == null) {
      return;
    }
    // Player "backpack"
    for (int row = 0; row < 3; row++) {
      for (int column = 0; column < 9; column++) {
        Slot slot = new Slot(_playerInventory, column + row * 9 + 9, xOffset + column * 18, yOffset + row * 18);
        addSlotToContainer(slot);
        transferBottom.add(slot);
      }
    }

    // Player "hotbar"
    for (int i1 = 0; i1 < 9; i1++) {
      Slot slot = new Slot(_playerInventory, i1, xOffset + i1 * 18, yOffset + 58);
      addSlotToContainer(slot);
      transferBottom.add(slot);
    }
  }

  public void addCMModuleSlot(int slotId, IInventory inventory, int xCoord, int yCoord, PipeCraftingManager pipe) {
    transferTop.add(addSlotToContainer(new CrafterSlot(inventory, slotId, xCoord, yCoord, pipe)));
  }

  /**
   * Add an upgrade slot that will accept upgrade items.
   *
   * @param slotId
   *            The slot number in the dummy IInventory this slot should map
   * @param manager
   *            ISlotUpgradeManager
   * @param upgradeSlotId
   *            Upgrade slot of modules
   * @param xCoord
   *            xCoord of TopLeft corner of where the slot should be rendered
   * @param yCoord
   *            yCoord of TopLeft corner of where the slot should be rendered
   * @param slotCheck
   *            ISlotCheck for item checker
   */
  @Override
  public Slot addUpgradeSlot(int slotId, ISlotUpgradeManager manager, int upgradeSlotId, int xCoord, int yCoord, ISlotCheck slotCheck) {
    Slot slot = addSlotToContainer(new UpgradeSlot(manager, upgradeSlotId, slotId, xCoord, yCoord, slotCheck));
    transferTop.add(slot);
    return slot;
  }

  /**
   * Transfer stack from dummy IInventory slot.
   *
   * @param player
   *            Current player
   * @param i
   *            Slot id
   */
  @Nonnull
  @Override
  public ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int i) {
    if (transferTop.isEmpty() || transferBottom.isEmpty()) {
      return super.transferStackInSlot(player, i);
    }
    Slot slot = inventorySlots.get(i);
    if (slot == null || slot instanceof DummySlot || slot instanceof UnmodifiableSlot || slot instanceof FluidSlot || slot instanceof ColorSlot || slot instanceof HandelableSlot || !slot.getHasStack()) {
      return ItemStack.EMPTY;
    }
    if (transferTop.contains(slot)) {
      handleShiftClickLists(slot, transferBottom, true, player);
      handleShiftClickLists(slot, transferBottom, false, player);
    } else if (transferBottom.contains(slot)) {
      handleShiftClickLists(slot, transferTop, true, player);
      handleShiftClickLists(slot, transferTop, false, player);
    }
    return ItemStack.EMPTY;
  }

  @Nonnull
  @Override
  public ItemStack superSlotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player) {
    ItemStack itemstack = ItemStack.EMPTY;
    InventoryPlayer inventoryplayer = player.inventory;

    if (clickTypeIn == ClickType.QUICK_CRAFT) {
      int j1 = this.dragEvent;
      this.dragEvent = getDragEvent(dragType);

      if ((j1 != 1 || this.dragEvent != 2) && j1 != this.dragEvent) {
        this.resetDrag();
      } else if (inventoryplayer.getItemStack().isEmpty()) {
        this.resetDrag();
      } else if (this.dragEvent == 0) {
        this.dragMode = extractDragMode(dragType);

        if (isValidDragMode(this.dragMode, player)) {
          this.dragEvent = 1;
          this.dragSlots.clear();
        } else {
          this.resetDrag();
        }
      } else if (this.dragEvent == 1) {
        Slot slot7 = this.inventorySlots.get(slotId);
        ItemStack itemstack12 = inventoryplayer.getItemStack();

        if (slot7 != null && canAddItemToSlot(slot7, itemstack12, true) && slot7.isItemValid(itemstack12) && (this.dragMode == 2
            || itemstack12.getCount() > this.dragSlots.size()) && this.canDragIntoSlot(slot7)) {
          this.dragSlots.add(slot7);
        }
      } else if (this.dragEvent == 2) {
        if (!this.dragSlots.isEmpty()) {
          ItemStack itemstack9 = inventoryplayer.getItemStack().copy();
          int k1 = inventoryplayer.getItemStack().getCount();

          for (Slot slot8 : this.dragSlots) {
            ItemStack itemstack13 = inventoryplayer.getItemStack();

            if (slot8 != null && canAddItemToSlot(slot8, itemstack13, true) && slot8.isItemValid(itemstack13) && (this.dragMode == 2
                || itemstack13.getCount() >= this.dragSlots.size()) && this.canDragIntoSlot(slot8)) {
              ItemStack itemstack14 = itemstack9.copy();
              int j3 = slot8.getHasStack() ? slot8.getStack().getCount() : 0;
              computeStackSize(this.dragSlots, this.dragMode, itemstack14, j3);
              int k3 = Math.min(itemstack14.getMaxStackSize(), slot8.getItemStackLimit(itemstack14));

              if (itemstack14.getCount() > k3) {
                itemstack14.setCount(k3);
              }

              k1 -= itemstack14.getCount() - j3;
              slot8.putStack(itemstack14);
            }
          }

          itemstack9.setCount(k1);
          inventoryplayer.setItemStack(itemstack9);
        }

        this.resetDrag();
      } else {
        this.resetDrag();
      }
    } else if (this.dragEvent != 0) {
      this.resetDrag();
    } else if ((clickTypeIn == ClickType.PICKUP || clickTypeIn == ClickType.QUICK_MOVE) && (dragType == 0 || dragType == 1)) {
      if (slotId == -999) {
        if (!inventoryplayer.getItemStack().isEmpty()) {
          if (dragType == 0) {
            player.dropItem(inventoryplayer.getItemStack(), true);
            inventoryplayer.setItemStack(ItemStack.EMPTY);
          }

          if (dragType == 1) {
            player.dropItem(inventoryplayer.getItemStack().splitStack(1), true);
          }
        }
      } else if (clickTypeIn == ClickType.QUICK_MOVE) {
        if (slotId < 0) {
          return ItemStack.EMPTY;
        }

        Slot slot5 = this.inventorySlots.get(slotId);

        if (slot5 == null || !slot5.canTakeStack(player)) {
          return ItemStack.EMPTY;
        }

        for (ItemStack itemstack7 = this.transferStackInSlot(player, slotId);
             !itemstack7.isEmpty() && ItemStack.areItemsEqual(slot5.getStack(), itemstack7); itemstack7 = this.transferStackInSlot(player, slotId)) {
          itemstack = itemstack7.copy();
        }
      } else {
        if (slotId < 0) {
          return ItemStack.EMPTY;
        }

        Slot slot6 = this.inventorySlots.get(slotId);

        if (slot6 != null) {
          ItemStack itemstack8 = slot6.getStack();
          ItemStack itemstack11 = inventoryplayer.getItemStack();

          if (!itemstack8.isEmpty()) {
            itemstack = itemstack8.copy();
          }

          if (itemstack8.isEmpty()) {
            if (!itemstack11.isEmpty() && slot6.isItemValid(itemstack11)) {
              int i3 = dragType == 0 ? itemstack11.getCount() : 1;

              if (i3 > slot6.getItemStackLimit(itemstack11)) {
                i3 = slot6.getItemStackLimit(itemstack11);
              }

              slot6.putStack(itemstack11.splitStack(i3));
            }
          } else if (slot6.canTakeStack(player)) {
            if (itemstack11.isEmpty()) {
              if (itemstack8.isEmpty()) {
                slot6.putStack(ItemStack.EMPTY);
                inventoryplayer.setItemStack(ItemStack.EMPTY);
              } else {
                int l2 = dragType == 0 ? itemstack8.getCount() : (itemstack8.getCount() + 1) / 2;
                inventoryplayer.setItemStack(slot6.decrStackSize(l2));

                if (itemstack8.isEmpty()) {
                  slot6.putStack(ItemStack.EMPTY);
                }

                slot6.onTake(player, inventoryplayer.getItemStack());
              }
            } else if (slot6.isItemValid(itemstack11)) {
              if (itemstack8.getItem() == itemstack11.getItem() && itemstack8.getMetadata() == itemstack11.getMetadata() && ItemStack
                  .areItemStackTagsEqual(itemstack8, itemstack11)) {
                int k2 = dragType == 0 ? itemstack11.getCount() : 1;

                if (k2 > slot6.getItemStackLimit(itemstack11) - itemstack8.getCount()) {
                  k2 = slot6.getItemStackLimit(itemstack11) - itemstack8.getCount();
                }

                if (k2 > itemstack11.getMaxStackSize() - itemstack8.getCount()) {
                  k2 = itemstack11.getMaxStackSize() - itemstack8.getCount();
                }

                itemstack11.shrink(k2);
                itemstack8.grow(k2);

                slot6.putStack(itemstack8); // XXX added reinserting of the modified itemStack (Fix ItemIdentifierInventory's disappearing items)
              } else if (itemstack11.getCount() <= slot6.getItemStackLimit(itemstack11)) {
                handleSwitch(slot6, itemstack8, itemstack11, player); // XXX added Slot switching handle method
                slot6.putStack(itemstack11);
                inventoryplayer.setItemStack(itemstack8);
              }
            } else if (itemstack8.getItem() == itemstack11.getItem() && itemstack11.getMaxStackSize() > 1 && (!itemstack8.getHasSubtypes()
                || itemstack8.getMetadata() == itemstack11.getMetadata()) && ItemStack.areItemStackTagsEqual(itemstack8, itemstack11)
                && !itemstack8.isEmpty()) {
              int j2 = itemstack8.getCount();

              if (j2 + itemstack11.getCount() <= itemstack11.getMaxStackSize()) {
                itemstack11.grow(j2);
                itemstack8 = slot6.decrStackSize(j2);

                if (itemstack8.isEmpty()) {
                  slot6.putStack(ItemStack.EMPTY);
                }

                slot6.onTake(player, inventoryplayer.getItemStack());
              }
            }
          }

          slot6.onSlotChanged();
        }
      }
    } else if (clickTypeIn == ClickType.SWAP && dragType >= 0 && dragType < 9) {
      Slot slot4 = this.inventorySlots.get(slotId);
      ItemStack itemstack6 = inventoryplayer.getStackInSlot(dragType);
      ItemStack itemstack10 = slot4.getStack();

      if (!itemstack6.isEmpty() || !itemstack10.isEmpty()) {
        if (itemstack6.isEmpty()) {
          if (slot4.canTakeStack(player)) {
            inventoryplayer.setInventorySlotContents(dragType, itemstack10);
            ReflectionHelper.invokePrivateMethod(Slot.class, slot4, "onSwapCraft", "func_190900_b", new Class[] { int.class }, new Object[] { itemstack10.getCount() });
            slot4.putStack(ItemStack.EMPTY);
            slot4.onTake(player, itemstack10);
          }
        } else if (itemstack10.isEmpty()) {
          if (slot4.isItemValid(itemstack6)) {
            int l1 = slot4.getItemStackLimit(itemstack6);

            if (itemstack6.getCount() > l1) {
              slot4.putStack(itemstack6.splitStack(l1));
            } else {
              slot4.putStack(itemstack6);
              inventoryplayer.setInventorySlotContents(dragType, ItemStack.EMPTY);
            }
          }
        } else if (slot4.canTakeStack(player) && slot4.isItemValid(itemstack6)) {
          int i2 = slot4.getItemStackLimit(itemstack6);

          if (itemstack6.getCount() > i2) {
            slot4.putStack(itemstack6.splitStack(i2));
            slot4.onTake(player, itemstack10);

            if (!inventoryplayer.addItemStackToInventory(itemstack10)) {
              player.dropItem(itemstack10, true);
            }
          } else {
            slot4.putStack(itemstack6);
            inventoryplayer.setInventorySlotContents(dragType, itemstack10);
            slot4.onTake(player, itemstack10);
          }
        }
      }
    } else if (clickTypeIn == ClickType.CLONE && player.capabilities.isCreativeMode && inventoryplayer.getItemStack().isEmpty() && slotId >= 0) {
      Slot slot3 = this.inventorySlots.get(slotId);

      if (slot3 != null && slot3.getHasStack()) {
        ItemStack itemstack5 = slot3.getStack().copy();
        itemstack5.setCount(itemstack5.getMaxStackSize());
        inventoryplayer.setItemStack(itemstack5);
      }
    } else if (clickTypeIn == ClickType.THROW && inventoryplayer.getItemStack().isEmpty() && slotId >= 0) {
      Slot slot2 = this.inventorySlots.get(slotId);

      if (slot2 != null && slot2.getHasStack() && slot2.canTakeStack(player)) {
        ItemStack itemstack4 = slot2.decrStackSize(dragType == 0 ? 1 : slot2.getStack().getCount());
        slot2.onTake(player, itemstack4);
        player.dropItem(itemstack4, true);
      }
    } else if (clickTypeIn == ClickType.PICKUP_ALL && slotId >= 0) {
      Slot slot = this.inventorySlots.get(slotId);
      ItemStack itemstack1 = inventoryplayer.getItemStack();

      if (!itemstack1.isEmpty() && (slot == null || !slot.getHasStack() || !slot.canTakeStack(player))) {
        int i = dragType == 0 ? 0 : this.inventorySlots.size() - 1;
        int j = dragType == 0 ? 1 : -1;

        for (int k = 0; k < 2; ++k) {
          for (int l = i; l >= 0 && l < this.inventorySlots.size() && itemstack1.getCount() < itemstack1.getMaxStackSize(); l += j) {
            Slot slot1 = this.inventorySlots.get(l);

            if (slot1.getHasStack() && canAddItemToSlot(slot1, itemstack1, true) && slot1.canTakeStack(player) && this
                .canMergeSlot(itemstack1, slot1)) {
              ItemStack itemstack2 = slot1.getStack();

              if (k != 0 || itemstack2.getCount() != itemstack2.getMaxStackSize()) {
                int i1 = Math.min(itemstack1.getMaxStackSize() - itemstack1.getCount(), itemstack2.getCount());
                ItemStack itemstack3 = slot1.decrStackSize(i1);
                itemstack1.grow(i1);

                if (itemstack3.isEmpty()) {
                  slot1.putStack(ItemStack.EMPTY);
                }

                slot1.onTake(player, itemstack3);
              }
            }
          }
        }
      }

      this.detectAndSendChanges();
    }

    return itemstack;
  }

  private void handleSwitch(Slot slot2, @Nonnull ItemStack out, @Nonnull ItemStack in, EntityPlayer player) {
    if (slot2 instanceof CrafterSlot) {
      TB_ModuleCM module = (TB_ModuleCM) ((CrafterSlot) slot2).get_pipe().getLogisticsModule();
      int moduleIndex = ((CrafterSlot) slot2).get_moduleIndex();
      if (out.getItem() == TB_ItemHandlers.moduleCrafter) {
        if (module.hasModule(moduleIndex)) {
          ItemModuleInformationManager.saveInformation(out, module.getModule(moduleIndex));
          module.removeModule(moduleIndex);
        }
      }
    }
  }

  private void handleShiftClickLists(Slot from, List<Slot> toList, boolean ignoreEmpty, EntityPlayer player) {
    if (!from.getHasStack()) {
      return;
    }
    for (Slot to : toList) {
      if (handleShiftClickForSlots(from, to, ignoreEmpty, player)) {
        return;
      }
    }
  }

  private boolean handleShiftClickForSlots(Slot from, Slot to, boolean ignoreEmpty, EntityPlayer player) {
    if (!from.getHasStack()) {
      return true;
    }
    ItemStack out = from.getStack();
    if (!to.getHasStack() && !ignoreEmpty && to.isItemValid(out)) {
      boolean remove = true;
      if (out.getCount() > to.getSlotStackLimit()) {
        out = from.decrStackSize(to.getSlotStackLimit());
        remove = false;
      }
      to.putStack(from.onTake(player, out));
      if (remove) {
        from.putStack(ItemStack.EMPTY);
      }
      return true;
    }
    if (from instanceof ModuleSlot || to instanceof ModuleSlot) {
      return false;
    }
    out = from.onTake(player, out);
    if (to.getHasStack() && to.getStack().isItemEqual(out) && ItemStack.areItemStackTagsEqual(to.getStack(), from.getStack())) {
      int free = Math.min(to.getSlotStackLimit(), to.getStack().getMaxStackSize()) - to.getStack().getCount();
      if (free > 0) {
        ItemStack toInsert = from.decrStackSize(free);
        toInsert = from.onTake(player, toInsert);
        ItemStack toStack = to.getStack();
        if (!toInsert.isEmpty() && !toStack.isEmpty()) {
          toStack.grow(toInsert.getCount());
          to.putStack(toStack);
          return !from.getHasStack();
        }
      }
    }
    return false;
  }
}
