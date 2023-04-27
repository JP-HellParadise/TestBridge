package net.jp.hellparadise.testbridge.network.guis.pipe;

import javax.annotation.Nonnull;

import logisticspipes.items.ItemUpgrade;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;

import net.jp.hellparadise.testbridge.client.gui.GuiCMPipe;
import net.jp.hellparadise.testbridge.helpers.inventory.DummyContainer;
import net.jp.hellparadise.testbridge.modules.TB_ModuleCM;
import net.jp.hellparadise.testbridge.modules.TB_ModuleCM.BlockingMode;
import net.jp.hellparadise.testbridge.pipes.PipeCraftingManager;
import net.jp.hellparadise.testbridge.pipes.upgrades.ModuleUpgradeManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class CMGuiProvider extends ModuleCoordinatesGuiProvider {

    private boolean isBufferUpgrade;
    private boolean isContainerConnected;
    private int blockingMode;

    public CMGuiProvider(int id) {
        super(id);
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

    @Override
    public GuiProvider template() {
        return new CMGuiProvider(getId());
    }

    @Override
    public Object getClientGui(EntityPlayer player) {
        LogisticsTileGenericPipe pipe = getTileAs(player.world, LogisticsTileGenericPipe.class);
        TB_ModuleCM module = this.getLogisticsModule(player.getEntityWorld(), TB_ModuleCM.class);
        if (!(pipe.pipe instanceof PipeCraftingManager) || module == null) {
            return null;
        }
        module.blockingMode.setValue(BlockingMode.values()[blockingMode]);
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
        for (int i = 0; i < 3; i++) for (int j = 0; j < 9; j++)
            dummy.addCMModuleSlot(9 * i + j, _moduleInventory, 8 + 18 * j, 16 + 18 * i, _cmPipe);

        for (int i = 0; i < _cmPipe.getChassisSize(); i++) {
            final int fI = i;
            ModuleUpgradeManager upgradeManager = _cmPipe.getModuleUpgradeManager(i);
            dummy.addUpgradeSlot(
                0,
                upgradeManager,
                0,
                -(_cmPipe.getChassisSize()) * 18,
                9 + i * 20,
                itemStack -> CMGuiProvider.checkStack(itemStack, _cmPipe, fI));
            dummy.addUpgradeSlot(
                1,
                upgradeManager,
                1,
                -(_cmPipe.getChassisSize()) * 18,
                9 + i * 20,
                itemStack -> CMGuiProvider.checkStack(itemStack, _cmPipe, fI));
        }

        for (int x = 0; x < 3; x++) {
            dummy.addDummySlot(x, ((PipeCraftingManager) pipe.pipe).getModules().excludedInventory, x * 18 - 141, 55);
        }

        _cmPipe.localModeWatchers.add(player);
        return dummy;
    }

    public static boolean checkStack(@Nonnull ItemStack stack, PipeCraftingManager cmPipe, int moduleSlot) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemUpgrade)) {
            return false;
        }
        LogisticsModule module = cmPipe.getModules()
            .getModule(moduleSlot);
        if (module == null) {
            return false;
        }
        return ((ItemUpgrade) stack.getItem()).getUpgradeForItem(stack, null)
            .isAllowedForModule(module);
    }

    public CMGuiProvider setBufferUpgrade(boolean bufferUpgrade) {
        isBufferUpgrade = bufferUpgrade;
        return this;
    }

    public CMGuiProvider setContainerConnected(boolean containerConnected) {
        isContainerConnected = containerConnected;
        return this;
    }

    public CMGuiProvider setBlockingMode(int blockingMode) {
        this.blockingMode = blockingMode;
        return this;
    }
}
