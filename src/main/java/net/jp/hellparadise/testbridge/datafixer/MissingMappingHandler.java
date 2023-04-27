package net.jp.hellparadise.testbridge.datafixer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import logisticspipes.LPConstants;

import net.jp.hellparadise.testbridge.Tags;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import appeng.core.AppEng;

import com.google.common.collect.ImmutableMap;

public class MissingMappingHandler {

    private final Map<String, String> itemIDMap = ImmutableMap.<String, String>builder()
        // pipe
        .put("pipe_lb.resultpipe", "lp:pipe_result")
        .put("pipe_lb.craftingmanager", "lp:pipe_crafting_manager")
        .put("upgrade_lb.buffer_upgrade", "lp:upgrade_buffer_cm")
        .put("upgrade_lb.adv_extraction_upgrade", "lp:upgrade_item_extraction")
        .put("lb.crafting_managerae", "ae2:crafting_manager")
        .put("lb.virtpattern", "tb:item_virtualpattern")
        .put("lb.logisticsfakeitem", "tb:item_placeholder")
        .put("lb.package", "tb:item_package")
        .build();

    private final Map<String, String> blockIDMap = ImmutableMap.<String, String>builder()
        .put("lb.crafting_managerae", "ae2:crafting_manager")
        .build();

    @SubscribeEvent
    public void onMissingBlocks(RegistryEvent.MissingMappings<Block> e) {
        for (RegistryEvent.MissingMappings.Mapping<Block> m : e.getAllMappings()) {
            if (blockIDMap.get(m.key.getPath()) == null) continue;
            String modID;
            String[] entry = blockIDMap.get(m.key.getPath())
                .split(":");
            switch (entry[0]) {
                case "lp":
                    modID = LPConstants.LP_MOD_ID;
                    break;
                case "ae2":
                    modID = AppEng.MOD_ID;
                    break;
                default:
                    modID = Tags.MODID;
            }
            Block value = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(modID, entry[1]));
            if (value == null) continue;
            m.remap(value);
        }
    }

    private final List<String> ignoreItems = Arrays.asList(
    // "solid_block", "tile.block"
    );

    @SubscribeEvent
    public void onMissingItems(RegistryEvent.MissingMappings<Item> e) {
        for (RegistryEvent.MissingMappings.Mapping<Item> m : e.getAllMappings()) {
            String old = m.key.getPath();
            if (ignoreItems.contains(old)) {
                m.ignore();
                continue;
            }

            if (itemIDMap.get(old) == null) continue;
            String modID;
            String[] entry = itemIDMap.get(old)
                .split(":");
            switch (entry[0]) {
                case "lp":
                    modID = LPConstants.LP_MOD_ID;
                    break;
                case "ae2":
                    modID = AppEng.MOD_ID;
                    break;
                default:
                    modID = Tags.MODID;
            }
            Item value = ForgeRegistries.ITEMS.getValue(new ResourceLocation(modID, entry[1]));
            if (value == null) continue;
            m.remap(value);
        }
    }
}
