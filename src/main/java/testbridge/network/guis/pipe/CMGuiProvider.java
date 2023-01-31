package testbridge.network.guis.pipe;

import javax.annotation.Nonnull;

import lombok.Getter;
import lombok.Setter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import logisticspipes.items.ItemUpgrade;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;

import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

import testbridge.client.gui.GuiCMPipe;
import testbridge.helpers.inventory.DummyContainer;
import testbridge.modules.TB_ModuleCM;
import testbridge.modules.TB_ModuleCM.BlockingMode;
import testbridge.pipes.PipeCraftingManager;
import testbridge.pipes.upgrades.ModuleUpgradeManager;

@StaticResolve
public class CMGuiProvider extends ModuleCoordinatesGuiProvider {

  @Getter
  @Setter
  private boolean isBufferUpgrade;

  @Getter
  @Setter
  private boolean isContainerConnected;

  @Getter
  @Setter
  private int blockingMode;

  public CMGuiProvider(int id) {
    super(id);
  }

  @Override
  public Object getClientGui(EntityPlayer player) {
    LogisticsTileGenericPipe pipe = getTileAs(player.world, LogisticsTileGenericPipe.class);
    TB_ModuleCM module = this.getLogisticsModule(player.getEntityWorld(), TB_ModuleCM.class);
    if (!(pipe.pipe instanceof PipeCraftingManager) || module == null) {
      return null;
    }
    module.getBlockingMode().setValue(BlockingMode.values()[blockingMode]);
    return new GuiCMPipe(player, (PipeCraftingManager) pipe.pipe, module, isBufferUpgrade, isContainerConnected);
  }

  @Override
  public DummyContainer getContainer(EntityPlayer player) {
    LogisticsTileGenericPipe pipe = getTileAs(player.world, LogisticsTileGenericPipe.class);
    if (!(pipe.pipe instanceof PipeCraftingManager)) {
      return null;
    }
    TB_ModuleCM moduleCM = ((PipeCraftingManager) pipe.pipe).getModules();
    MainProxy.sendPacketToPlayer(moduleCM.getCMPipePacket(), player);
    final PipeCraftingManager _cmPipe = (PipeCraftingManager) pipe.pipe;
    IInventory _moduleInventory = _cmPipe.getModuleInventory();
    DummyContainer dummy = new DummyContainer(player, _moduleInventory, moduleCM);
    dummy.addNormalSlotsForPlayerInventory(8, 85);
    for (int i = 0; i < 3; i++)
      for (int j = 0; j < 9; j++)
        dummy.addCMModuleSlot(9*i+j, _moduleInventory, 8 + 18*j, 16 + 18*i, _cmPipe);

    for (int i = 0; i < _cmPipe.getChassisSize(); i++) {
      final int fI = i;
      ModuleUpgradeManager upgradeManager = _cmPipe.getModuleUpgradeManager(i);
      dummy.addUpgradeSlot(0, upgradeManager, 0, - (_cmPipe.getChassisSize()) * 18, 9 + i * 20, itemStack -> CMGuiProvider.checkStack(itemStack, _cmPipe, fI));
      dummy.addUpgradeSlot(1, upgradeManager, 1, - (_cmPipe.getChassisSize()) * 18, 9 + i * 20, itemStack -> CMGuiProvider.checkStack(itemStack, _cmPipe, fI));
    }

    for (int x = 0; x < 3; x++) {
      dummy.addDummySlot(x, moduleCM.excludedInventory, x * 18 - 141, 55);
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

  @Override
  public void writeData(LPDataOutput output) {
    super.writeData(output);
    output.writeBoolean(isBufferUpgrade);
    output.writeInt(blockingMode);
    output.writeBoolean(isContainerConnected);
  }

  @Override
  public void readData(LPDataInput input) {
    super.readData(input);
    isBufferUpgrade = input.readBoolean();
    blockingMode = input.readInt();
    isContainerConnected = input.readBoolean();
  }
}
