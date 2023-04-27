package net.jp.hellparadise.testbridge.integration.modules.theoneprobe.part;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;

import net.jp.hellparadise.testbridge.helpers.TextHelper;
import net.jp.hellparadise.testbridge.helpers.interfaces.ITranslationKey;
import net.jp.hellparadise.testbridge.part.PartSatelliteBus;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import appeng.api.parts.IPart;

public class SatelliteBusProvider implements IPartProbInfoProvider, ITranslationKey {

    @Override
    public void addProbeInfo(IPart part, ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world,
        IBlockState blockState, IProbeHitData data) {
        if (part instanceof PartSatelliteBus) {
            String satName = ((PartSatelliteBus) part).getSatellitePipeName();
            if (satName.isEmpty()) probeInfo.text(new TextHelper(top$sat_prefix + "no_name").getTranslated());
            else {
                probeInfo.text(
                    new TextHelper(top$sat_prefix + "name").addArgument(satName)
                        .getTranslated());
            }
        }
    }
}
