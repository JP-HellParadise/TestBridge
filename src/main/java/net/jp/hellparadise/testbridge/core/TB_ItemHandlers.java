package net.jp.hellparadise.testbridge.core;

import javax.annotation.Nonnull;
import logisticspipes.LPItems;
import logisticspipes.modules.ModuleCrafter;
import net.jp.hellparadise.testbridge.items.FakeItem;
import net.jp.hellparadise.testbridge.items.VirtualPatternAE;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import org.apache.commons.lang3.RandomStringUtils;

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

    // Items
    public static Item itemHolder = TB_ItemHandlers.createItem(new FakeItem(false), "placeholder", "", null);
    public static Item itemPackage = TB_ItemHandlers.createItem(new FakeItem(true), "package", "", CreativeTabs.MISC);
    public static Item virtualPattern = TB_ItemHandlers.createItem(new VirtualPatternAE(), "virtualpattern", "", null);

    public static ItemStack getCrafterModule() {
        Item item = Item.REGISTRY.getObject(LPItems.modules.get(ModuleCrafter.getName()));
        return new ItemStack(item == null ? Items.AIR : item);
    }

    public static Item createItem(@Nonnull final Item item, @Nonnull final String name, @Nonnull final String key,
        CreativeTabs creativeTabs) {
        String itemName;
        String translationKey;
        if (!name.isEmpty()) {
            itemName = "item_" + name;
        } else {
            if (TB_Config.instance()
                .isFeatureEnabled(TB_Config.TBFeature.LOGGING))
                TestBridge.log.info(
                    "{} don't have name properly, will create random name instead!",
                    item.getClass()
                        .getName());
            itemName = "randomItem_" + RandomStringUtils.randomAlphabetic(10);
        }
        if (key.isEmpty()) {
            translationKey = Reference.MODID + "." + itemName;
        } else translationKey = key;
        final Item result = item.setTranslationKey(translationKey)
            .setRegistryName(Reference.MODID, itemName);
        if (creativeTabs != null) {
            return result.setCreativeTab(creativeTabs);
        }
        return result;
    }

    public static Block createBlock(@Nonnull final Block block, @Nonnull final String name, @Nonnull final String key,
        CreativeTabs creativeTabs) {
        String itemName;
        String translationKey;
        if (!name.isEmpty()) {
            itemName = "block_" + name;
        } else {
            if (TB_Config.instance()
                .isFeatureEnabled(TB_Config.TBFeature.LOGGING))
                TestBridge.log.info(
                    "{} don't have name properly, will create random name instead!",
                    block.getClass()
                        .getName());
            itemName = "randomBlock_" + RandomStringUtils.randomAlphabetic(10);
        }
        if (key.isEmpty()) {
            translationKey = Reference.MODID + "." + itemName;
        } else translationKey = key;
        final Block result = block.setTranslationKey(translationKey)
            .setRegistryName(Reference.MODID, itemName);
        if (creativeTabs != null) {
            return result.setCreativeTab(creativeTabs);
        }
        return result;
    }

}
