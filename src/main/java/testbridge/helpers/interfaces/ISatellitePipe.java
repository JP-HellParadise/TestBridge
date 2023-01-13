package testbridge.helpers.interfaces;

import net.minecraft.util.EnumFacing;

import network.rs485.logisticspipes.connection.Adjacent;

import javax.annotation.Nullable;

public interface ISatellitePipe {
  void nextOrientation();
  void setPointedOrientation(@Nullable EnumFacing dir);
  Adjacent getAvailableAdjacent();
}
