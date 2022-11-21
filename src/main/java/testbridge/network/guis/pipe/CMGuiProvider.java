package testbridge.network.guis.pipe;

import logisticspipes.items.ItemUpgrade;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.abstractguis.BooleanModuleCoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import testbridge.gui.GuiCMPipe;
import testbridge.modules.TB_ModuleCM;
import testbridge.pipes.PipeCraftingManager;
import testbridge.pipes.upgrades.ModuleUpgradeManager;
import testbridge.utils.gui.DummyContainer;

import javax.annotation.Nonnull;

@StaticResolve
public class CMGuiProvider extends BooleanModuleCoordinatesGuiProvider {

  public CMGuiProvider(int id) {
    super(id);
  }

  @Override
  public Object getClientGui(EntityPlayer player) {
    LogisticsTileGenericPipe pipe = getTileAs(player.world, LogisticsTileGenericPipe.class);
    TB_ModuleCM module = this.getLogisticsModule(player.getEntityWorld(), TB_ModuleCM.class);
    if (!(pipe.pipe instanceof PipeCraftingManager)) {
      return null;
    }
    return new GuiCMPipe(player, module, (PipeCraftingManager) pipe.pipe, isFlag());
  }

  @Override
  public DummyContainer getContainer(EntityPlayer player) {
    LogisticsTileGenericPipe pipe = getTileAs(player.world, LogisticsTileGenericPipe.class);
    if (!(pipe.pipe instanceof PipeCraftingManager)) {
      return null;
    }
    final PipeCraftingManager _cmPipe = (PipeCraftingManager) pipe.pipe;
    IInventory _moduleInventory = _cmPipe.getModuleInventory();
    DummyContainer dummy = new DummyContainer(player.inventory, _moduleInventory);
    if (_cmPipe.getChassisSize() < 5) {
      dummy.addNormalSlotsForPlayerInventory(18, 97);
    } else {
      dummy.addNormalSlotsForPlayerInventory(18, 174);
    }
    for (int i = 0; i < _cmPipe.getChassisSize(); i++) {
      dummy.addCMModuleSlot(i, _moduleInventory, 19, 9 + 20 * i, _cmPipe);
    }

    if (_cmPipe.getUpgradeManager().hasUpgradeModuleUpgrade()) {
      for (int i = 0; i < _cmPipe.getChassisSize(); i++) {
        final int fI = i;
        ModuleUpgradeManager upgradeManager = _cmPipe.getModuleUpgradeManager(i);
//        dummy.addUpgradeSlot(0, upgradeManager, 0, 145, 9 + i * 20, itemStack -> CMGuiProvider.checkStack(itemStack, _cmPipe, fI));
//        dummy.addUpgradeSlot(1, upgradeManager, 1, 165, 9 + i * 20, itemStack -> CMGuiProvider.checkStack(itemStack, _cmPipe, fI));
      }
    }
    return dummy;
  }

  public static boolean checkStack(@Nonnull ItemStack stack, PipeCraftingManager cmPipe, int moduleSlot) {
    if (stack.isEmpty() || !(stack.getItem() instanceof ItemUpgrade)) {
      return false;
    }
    LogisticsModule module = cmPipe.getModules().getModule(moduleSlot);
    if (module == null) {
      return false;
    }
    return ((ItemUpgrade) stack.getItem()).getUpgradeForItem(stack, null).isAllowedForModule(module);
  }

  @Override
  public GuiProvider template() {
    return new CMGuiProvider(getId());
  }
}
