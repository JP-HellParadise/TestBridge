package net.jp.hellparadise.testbridge.helpers.interfaces;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.tileentity.TileEntity;

public interface SatelliteInfo {

    @Nullable TileEntity getContainer();

    @Nonnull
    Set<SatelliteInfo> getSatellitesOfType();

    @Nonnull
    String getSatelliteName();

    void setSatelliteName(@Nonnull String var1);

    void ensureAllSatelliteStatus();

    default boolean isExist(String _name) {
        return getSatellitesOfType().stream()
                .anyMatch(
                        it -> it.getSatelliteName()
                                .equals(_name));
    }
}
