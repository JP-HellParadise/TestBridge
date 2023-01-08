package testbridge.integration.modules.theoneprobe.part;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;

import appeng.api.parts.IPart;

import testbridge.helpers.TBText;
import testbridge.interfaces.ITranslationKey;
import testbridge.part.PartSatelliteBus;

public class SatelliteBusProvider implements IPartProbInfoProvider, ITranslationKey {

  @Override
  public void addProbeInfo(IPart part, ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
    if (part instanceof PartSatelliteBus) {
      String satName = ((PartSatelliteBus) part).getSatellitePipeName();
      if (satName.isEmpty())
        probeInfo.text(new TBText(top$sat_prefix + "no_name").getTranslated());
      else {
        probeInfo.text(new TBText(top$sat_prefix + "name").addArgument(satName).getTranslated());
      }
    }
  }
}
