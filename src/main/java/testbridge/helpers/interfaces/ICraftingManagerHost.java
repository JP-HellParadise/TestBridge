package testbridge.helpers.interfaces;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import appeng.api.implementations.IUpgradeableHost;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.core.sync.GuiBridge;

import testbridge.helpers.DualityCraftingManager;

public interface ICraftingManagerHost extends ICraftingProvider, IUpgradeableHost, ICraftingRequester {

  DualityCraftingManager getCMDuality();

  TileEntity getTileEntity();

  void saveChanges();

  String getSatellite();

  void setSatellite(String satName);

  ItemStack getItemStackRepresentation();

  GuiBridge getGuiBridge();

  BlockPos getBlockPos();
}

