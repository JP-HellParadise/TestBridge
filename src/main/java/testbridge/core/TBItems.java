package testbridge.core;

import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;

import net.minecraft.item.Item;

public class TBItems {

  // Pipes
  @ObjectHolder("logisticspipes:pipe_result")
  public static Item pipeResult;

  @ObjectHolder("logisticspipes:pipe_crafting_manager")
  public static Item pipeCraftingManager;

  // Modules
  @ObjectHolder("logisticspipes:module_crafter")
  public static Item moduleCrafter;

  // Upgrades
  @ObjectHolder("logisticspipes:upgrade_buffer_cm")
  public static Item upgradeBuffer;

  // Blocks

}
