package testbridge.mixins.logisticspipes.recipes;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import logisticspipes.LPItems;
import logisticspipes.blocks.LogisticsProgramCompilerTileEntity.ProgrammCategories;
import logisticspipes.recipes.CraftingPartRecipes;
import logisticspipes.recipes.CraftingParts;
import logisticspipes.recipes.PipeChippedCraftingRecipes;
import logisticspipes.recipes.RecipeManager;

import testbridge.core.TBItems;

@Mixin(value = PipeChippedCraftingRecipes.class, remap = false)
public abstract class TB_MixinRecipes extends CraftingPartRecipes {
  @Shadow
  protected abstract Ingredient getIngredientForProgrammer(Item targetPipe);

  @Shadow
  protected abstract void registerPipeRecipeCategory(ResourceLocation recipeCategory, Item targetPipe);

  @Inject(method = "loadRecipes", at = @At(value = "TAIL"))
  private void TB_loadRecipes(CraftingParts parts, CallbackInfo ci) {
    //  Result Pipe
    registerPipeRecipeCategory(ProgrammCategories.MODDED, TBItems.pipeResult);
    RecipeManager.craftingManager.addRecipe(new ItemStack(TBItems.pipeResult),
        new RecipeManager.RecipeLayout(
            " p ",
            "rfr",
            " s "
        ),
        new RecipeManager.RecipeIndex('p', getIngredientForProgrammer(TBItems.pipeResult)),
        new RecipeManager.RecipeIndex('r', "dustRedstone"),
        new RecipeManager.RecipeIndex('f', parts.getChipFpga()),
        new RecipeManager.RecipeIndex('s', LPItems.pipeBasic)
    );

    // Crafting Manager pipe
    registerPipeRecipeCategory(ProgrammCategories.MODDED, TBItems.pipeCraftingManager);
    RecipeManager.craftingManager.addRecipe(new ItemStack(TBItems.pipeCraftingManager),
        new RecipeManager.RecipeLayout(
            "fpf",
            "bsb",
            "fdf"
        ),
        new RecipeManager.RecipeIndex('b', parts.getChipAdvanced()),
        new RecipeManager.RecipeIndex('p', getIngredientForProgrammer(TBItems.pipeCraftingManager)),
        new RecipeManager.RecipeIndex('s', LPItems.pipeCrafting),
        new RecipeManager.RecipeIndex('d', "gemDiamond"),
        new RecipeManager.RecipeIndex('f', parts.getChipFpga())
    );

    // Buffer Upgrade
    registerPipeRecipeCategory(ProgrammCategories.MODDED, TBItems.upgradeBuffer);
    RecipeManager.craftingManager.addRecipe(new ItemStack(TBItems.upgradeBuffer, 1),
        new RecipeManager.RecipeLayout(
            "rpr",
            "gag",
            "qnq"
        ),
        new RecipeManager.RecipeIndex('r', "dustRedstone"),
        new RecipeManager.RecipeIndex('p', getIngredientForProgrammer(TBItems.upgradeBuffer)),
        new RecipeManager.RecipeIndex('g', "ingotGold"),
        new RecipeManager.RecipeIndex('a', parts.getChipAdvanced()),
        new RecipeManager.RecipeIndex('q', "paper"),
        new RecipeManager.RecipeIndex('n', "nuggetGold")
    );
  }
}
