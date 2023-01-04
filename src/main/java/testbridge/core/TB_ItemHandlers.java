package testbridge.core;

import javax.annotation.Nonnull;

import net.minecraft.init.Items;
import org.apache.commons.lang3.RandomStringUtils;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;

import logisticspipes.LPItems;
import logisticspipes.modules.ModuleCrafter;

import testbridge.helpers.interfaces.IBlocks_TB;
import testbridge.items.FakeItem;
import testbridge.items.VirtualPatternAE;

public class TB_ItemHandlers {

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
  public static ItemStack tile_cm = ((IBlocks_TB) AE2Plugin.INSTANCE.api.definitions().blocks()).cmBlock().maybeStack(1).orElse(ItemStack.EMPTY);

  // Items

  public static Item itemHolder = createItem(new FakeItem(false), "placeholder", "", null);
  public static Item itemPackage = createItem(new FakeItem(true), "package", "", CreativeTabs.MISC);
  public static Item virtualPattern = createItem(new VirtualPatternAE(), "virtualpattern", "", null);

  public static Item createItem(@Nonnull final Item item, @Nonnull final String name, @Nonnull final String key, CreativeTabs creativeTabs) {
    String itemName;
    String translationKey;
    if (!name.equals("")) {
      itemName = "item_" + name;
    } else {
      TestBridge.log.error("Item don't have name properly, will create random name instead");
      itemName = "randomItem_" + RandomStringUtils.randomAlphabetic(10);
    }
    if (key.equals("")) {
      translationKey = "testbridge." + itemName;
    } else translationKey = key;
    final Item result = item.setTranslationKey(translationKey).setRegistryName(TestBridge.MODID, itemName);
    if (creativeTabs != null) {
      return result.setCreativeTab(creativeTabs);
    }
    return result;
  }

  public static Block createBlock(@Nonnull final Block block, @Nonnull final String name, @Nonnull final String key, CreativeTabs creativeTabs) {
    String itemName;
    String translationKey;
    if (!name.equals("")) {
      itemName = "block_" + name;
    } else {
      TestBridge.log.error("Item don't have name properly, will create random name instead");
      itemName = "randomBlock_" + RandomStringUtils.randomAlphabetic(10);
    }
    if (key.equals("")) {
      translationKey = "testbridge." + itemName;
    } else translationKey = key;
    final Block result = block.setTranslationKey(translationKey).setRegistryName(TestBridge.MODID, itemName);
    if (creativeTabs != null) {
      return result.setCreativeTab(creativeTabs);
    }
    return result;
  }

  public static ItemStack getCrafterModule() {
    Item item = Item.REGISTRY.getObject(LPItems.modules.get(ModuleCrafter.getName()));
    return new ItemStack(item == null ? Items.AIR : item);
  }

}
