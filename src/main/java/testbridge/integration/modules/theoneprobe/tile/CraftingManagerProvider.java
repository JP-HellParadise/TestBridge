package testbridge.integration.modules.theoneprobe.tile;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.config.Config;

import appeng.tile.AEBaseTile;

import testbridge.block.tile.TileCraftingManager;
import testbridge.helpers.TBText;
import testbridge.interfaces.ITranslationKey;

public class CraftingManagerProvider implements ITileProbInfoProvider, ITranslationKey {

  @Override
  public void addProbeInfo(AEBaseTile tile, ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
    if (tile instanceof TileCraftingManager) {
      addConnectInfo((TileCraftingManager) tile, probeInfo);

      IItemHandler patternInv = ((TileCraftingManager) tile).getInventoryByName("patterns");
      int patternCount = 0;

      for (int i = 0; i < patternInv.getSlots(); i++) {
        if (!patternInv.getStackInSlot(i).isEmpty()) {
          patternCount++;
        }
      }

      if (patternCount != 0) {
        IProbeInfo inv = probeInfo.vertical(probeInfo.defaultLayoutStyle().borderColor(Config.chestContentsBorderColor).spacing(0));
        IProbeInfo infoRow = null;
        for (int i = 0; i < patternCount; i++) {
          if (i % 9 == 0)
            infoRow = inv.horizontal();
          infoRow.item(patternInv.getStackInSlot(i));
        }
      } else
        probeInfo.text(new TBText(top$cm_prefix + "no_patterns").getTranslated());
    }
  }

  private void addConnectInfo(TileCraftingManager tile, IProbeInfo probeInfo) {
    String satName = tile.getSatelliteName();
    probeInfo.text(new TBText(top$cm_prefix + "select_sat")
        .addArgument(new TBText(satName.isEmpty() ? top$cm_prefix + "none" : tile.getSatellitePart() != null ?
            top$cm_prefix + "valid" : top$cm_prefix + "sat_error").addArgument(satName).getTranslated())
        .getTranslated());
  }
}
