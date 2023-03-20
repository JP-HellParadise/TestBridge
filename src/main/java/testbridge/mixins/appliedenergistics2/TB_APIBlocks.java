package testbridge.mixins.appliedenergistics2;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.api.definitions.IBlockDefinition;
import appeng.bootstrap.FeatureFactory;
import appeng.bootstrap.definitions.TileEntityDefinition;
import appeng.core.api.definitions.ApiBlocks;
import appeng.core.features.AEFeature;
import appeng.core.features.registries.PartModels;

import testbridge.block.BlockCraftingManager;
import testbridge.block.tile.TileEntityCraftingManager;
import testbridge.helpers.interfaces.IBlocks_TB;

@Mixin(value = ApiBlocks.class, remap = false)
public abstract class TB_APIBlocks implements IBlocks_TB {
  @Unique
  private IBlockDefinition cmBlock;

  @Override
  @Unique
  public IBlockDefinition cmBlock() {
    return cmBlock;
  }

  @Inject(method = "<init>", at = @At(value = "RETURN"), remap = false)
  public void setCmBlock(FeatureFactory registry, PartModels partModels, CallbackInfo ci){
    this.cmBlock  = registry.block("crafting_manager", BlockCraftingManager::new)
        .features(AEFeature.INTERFACE)
        .tileEntity(new TileEntityDefinition(TileEntityCraftingManager.class))
        .build();
  }
}
