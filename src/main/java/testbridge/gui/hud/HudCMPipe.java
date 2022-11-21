package testbridge.gui.hud;

import logisticspipes.gui.hud.BasicHUDGui;
import logisticspipes.interfaces.IHUDConfig;
import logisticspipes.interfaces.IHUDModuleHandler;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.utils.item.ItemIdentifierInventory;

import testbridge.interfaces.TB_IHUDConfig;
import testbridge.pipes.PipeCraftingManager;

public class HudCMPipe extends BasicHUDGui {

  private final PipeCraftingManager pipe;
  private final ItemIdentifierInventory moduleInventory;

  private int selected = -1;
  private int modulePage = 0;

  private int xCursor;
  private int yCursor;

  public HudCMPipe(PipeCraftingManager pipeCraftingManager, ItemIdentifierInventory _moduleInventory) {
    pipe = pipeCraftingManager;
    moduleInventory = _moduleInventory;
  }

  public boolean display(TB_IHUDConfig config) {
    return config.isHUDCraftingManager() && (pipe.displayList.size() > 0);
  }

  @Override
  public boolean display(IHUDConfig ihudConfig) {
    return false;
  }

  @Override
  public void handleCursor(int x, int y) {
    super.handleCursor(x, y);
    xCursor = x;
    yCursor = y;
  }

  private void moduleClicked(int number) {
    selected = number;
    if (selected != -1) {
      LogisticsModule selectedmodule = pipe.getSubModule(selected);
      if (selectedmodule instanceof IHUDModuleHandler) {
        ((IHUDModuleHandler) selectedmodule).startHUDWatching();
      }
    }
  }

  private void resetSelection() {
    if (selected != -1) {
      LogisticsModule selectedmodule = pipe.getSubModule(selected);
      if (selectedmodule instanceof IHUDModuleHandler) {
        ((IHUDModuleHandler) selectedmodule).stopHUDWatching();
      }
    }
    selected = -1;
  }

  private boolean isSlotSelected() {
    return selected != -1;
  }

  private boolean isSlotSelected(int number) {
    return selected == number;
  }

  private boolean shouldDisplayButton(int number) {
    return modulePage * 3 <= number && number < (modulePage + 1) * 3;
  }

  public void stopWatching() {
    resetSelection();
  }

  @Override
  public boolean cursorOnWindow(int x, int y) {
    if (pipe.displayList.size() > 0) {
      return -50 < x && x < 50 && -28 < y && y < 30;
    } else {
      return -30 < x && x < 30 && -22 < y && y < 25;
    }
  }
}
