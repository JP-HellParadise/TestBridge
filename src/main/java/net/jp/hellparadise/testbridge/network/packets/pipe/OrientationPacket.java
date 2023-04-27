package net.jp.hellparadise.testbridge.network.packets.pipe;

import javax.annotation.Nullable;

import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

import net.jp.hellparadise.testbridge.pipes.PipeCraftingManager;
import net.jp.hellparadise.testbridge.pipes.ResultPipe;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;

@StaticResolve
public class OrientationPacket extends CoordinatesPacket {

    @Nullable
    private EnumFacing dir;

    public OrientationPacket(int id) {
        super(id);
    }

    @Override
    public void processPacket(EntityPlayer player) {
        LogisticsTileGenericPipe pipe = this.getPipe(player.world, LTGPCompletionCheck.PIPE);
        if (pipe.pipe instanceof PipeCraftingManager) {
            ((PipeCraftingManager) pipe.pipe).setPointedOrientation(dir);
        } else if (pipe.pipe instanceof ResultPipe) {
            ((ResultPipe) pipe.pipe).setPointedOrientation(dir);
        }
    }

    @Override
    public ModernPacket template() {
        return new OrientationPacket(getId());
    }

    public OrientationPacket setDir(@Nullable EnumFacing dir) {
        this.dir = dir;
        return this;
    }
}
