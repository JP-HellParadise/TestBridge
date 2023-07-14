package net.jp.hellparadise.testbridge.integration.modules.theoneprobe.tile;

import appeng.tile.AEBaseTile;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.config.Config;
import net.jp.hellparadise.testbridge.block.tile.TileEntityCraftingManager;
import net.jp.hellparadise.testbridge.helpers.TextHelper;
import net.jp.hellparadise.testbridge.helpers.interfaces.ITranslationKey;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

public class CraftingManagerProvider implements ITileProbInfoProvider, ITranslationKey {

    @Override
    public void addProbeInfo(AEBaseTile tile, ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world,
        IBlockState blockState, IProbeHitData data) {
        if (tile instanceof TileEntityCraftingManager) {
            addConnectInfo((TileEntityCraftingManager) tile, probeInfo);

            IItemHandler patternInv = ((TileEntityCraftingManager) tile).getInventoryByName("patterns");
            int patternCount = 0;

            for (int i = 0; i < patternInv.getSlots(); i++) {
                if (!patternInv.getStackInSlot(i)
                    .isEmpty()) {
                    patternCount++;
                }
            }

            if (patternCount != 0) {
                IProbeInfo inv = probeInfo.vertical(
                    probeInfo.defaultLayoutStyle()
                        .borderColor(Config.chestContentsBorderColor)
                        .spacing(0));
                IProbeInfo infoRow = null;
                for (int i = 0; i < patternCount; i++) {
                    if (i % 9 == 0) infoRow = inv.horizontal();
                    infoRow.item(patternInv.getStackInSlot(i));
                }
            } else probeInfo.text(new TextHelper(top$cm_prefix + "no_patterns").getTranslated());
        }
    }

    private void addConnectInfo(TileEntityCraftingManager tile, IProbeInfo probeInfo) {
        String satName = tile.getSatelliteName();
        probeInfo.text(
            new TextHelper(top$cm_prefix + "select_sat")
                .addArgument(
                    new TextHelper(
                        satName.isEmpty() ? top$cm_prefix + "none"
                            : tile.getSatellitePart() != null ? top$cm_prefix + "valid"
                                : top$cm_prefix + "router_error").addArgument(satName)
                                    .getTranslated())
                .getTranslated());
    }
}
