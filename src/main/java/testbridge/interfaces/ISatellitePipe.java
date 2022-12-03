package testbridge.interfaces;

import network.rs485.logisticspipes.connection.Adjacent;

public interface ISatellitePipe {
  Adjacent getAvailableAdjacent();
}
