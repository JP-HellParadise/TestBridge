package testbridge.interfaces;

import logisticspipes.interfaces.IHUDConfig;

public interface TB_IHUDConfig extends IHUDConfig {
  boolean isHUDCraftingManager();

  void setHUDCraftingManager(boolean state);
}
