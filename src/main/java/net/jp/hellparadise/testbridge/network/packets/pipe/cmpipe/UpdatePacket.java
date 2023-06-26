package net.jp.hellparadise.testbridge.network.packets.pipe.cmpipe;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import logisticspipes.utils.StaticResolve;

import net.jp.hellparadise.testbridge.modules.TB_ModuleCM;
import net.minecraft.entity.player.EntityPlayer;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class UpdatePacket extends ModuleCoordinatesPacket {

    private String satelliteName = "";
    private String resultName = "";
    private int blockingMode;

    public UpdatePacket(int id) {
        super(id);
    }

    @Override
    public void processPacket(EntityPlayer player) {
        TB_ModuleCM module = this.getLogisticsModule(player, TB_ModuleCM.class);
        if (module == null) {
            return;
        }
        module.handleCMUpdatePacket(this);
    }

    @Override
    public void writeData(LPDataOutput output) {
        super.writeData(output);
        output.writeUTF(satelliteName);
        output.writeUTF(resultName);
        output.writeInt(blockingMode);
    }

    @Override
    public void readData(LPDataInput input) {
        super.readData(input);
        satelliteName = input.readUTF();
        resultName = input.readUTF();
        blockingMode = input.readInt();
    }

    @Override
    public ModernPacket template() {
        return new UpdatePacket(getId());
    }

    public String getSatelliteName() {
        return satelliteName;
    }

    public UpdatePacket setSatelliteName(String satelliteName) {
        this.satelliteName = satelliteName;
        return this;
    }

    public String getResultName() {
        return resultName;
    }

    public UpdatePacket setResultName(String resultName) {
        this.resultName = resultName;
        return this;
    }

    public int getBlockingMode() {
        return blockingMode;
    }

    public UpdatePacket setBlockingMode(int blockingMode) {
        this.blockingMode = blockingMode;
        return this;
    }
}
