package net.jp.hellparadise.testbridge.network.packets.logisticspipe;

import java.util.List;
import java.util.UUID;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.tuples.Pair;
import net.jp.hellparadise.testbridge.client.popup.GuiSelectResultPopup;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class ProvideSatResultListPacket extends ModernPacket {

    private List<Pair<String, UUID>> uuidList;

    public ProvideSatResultListPacket(int id) {
        super(id);
    }

    @Override
    public void readData(LPDataInput input) {
        super.readData(input);
        uuidList = input.readArrayList(input1 -> new Pair<>(input1.readUTF(), input1.readUUID()));
    }

    @Override
    public void writeData(LPDataOutput output) {
        super.writeData(output);
        output.writeCollection(uuidList, (output1, object) -> {
            output1.writeUTF(object.getValue1());
            output1.writeUUID(object.getValue2());
        });
    }

    @Override
    public void processPacket(EntityPlayer player) {
        if (Minecraft.getMinecraft().currentScreen instanceof LogisticsBaseGuiScreen) {
            SubGuiScreen subGUI = ((LogisticsBaseGuiScreen) Minecraft.getMinecraft().currentScreen).getSubGui();
            if (subGUI instanceof GuiSelectResultPopup) {
                ((GuiSelectResultPopup) subGUI).handleResultList(uuidList);
            }
        }
    }

    @Override
    public ModernPacket template() {
        return new ProvideSatResultListPacket(getId());
    }

    public ProvideSatResultListPacket setUuidList(List<Pair<String, UUID>> uuidList) {
        this.uuidList = uuidList;
        return this;
    }
}
