package testbridge.core;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.RandomStringUtils;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;

import testbridge.items.FakeItem;

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

  // Items

  public static Item itemHolder = createItem(new FakeItem(false), "placeholder", "", null);

  public static Item itemPackage = createItem(new FakeItem(true), "package", "", null);

  private static Item createItem(@Nonnull final Item item, @Nonnull final String name, @Nonnull final String key, CreativeTabs creativeTabs) {
    String itemName;
    String translationKey = "";
    if (!name.equals("")) {
      itemName = "item_" + name;
    } else {
      TestBridge.log.error("Item don't have name properly, will create random name instead");
      itemName = "tb_itemRandom_" + RandomStringUtils.randomAlphabetic(10);
    }
    if (key.equals("")) {
      translationKey = "testbridge." + itemName;
    } else translationKey = key;
    final Item result = item.setTranslationKey(translationKey).setRegistryName(TestBridge.ID, itemName);
    if (creativeTabs != null) {
      return result.setCreativeTab(creativeTabs);
    }
    return result;
  }
}
