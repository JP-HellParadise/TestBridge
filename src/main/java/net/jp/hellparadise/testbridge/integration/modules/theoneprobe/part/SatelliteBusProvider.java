package net.jp.hellparadise.testbridge.integration.modules.theoneprobe.part;

import appeng.api.parts.IPart;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import net.jp.hellparadise.testbridge.helpers.TranslationKey;
import net.jp.hellparadise.testbridge.part.PartSatelliteBus;
import net.jp.hellparadise.testbridge.utils.TextUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class SatelliteBusProvider implements IPartProbInfoProvider {

    @Override
    public void addProbeInfo(IPart part, ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world,
        IBlockState blockState, IProbeHitData data) {
        if (part instanceof PartSatelliteBus) {
            String satName = ((PartSatelliteBus) part).getSatelliteName();
            probeInfo.text(TextUtil.translate(
                    TranslationKey.top$sat_prefix + (satName.isEmpty() ? "no_name" : "name"), satName));
        }
    }
}
