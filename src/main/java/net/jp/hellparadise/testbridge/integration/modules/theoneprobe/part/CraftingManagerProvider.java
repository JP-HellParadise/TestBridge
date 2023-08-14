package net.jp.hellparadise.testbridge.integration.modules.theoneprobe.part;

import appeng.api.parts.IPart;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import net.jp.hellparadise.testbridge.helpers.interfaces.ae2.ICraftingManagerHost;
import net.jp.hellparadise.testbridge.integration.modules.theoneprobe.common.ReadInvCraftingManager;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class CraftingManagerProvider implements IPartProbInfoProvider, ReadInvCraftingManager {

    @Override
    public void addProbeInfo(IPart part, ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world,
        IBlockState blockState, IProbeHitData data) {
        if (part instanceof ICraftingManagerHost) {
            addConnectInfo((ICraftingManagerHost) part, probeInfo);
            addInfo(part, probeInfo);
        }
    }
}
