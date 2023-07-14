package net.jp.hellparadise.testbridge.network.packets.pipe.cmpipe;

import java.util.UUID;
import logisticspipes.network.abstractpackets.IntegerModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import net.jp.hellparadise.testbridge.modules.TB_ModuleCM;
import net.minecraft.entity.player.EntityPlayer;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class SetSatResultPacket extends IntegerModuleCoordinatesPacket {

    private UUID pipeUUID;

    public SetSatResultPacket(int id) {
        super(id);
    }

    @Override
    public void readData(LPDataInput input) {
        super.readData(input);
        pipeUUID = input.readUUID();
    }

    @Override
    public void writeData(LPDataOutput output) {
        super.writeData(output);
        output.writeUUID(pipeUUID);
    }

    @Override
    public void processPacket(EntityPlayer player) {
        TB_ModuleCM module = this.getLogisticsModule(player, TB_ModuleCM.class);
        if (module == null) {
            return;
        }
        if (getInteger() == 1) {
            module.setSatelliteUUID(pipeUUID);
        } else if (getInteger() == 2) {
            module.setResultUUID(pipeUUID);
        }
    }

    @Override
    public ModernPacket template() {
        return new SetSatResultPacket(getId());
    }

    public SetSatResultPacket setPipeUUID(UUID pipeUUID) {
        this.pipeUUID = pipeUUID;
        return this;
    }
}
