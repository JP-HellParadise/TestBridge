package testbridge.pipes.upgrades;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.upgrades.IPipeUpgrade;

import testbridge.pipes.PipeCraftingManager;

public class BufferCMUpgrade implements IPipeUpgrade {

  public static String getName() {
    return "buffer_cm";
  }

  @Override
  public boolean needsUpdate() {
    return false;
  }

  @Override
  public boolean isAllowedForPipe(CoreRoutedPipe pipe) {
    return pipe instanceof PipeCraftingManager;
  }

  @Override
  public boolean isAllowedForModule(LogisticsModule pipe) {
    return false;
  }

  @Override
  public String[] getAllowedPipes() {
    return new String[] { "crafting_manager" };
  }

  @Override
  public String[] getAllowedModules() {
    return new String[] {};
  }
}
