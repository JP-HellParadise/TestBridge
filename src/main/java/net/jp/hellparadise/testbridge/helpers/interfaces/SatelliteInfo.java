package net.jp.hellparadise.testbridge.helpers.interfaces;

import java.util.Set;
import javax.annotation.Nonnull;
import net.minecraft.tileentity.TileEntity;

public interface SatelliteInfo {

    @Nonnull
    TileEntity getContainer();

    @Nonnull
    Set<SatelliteInfo> getSatellitesOfType();

    @Nonnull
    String getSatelliteName();

    void setSatelliteName(@Nonnull String var1);

    void ensureAllSatelliteStatus();
}
