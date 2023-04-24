package testbridge.integration.modules.theoneprobe;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;

import appeng.api.parts.IPart;
import appeng.integration.modules.theoneprobe.part.PartAccessor;

import io.github.korewali.Tags;

import testbridge.integration.modules.theoneprobe.part.CraftingManagerProvider;
import testbridge.integration.modules.theoneprobe.part.IPartProbInfoProvider;
import testbridge.integration.modules.theoneprobe.part.SatelliteBusProvider;

public class PartInfoProvider implements IProbeInfoProvider {
  private final List<IPartProbInfoProvider> providers;

  private final PartAccessor accessor = new PartAccessor();

  public PartInfoProvider() {
    final IPartProbInfoProvider crafting_manager = new CraftingManagerProvider();
    final IPartProbInfoProvider satellite_bus = new SatelliteBusProvider();

    this.providers = Lists.newArrayList(crafting_manager, satellite_bus);
  }

  @Override
  public String getID() {
    return Tags.MODID + ":PartInfoProvider";
  }

  @Override
  public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
    final TileEntity te = world.getTileEntity(data.getPos());
    final Optional<IPart> maybePart = this.accessor.getMaybePart(te, data);

    if (maybePart.isPresent()) {
      final IPart part = maybePart.get();

      for (final IPartProbInfoProvider provider : this.providers) {
        provider.addProbeInfo(part, mode, probeInfo, player, world, blockState, data);
      }
    }
  }
}
