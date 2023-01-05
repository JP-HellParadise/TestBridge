package testbridge.integration.modules.logisticspipe;

import logisticspipes.LPItems;
import logisticspipes.blocks.LogisticsProgramCompilerTileEntity;
import logisticspipes.items.ItemUpgrade;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.recipes.NBTIngredient;
import logisticspipes.recipes.RecipeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;
import testbridge.core.TB_ItemHandlers;
import testbridge.core.TestBridge;
import testbridge.integration.IIntegrationModule;
import testbridge.pipes.PipeCraftingManager;
import testbridge.pipes.ResultPipe;
import testbridge.pipes.upgrades.BufferCMUpgrade;

public class LPModule implements IIntegrationModule {
  public void preInit() {
    MinecraftForge.EVENT_BUS.register(this);
  }

  public void init(){
    if (FMLLaunchHandler.side() == Side.SERVER) {
      TestBridge.TBTextures.registerBlockIcons(null);
    }

    loadRecipes();
  }

  @SubscribeEvent
  @SideOnly(Side.CLIENT)
  public static void textureLoad(TextureStitchEvent.Pre event) {
    TestBridge.TBTextures.registerBlockIcons(Minecraft.getMinecraft().getTextureMapBlocks());
  }

  @SubscribeEvent
  private void initItems(RegistryEvent.Register<Item> event) {
    // Pipe
    LogisticsBlockGenericPipe.registerPipe(registry, "result", ResultPipe::new);
    LogisticsBlockGenericPipe.registerPipe(registry, "crafting_manager", PipeCraftingManager::new);
    // Upgrade
    ItemUpgrade.registerUpgrade(registry, BufferCMUpgrade.getName(), BufferCMUpgrade::new);
  }

  private static void loadRecipes() {
    ResourceLocation resultPipe = TB_ItemHandlers.pipeResult.getRegistryName();
    ResourceLocation craftingMgrPipe = TB_ItemHandlers.pipeCraftingManager.getRegistryName();
    ResourceLocation bufferUpgrage = TB_ItemHandlers.upgradeBuffer.getRegistryName();

    LogisticsProgramCompilerTileEntity.programByCategory.get(LogisticsProgramCompilerTileEntity.ProgrammCategories.MODDED).add(resultPipe);
    LogisticsProgramCompilerTileEntity.programByCategory.get(LogisticsProgramCompilerTileEntity.ProgrammCategories.MODDED).add(craftingMgrPipe);
    LogisticsProgramCompilerTileEntity.programByCategory.get(LogisticsProgramCompilerTileEntity.ProgrammCategories.MODDED).add(bufferUpgrage);
    ResourceLocation group = new ResourceLocation(TestBridge.MODID, "recipes");

    //  Result Pipe
    RecipeManager.craftingManager.addRecipe(new ItemStack(TB_ItemHandlers.pipeResult),
        new RecipeManager.RecipeLayout(
            " p ",
            "rfr",
            " s "
        ),
        new RecipeManager.RecipeIndex('p', getIngredientForProgrammer(TB_ItemHandlers.pipeResult)),
        new RecipeManager.RecipeIndex('r', "dustRedstone"),
        new RecipeManager.RecipeIndex('f', LPItems.chipFPGA),
        new RecipeManager.RecipeIndex('s', LPItems.pipeBasic)
    );

    // Crafting Manager pipe
    RecipeManager.craftingManager.addRecipe(new ItemStack(TB_ItemHandlers.pipeCraftingManager),
        new RecipeManager.RecipeLayout(
            "fpf",
            "bsb",
            "fdf"
        ),
        new RecipeManager.RecipeIndex('b', LPItems.chipAdvanced),
        new RecipeManager.RecipeIndex('p', getIngredientForProgrammer(TB_ItemHandlers.pipeCraftingManager)),
        new RecipeManager.RecipeIndex('s', LPItems.pipeCrafting),
        new RecipeManager.RecipeIndex('d', "gemDiamond"),
        new RecipeManager.RecipeIndex('f', LPItems.chipFPGA)
    );

    // Buffer Upgrade
    RecipeManager.craftingManager.addRecipe(new ItemStack(TB_ItemHandlers.upgradeBuffer, 1),
        new RecipeManager.RecipeLayout(
            "rpr",
            "gag",
            "qnq"
        ),
        new RecipeManager.RecipeIndex('r', "dustRedstone"),
        new RecipeManager.RecipeIndex('p', getIngredientForProgrammer(TB_ItemHandlers.upgradeBuffer)),
        new RecipeManager.RecipeIndex('g', "ingotGold"),
        new RecipeManager.RecipeIndex('a', LPItems.chipAdvanced),
        new RecipeManager.RecipeIndex('q', "paper"),
        new RecipeManager.RecipeIndex('n', "nuggetGold")
    );
  }

  private static Ingredient getIngredientForProgrammer(Item targetPipe) {
    ItemStack programmerStack = new ItemStack(LPItems.logisticsProgrammer);
    programmerStack.setTagCompound(new NBTTagCompound());

    // Suppress NPE warning
    assert programmerStack.getTagCompound() != null;
    assert targetPipe.getRegistryName() != null;

    programmerStack.getTagCompound().setString("LogisticsRecipeTarget", targetPipe.getRegistryName().toString());
    return NBTIngredient.fromStacks(programmerStack);
  }
}
