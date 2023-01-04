package testbridge.integration.modules.theoneprobe.part;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;

import appeng.api.parts.IPart;

import testbridge.core.TestBridge;

public interface IPartProbInfoProvider {

  String prefix_TB = "top." + TestBridge.MODID + ".";
  String prefix_LP = "top.logisticspipes.";

  void addProbeInfo(IPart part, ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data);

}
