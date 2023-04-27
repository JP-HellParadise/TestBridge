package net.jp.hellparadise.testbridge.network.packets.pipehandler;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.satpipe.SetNameResult;
import logisticspipes.utils.StaticResolve;

import net.jp.hellparadise.testbridge.client.gui.GuiResultPipe;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.client.FMLClientHandler;

@StaticResolve
public class TB_SetNameResult extends SetNameResult {

    public TB_SetNameResult(int id) {
        super(id);
    }

    @Override
    public void processPacket(EntityPlayer player) {
        if (FMLClientHandler.instance()
            .getClient().currentScreen instanceof GuiResultPipe) {
            ((GuiResultPipe<?>) FMLClientHandler.instance()
                .getClient().currentScreen).handleResponse(getResult(), getNewName());
        }
    }

    @Override
    public ModernPacket template() {
        return new TB_SetNameResult(getId());
    }

}
