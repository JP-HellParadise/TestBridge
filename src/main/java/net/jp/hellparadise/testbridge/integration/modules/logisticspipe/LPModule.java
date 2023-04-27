package net.jp.hellparadise.testbridge.integration.modules.logisticspipe;

import logisticspipes.LPItems;
import logisticspipes.blocks.LogisticsProgramCompilerTileEntity;
import logisticspipes.recipes.NBTIngredient;
import logisticspipes.recipes.RecipeManager;

import net.jp.hellparadise.testbridge.client.TB_Textures;
import net.jp.hellparadise.testbridge.core.TB_ItemHandlers;
import net.jp.hellparadise.testbridge.integration.IIntegrationModule;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.Side;

public class LPModule implements IIntegrationModule {

    public static TB_Textures TBTextures = new TB_Textures();

    public void preInit() {
        MinecraftForge.EVENT_BUS.register(LPEventHandler.preInit.class);
    }

    public void init() {
        if (FMLLaunchHandler.side() == Side.SERVER) {
            TBTextures.registerBlockIcons(null);
        }

        loadRecipes();
    }

    private static void loadRecipes() {
        ResourceLocation resultPipe = TB_ItemHandlers.pipeResult.getRegistryName();
        ResourceLocation craftingMgrPipe = TB_ItemHandlers.pipeCraftingManager.getRegistryName();
        ResourceLocation bufferUpgrade = TB_ItemHandlers.upgradeBuffer.getRegistryName();

        LogisticsProgramCompilerTileEntity.programByCategory
            .get(LogisticsProgramCompilerTileEntity.ProgrammCategories.MODDED)
            .add(resultPipe);
        LogisticsProgramCompilerTileEntity.programByCategory
            .get(LogisticsProgramCompilerTileEntity.ProgrammCategories.MODDED)
            .add(craftingMgrPipe);
        LogisticsProgramCompilerTileEntity.programByCategory
            .get(LogisticsProgramCompilerTileEntity.ProgrammCategories.MODDED)
            .add(bufferUpgrade);

        // Result Pipe
        RecipeManager.craftingManager.addRecipe(
            new ItemStack(TB_ItemHandlers.pipeResult),
            new RecipeManager.RecipeLayout(" p ", "rfr", " s "),
            new RecipeManager.RecipeIndex('p', getIngredientForProgrammer(TB_ItemHandlers.pipeResult)),
            new RecipeManager.RecipeIndex('r', "dustRedstone"),
            new RecipeManager.RecipeIndex('f', LPItems.chipFPGA),
            new RecipeManager.RecipeIndex('s', LPItems.pipeBasic));

        // Crafting Manager pipe
        RecipeManager.craftingManager.addRecipe(
            new ItemStack(TB_ItemHandlers.pipeCraftingManager),
            new RecipeManager.RecipeLayout("fpf", "bsb", "fdf"),
            new RecipeManager.RecipeIndex('b', LPItems.chipAdvanced),
            new RecipeManager.RecipeIndex('p', getIngredientForProgrammer(TB_ItemHandlers.pipeCraftingManager)),
            new RecipeManager.RecipeIndex('s', LPItems.pipeCrafting),
            new RecipeManager.RecipeIndex('d', "gemDiamond"),
            new RecipeManager.RecipeIndex('f', LPItems.chipFPGA));

        // Buffer Upgrade
        RecipeManager.craftingManager.addRecipe(
            new ItemStack(TB_ItemHandlers.upgradeBuffer, 1),
            new RecipeManager.RecipeLayout("rpr", "gag", "qnq"),
            new RecipeManager.RecipeIndex('r', "dustRedstone"),
            new RecipeManager.RecipeIndex('p', getIngredientForProgrammer(TB_ItemHandlers.upgradeBuffer)),
            new RecipeManager.RecipeIndex('g', "ingotGold"),
            new RecipeManager.RecipeIndex('a', LPItems.chipAdvanced),
            new RecipeManager.RecipeIndex('q', "paper"),
            new RecipeManager.RecipeIndex('n', "nuggetGold"));
    }

    private static Ingredient getIngredientForProgrammer(Item targetPipe) {
        ItemStack programmerStack = new ItemStack(LPItems.logisticsProgrammer);
        programmerStack.setTagCompound(new NBTTagCompound());
        programmerStack.getTagCompound()
            .setString(
                "LogisticsRecipeTarget",
                targetPipe.getRegistryName()
                    .toString());
        return NBTIngredient.fromStacks(programmerStack);
    }
}
