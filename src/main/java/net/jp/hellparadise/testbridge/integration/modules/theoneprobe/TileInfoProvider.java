package net.jp.hellparadise.testbridge.integration.modules.theoneprobe;

import appeng.tile.AEBaseTile;
import com.google.common.collect.Lists;
import java.util.List;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;
import net.jp.hellparadise.testbridge.core.Reference;
import net.jp.hellparadise.testbridge.integration.modules.theoneprobe.tile.CraftingManagerProvider;
import net.jp.hellparadise.testbridge.integration.modules.theoneprobe.tile.ITileProbInfoProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public final class TileInfoProvider implements IProbeInfoProvider {

    private final List<ITileProbInfoProvider> providers;

    public TileInfoProvider() {
        final ITileProbInfoProvider crafting_manager = new CraftingManagerProvider();

        this.providers = Lists.newArrayList(crafting_manager);
    }

    @Override
    public String getID() {
        return Reference.MOD_ID + ":TileInfoProvider";
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world,
        IBlockState blockState, IProbeHitData data) {
        final TileEntity tile = world.getTileEntity(data.getPos());

        if (tile instanceof AEBaseTile aeBaseTile) {
            for (final ITileProbInfoProvider provider : this.providers) {
                provider.addProbeInfo(aeBaseTile, mode, probeInfo, player, world, blockState, data);
            }
        }
    }
}
