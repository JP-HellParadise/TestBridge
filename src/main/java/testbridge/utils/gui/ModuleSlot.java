package testbridge.utils.gui;

import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.utils.gui.RestrictedSlot;

import lombok.Getter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

import net.minecraft.item.ItemStack;
import testbridge.core.TBItems;
import testbridge.pipes.PipeCraftingManager;

import javax.annotation.Nonnull;

public class ModuleSlot extends RestrictedSlot {
  @Getter
  private final PipeCraftingManager _pipe;
  @Getter
  private final int _moduleIndex;

  public ModuleSlot(IInventory iinventory, int i, int j, int k, PipeCraftingManager pipe) {
    super(iinventory, i, j, k, TBItems.moduleCrafter);
    _pipe = pipe;
    _moduleIndex = i;
  }

  @Nonnull
  @Override
  public ItemStack onTake(@Nonnull EntityPlayer player, @Nonnull ItemStack itemStack) {
    ItemModuleInformationManager.saveInformation(itemStack, _pipe.getSubModule(_moduleIndex));
    return super.onTake(player, itemStack);
  }
}
