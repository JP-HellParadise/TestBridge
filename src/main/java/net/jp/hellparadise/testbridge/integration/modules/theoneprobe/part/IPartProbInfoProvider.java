package net.jp.hellparadise.testbridge.integration.modules.theoneprobe.part;

import appeng.api.parts.IPart;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public interface IPartProbInfoProvider {

    void addProbeInfo(IPart part, ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world,
        IBlockState blockState, IProbeHitData data);
}
