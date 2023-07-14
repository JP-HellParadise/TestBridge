package net.jp.hellparadise.testbridge.integration.modules.theoneprobe.tile;

import appeng.tile.AEBaseTile;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public interface ITileProbInfoProvider {

    void addProbeInfo(AEBaseTile tile, ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world,
        IBlockState blockState, IProbeHitData data);
}
