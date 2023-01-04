package testbridge.integration.modules.theoneprobe.part;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.config.Config;

import appeng.api.parts.IPart;

import network.rs485.logisticspipes.util.TextUtil;

import testbridge.helpers.DualityCraftingManager;
import testbridge.part.PartCraftingManager;
import testbridge.part.PartSatelliteBus;

public class CraftingManagerProvider implements IPartProbInfoProvider {

  @Override
  public void addProbeInfo(IPart part, ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
    if (part instanceof PartCraftingManager) {
      IProbeInfo cmInfo = probeInfo.vertical();
      DualityCraftingManager duality = ((PartCraftingManager) part).getCMDuality();
      cmInfo.text(getSatName(duality.getSatellite(), duality.findSatellite(duality.getSatellite())));

      IItemHandler patternInv = duality.getInventoryByName("patterns");
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
        cmInfo.text(TextUtil.translate(prefix_TB + "crafting_manager.no_patterns"));
    }
  }

  private String getSatName(String satName, PartSatelliteBus part) {
    if (satName.isEmpty()) {
      return TextUtil.translate(prefix_TB + "crafting_manager.select_sat", TextUtil.translate(prefix_TB + "crafting_manager.none"));
    } else if (part != null) {
      return TextUtil.translate(prefix_TB + "crafting_manager.select_sat", TextUtil.translate(prefix_TB + "crafting_manager.valid", satName));
    }
    return TextUtil.translate(prefix_TB + "crafting_manager.select_sat", TextUtil.translate(prefix_TB + "crafting_manager.sat_error", satName));
  }
}
