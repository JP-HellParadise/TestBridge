package testbridge.integration.modules.theoneprobe.part;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;

import appeng.api.parts.IPart;

import network.rs485.logisticspipes.util.TextUtil;

import testbridge.part.PartSatelliteBus;

public class SatelliteBusProvider implements IPartProbInfoProvider{

  @Override
  public void addProbeInfo(IPart part, ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
    if (part instanceof PartSatelliteBus) {
      probeInfo.text(getSatName((PartSatelliteBus) part));
    }
  }

  private String getSatName(PartSatelliteBus part) {
    String satName = part.getSatellitePipeName();
    if (satName.isEmpty())
      return TextUtil.translate(prefix_LP + "pipe.satellite.no_name");
    else
      return TextUtil.translate(prefix_LP + "pipe.satellite.name", satName);
  }
}
