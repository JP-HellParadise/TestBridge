package testbridge.network.packets.pipe;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import logisticspipes.utils.StaticResolve;

import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

import testbridge.modules.TB_ModuleCM;

@StaticResolve
public class CMPipeUpdatePacket extends ModuleCoordinatesPacket {

	private String satelliteName = "";
  private String resultName = "";
	private int blockingMode;

  public CMPipeUpdatePacket(int id) {
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
		satelliteName= input.readUTF();
		resultName = input.readUTF();
		blockingMode = input.readInt();
	}

	@Override
	public ModernPacket template() {
		return new CMPipeUpdatePacket(getId());
	}

	public String getSatelliteName() {
		return satelliteName;
	}

	public CMPipeUpdatePacket setSatelliteName(String satelliteName) {
		this.satelliteName = satelliteName;
		return this;
	}

	public String getResultName() {
		return resultName;
	}

	public CMPipeUpdatePacket setResultName(String resultName) {
		this.resultName = resultName;
		return this;
	}

	public int getBlockingMode() {
		return blockingMode;
	}

	public CMPipeUpdatePacket setBlockingMode(int blockingMode) {
		this.blockingMode = blockingMode;
		return this;
	}
}
