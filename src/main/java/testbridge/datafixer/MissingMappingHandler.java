package testbridge.datafixer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import com.google.common.collect.ImmutableMap;

import logisticspipes.LPConstants;

public class MissingMappingHandler {
  private Map<String, String> itemIDMap = ImmutableMap.<String, String>builder()
      //pipe
      .put("item.resultpipe", "pipe_result")
      .put("item.crafting_manager", "pipe_crafting_manager")
      .build();

//  private Map<String, String> blockIDMap = ImmutableMap.<String, String>builder()
//      .put("tile.block", "solid_block")
//      .build();

//  @SubscribeEvent
//  public void onMissingBlocks(RegistryEvent.MissingMappings<Block> e) {
//    for (RegistryEvent.MissingMappings.Mapping<Block> m : e.getMappings()) {
//      String entry = blockIDMap.get(m.key.getPath());
//      if (entry == null) continue;
//      Block value = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(LPConstants.LP_MOD_ID, entry));
//      if (value == null) continue;
//      m.remap(value);
//    }
//  }

  private List<String> ignoreItems = Arrays.asList(
//      "solid_block", "tile.block"
  );

  @SubscribeEvent
  public void onMissingItems(RegistryEvent.MissingMappings<Item> e) {
    for (RegistryEvent.MissingMappings.Mapping<Item> m : e.getMappings()) {
      String old = m.key.getPath();
      if (ignoreItems.contains(old)) {
        m.ignore();
        continue;
      }
      String entry = itemIDMap.get(old);
      if (entry == null) continue;
      Item value = ForgeRegistries.ITEMS.getValue(new ResourceLocation(LPConstants.LP_MOD_ID, entry));
      if (value == null) continue;
      m.remap(value);
    }
  }
}
