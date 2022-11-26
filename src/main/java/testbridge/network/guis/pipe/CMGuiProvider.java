package testbridge.network.guis.pipe;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import logisticspipes.items.ItemUpgrade;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.abstractguis.BooleanModuleCoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;

import testbridge.gui.GuiCMPipe;
import testbridge.pipes.PipeCraftingManager;
import testbridge.pipes.upgrades.ModuleUpgradeManager;
import testbridge.utils.gui.DummyContainer;

@StaticResolve
public class CMGuiProvider extends BooleanModuleCoordinatesGuiProvider {

  public CMGuiProvider(int id) {
    super(id);
  }

  @Override
  public Object getClientGui(EntityPlayer player) {
    LogisticsTileGenericPipe pipe = getTileAs(player.world, LogisticsTileGenericPipe.class);
    if (!(pipe.pipe instanceof PipeCraftingManager)) {
      return null;
    }
    return new GuiCMPipe(player, (PipeCraftingManager) pipe.pipe, isFlag());
  }

  @Override
  public DummyContainer getContainer(EntityPlayer player) {
    LogisticsTileGenericPipe pipe = getTileAs(player.world, LogisticsTileGenericPipe.class);
    if (!(pipe.pipe instanceof PipeCraftingManager)) {
      return null;
    }
    MainProxy.sendPacketToPlayer(((PipeCraftingManager) pipe.pipe).getModules().getCMPipePacket(), player);
    final PipeCraftingManager _cmPipe = (PipeCraftingManager) pipe.pipe;
    IInventory _moduleInventory = _cmPipe.getModuleInventory();
    DummyContainer dummy = new DummyContainer(player.inventory, _moduleInventory);
    dummy.addNormalSlotsForPlayerInventory(8, 85);
    for (int i = 0; i < 3; i++)
      for (int j = 0; j < 9; j++)
        dummy.addCMModuleSlot(9*i+j, _moduleInventory, 8 + 18*j, 16 + 18*i, _cmPipe);

    if (_cmPipe.getUpgradeManager().hasUpgradeModuleUpgrade()) {
      for (int i = 0; i < _cmPipe.getChassisSize(); i++) {
        final int fI = i;
        ModuleUpgradeManager upgradeManager = _cmPipe.getModuleUpgradeManager(i);
        dummy.addUpgradeSlot(0, upgradeManager, 0, - (i - _cmPipe.getChassisSize()) * 18, 9 + i * 20, itemStack -> CMGuiProvider.checkStack(itemStack, _cmPipe, fI));
        dummy.addUpgradeSlot(1, upgradeManager, 1, - (i - _cmPipe.getChassisSize()) * 18, 9 + i * 20, itemStack -> CMGuiProvider.checkStack(itemStack, _cmPipe, fI));
      }
    }
    _cmPipe.localModeWatchers.add(player);
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
