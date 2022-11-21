package testbridge.network.packets.pipe;

import logisticspipes.network.packets.pipe.CraftingPipeUpdatePacket;
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
public class CMPipeUpdatePacket extends CraftingPipeUpdatePacket {

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
		output.writeUTF(super.getSatelliteName());
 		output.writeUTF(resultName);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		super.setSatelliteName(input.readUTF());
		resultName = input.readUTF();
	}

	@Override
	public ModernPacket template() {
		return new CraftingPipeUpdatePacket(getId());
	}

	@Override
	public CMPipeUpdatePacket setSatelliteName(String satelliteName) {
		super.setSatelliteName(satelliteName);
		return this;
	}
}
