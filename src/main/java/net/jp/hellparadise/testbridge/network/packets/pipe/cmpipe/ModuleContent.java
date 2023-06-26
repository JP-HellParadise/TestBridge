package net.jp.hellparadise.testbridge.network.packets.pipe.cmpipe;

import logisticspipes.network.abstractpackets.InventoryModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

import net.jp.hellparadise.testbridge.pipes.PipeCraftingManager;
import net.minecraft.entity.player.EntityPlayer;

@StaticResolve
public class ModuleContent extends InventoryModuleCoordinatesPacket {

    public ModuleContent(int id) {
        super(id);
    }

    @Override
    public ModernPacket template() {
        return new ModuleContent(getId());
    }

    @Override
    public void processPacket(EntityPlayer player) {
        final LogisticsTileGenericPipe pipe = this.getPipe(player.world, LTGPCompletionCheck.PIPE);
        if (pipe.pipe instanceof PipeCraftingManager) {
            PipeCraftingManager chassis = (PipeCraftingManager) pipe.pipe;
            chassis.handleModuleItemIdentifierList(getIdentList());
        }
    }
}
