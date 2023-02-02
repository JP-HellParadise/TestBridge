package testbridge.helpers;

import java.util.Optional;
import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;

import appeng.api.networking.IGridHost;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;

public class TBActionSource implements IActionSource {
  final IGridHost host;

  public TBActionSource(IGridHost host) {
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
    return Optional.ofNullable((IActionHost) this.host);
  }

  @Nonnull
  @Override
  public <T> Optional<T> context(@Nonnull Class<T> aClass) {
    return Optional.empty();
  }
}
