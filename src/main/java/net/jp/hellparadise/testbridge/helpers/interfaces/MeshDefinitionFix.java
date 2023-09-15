package net.jp.hellparadise.testbridge.helpers.interfaces;

import javax.annotation.Nonnull;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.ItemStack;

public interface MeshDefinitionFix extends ItemMeshDefinition {

    ModelResourceLocation getLocation(final ItemStack stack);

    // Helper method to easily create lambda instances of this class
    static ItemMeshDefinition create(final MeshDefinitionFix lambda) {
        return lambda;
    }

    @Nonnull
    @Override
    default ModelResourceLocation getModelLocation(@Nonnull final ItemStack stack) {
        return getLocation(stack);
    }
}
