package testbridge.network.packets.pipe;

import net.minecraft.entity.player.EntityPlayer;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import logisticspipes.utils.StaticResolve;

import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

import testbridge.modules.TB_ModuleCM;

@StaticResolve
public class CMPipeUpdatePacket extends ModuleCoordinatesPacket {

	@Getter
	@Setter
	private String satelliteName = "";

	@Getter
	@Setter
  private String resultName = "";

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
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		satelliteName= input.readUTF();
		resultName = input.readUTF();
	}

	@Override
	public ModernPacket template() {
		return new CMPipeUpdatePacket(getId());
	}
}
