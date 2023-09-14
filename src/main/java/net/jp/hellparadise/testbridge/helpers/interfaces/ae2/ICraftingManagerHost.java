package net.jp.hellparadise.testbridge.helpers.interfaces.ae2;

import appeng.api.implementations.IUpgradeableHost;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.core.sync.GuiBridge;
import appeng.me.GridAccessException;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.jp.hellparadise.testbridge.helpers.DualityCraftingManager;
import net.jp.hellparadise.testbridge.part.PartSatelliteBus;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public interface ICraftingManagerHost extends ICraftingProvider, IUpgradeableHost, ICraftingRequester {

    DualityCraftingManager getCMDuality();

    TileEntity getTileEntity();

    void saveChanges();

    String getSatelliteName();

    PartSatelliteBus getSatellitePart();

    void setSatelliteName(String satName);

    GuiBridge getGuiBridge();

    BlockPos getBlockPos();

    default List<String> getAvailableSat() {
        List<String> retrieveList = new ObjectArrayList<>();
        try {
            for (final IGridNode node : getCMDuality().gridProxy.getGrid()
                    .getMachines(PartSatelliteBus.class)) {
                IGridHost h = node.getMachine();
                if (h instanceof PartSatelliteBus part) {
                    if (!part.getSatelliteName()
                            .isEmpty()) {
                        retrieveList.add(part.getSatelliteName());
                    }
                }
            }
        } catch (final GridAccessException ignore) {
            // :P
        }
        return retrieveList;
    }

    default int sideOrdinal() {
        return -1;
    }
}
