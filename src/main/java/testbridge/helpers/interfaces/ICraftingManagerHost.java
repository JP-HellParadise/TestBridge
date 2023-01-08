package testbridge.helpers.interfaces;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import appeng.api.implementations.IUpgradeableHost;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.core.sync.GuiBridge;

import testbridge.helpers.DualityCraftingManager;
import testbridge.part.PartSatelliteBus;

public interface ICraftingManagerHost extends ICraftingProvider, IUpgradeableHost, ICraftingRequester {

  DualityCraftingManager getCMDuality();

  TileEntity getTileEntity();

  void saveChanges();

  String getSatelliteName();

  PartSatelliteBus getSatellitePart();

  void setSatellite(String satName);

  GuiBridge getGuiBridge();

  BlockPos getBlockPos();
}

