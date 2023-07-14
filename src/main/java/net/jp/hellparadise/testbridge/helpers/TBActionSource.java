package net.jp.hellparadise.testbridge.helpers;

import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayer;

public class TBActionSource implements IActionSource {

    final IActionHost host;

    public TBActionSource(IActionHost host) {
        this.host = host;
    }

    @Nonnull
    @Override
    public Optional<EntityPlayer> player() {
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Optional<IActionHost> machine() {
        return Optional.ofNullable(this.host);
    }

    @Nonnull
    @Override
    public <T> Optional<T> context(@Nonnull Class<T> aClass) {
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TBActionSource that = (TBActionSource) o;
        return host.equals(that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host);
    }
}
