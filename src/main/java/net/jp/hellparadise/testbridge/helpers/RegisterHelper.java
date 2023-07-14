package net.jp.hellparadise.testbridge.helpers;

import java.util.HashSet;
import java.util.Set;
import net.jp.hellparadise.testbridge.helpers.interfaces.MeshDefinitionFix;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;

public class RegisterHelper {

    /**
     * The {@link Item}s that have had models registered so far.
     */
    private static final Set<Item> itemsRegistered = new HashSet<>();

    /**
     * Register a single model for an {@link Item}.
     * <p>
     * Uses the registry name as the domain/path and {@code "inventory"} as the variant.
     *
     * @param item The Item
     */
    private static void registerItemModel(final Item item) {
        registerItemModel(
            item,
            item.getRegistryName()
                .toString());
    }

    /**
     * Register a single model for an {@link Item}.
     * <p>
     * Uses {@code modelLocation} as the domain/path and {@code "inventory"} as the variant.
     *
     * @param item          The Item
     * @param modelLocation The model location
     */
    public static void registerItemModel(final Item item, final String modelLocation) {
        final ModelResourceLocation fullModelLocation = new ModelResourceLocation(modelLocation, "inventory");
        registerItemModel(item, fullModelLocation);
    }

    /**
     * Register a single model for an {@link Item}.
     * <p>
     * Uses {@code fullModelLocation} as the domain, path and variant.
     *
     * @param item              The Item
     * @param fullModelLocation The full model location
     */
    private static void registerItemModel(final Item item, final ModelResourceLocation fullModelLocation) {
        ModelBakery.registerItemVariants(item, fullModelLocation); // Ensure the custom model is loaded and prevent the
                                                                   // default model from being loaded
        registerItemModel(item, MeshDefinitionFix.create(stack -> fullModelLocation));
    }

    /**
     * Register an {@link ItemMeshDefinition} for an {@link Item}.
     *
     * @param item           The Item
     * @param meshDefinition The ItemMeshDefinition
     */
    private static void registerItemModel(final Item item, final ItemMeshDefinition meshDefinition) {
        itemsRegistered.add(item);
        ModelLoader.setCustomMeshDefinition(item, meshDefinition);
    }
}
