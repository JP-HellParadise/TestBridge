package net.jp.hellparadise.testbridge.network.packets.pipe.cmpipe;

import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import net.jp.hellparadise.testbridge.pipes.PipeCraftingManager;
import net.minecraft.entity.player.EntityPlayer;
import network.rs485.logisticspipes.module.Gui;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class CMGui extends CoordinatesPacket {

    private int modulePos;

    public CMGui(int id) {
        super(id);
    }

    @Override
    public void writeData(LPDataOutput output) {
        output.writeInt(modulePos);
        super.writeData(output);
    }

    @Override
    public void readData(LPDataInput input) {
        modulePos = input.readInt();
        super.readData(input);
    }

    @Override
    public void processPacket(EntityPlayer player) {
        final LogisticsTileGenericPipe pipe = getPipe(player.world, LTGPCompletionCheck.PIPE);
        if (pipe.pipe instanceof PipeCraftingManager cm_pipe
                && cm_pipe.getSubModule(modulePos) instanceof Gui subModule) {
            Gui.getPipeGuiProvider(subModule)
                .setPosX(getPosX())
                .setPosY(getPosY())
                .setPosZ(getPosZ())
                .open(player);
        }
    }

    @Override
    public ModernPacket template() {
        return new CMGui(getId());
    }

    public CMGui setModulePos(int modulePos) {
        this.modulePos = modulePos;
        return this;
    }
}
