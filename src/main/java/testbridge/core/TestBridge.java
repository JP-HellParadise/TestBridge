package testbridge.core;

import javax.annotation.Nonnull;

import logisticspipes.recipes.NBTIngredient;
import logisticspipes.recipes.RecipeManager;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.ShapedOreRecipe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.SidedProxy;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import logisticspipes.blocks.LogisticsProgramCompilerTileEntity;
import logisticspipes.blocks.LogisticsProgramCompilerTileEntity.ProgrammCategories;
import logisticspipes.LPItems;
import logisticspipes.LogisticsPipes;
import logisticspipes.items.ItemUpgrade;

import testbridge.datafixer.TBDataFixer;
import testbridge.network.GuiHandler;
import testbridge.part.PartSatelliteBus;
import testbridge.pipes.PipeCraftingManager;
import testbridge.pipes.ResultPipe;
import testbridge.pipes.upgrades.BufferCMUpgrade;
import testbridge.proxy.CommonProxy;
import testbridge.textures.TB_Textures;

@Mod(modid = TestBridge.ID, name = TestBridge.NAME, version = TestBridge.VERSION, dependencies = TestBridge.DEPS, guiFactory = "", acceptedMinecraftVersions = "1.12.2")
public class TestBridge extends LogisticsPipes {

  public static final String ID = "testbridge";
  public static final String NAME = "Test Bridge";
  public static final String VERSION = "@VERSION@";
  public static final String DEPS = "after:appliedenergistics2;after:refinedstorage@[1.6.15,);required-after:mixinbooter@[5.0,);";

  @Getter
  private static boolean debug = true;

  public TestBridge() {
    MinecraftForge.EVENT_BUS.register(this);
  }

  @Mod.Instance("testbridge")
  public static TestBridge INSTANCE;

  @SidedProxy(
      clientSide = "testbridge.proxy.ClientProxy",
      serverSide = "testbridge.proxy.CommonProxy")
  public static CommonProxy proxy;

  public static final Logger log = LogManager.getLogger(NAME);

  //Creative tab
  public static final CreativeTabs CREATIVE_TAB_TB = new CreativeTabs("Test_Bridge") {
    @SideOnly(Side.CLIENT)
    @Nonnull
    public ItemStack createIcon() {
      return new ItemStack(LPItems.pipeBasic);
    }
  };

  public static TB_Textures TBTextures = new TB_Textures();

  @Getter
  private static boolean AELoaded;
  @Getter
  private static boolean RSLoaded;
  @Getter
  private static boolean TOPLoaded;

  @Mod.EventHandler
  public void _preInit(FMLPreInitializationEvent event) {
    log.info("==================================================================================");
    log.info("Test Bridge: Start Pre Initialization");
    long tM = System.currentTimeMillis();
    AELoaded = Loader.isModLoaded("appliedenergistics2");
    RSLoaded = Loader.isModLoaded("refinedstorage");
    TOPLoaded = Loader.isModLoaded("theoneprobe");

    proxy.preInit(event);

    if (AELoaded) {
      AE2Plugin.preInit();
    }



    //TODO: preInit

    log.info("Pre Initialization took in {} milliseconds", (System.currentTimeMillis() - tM));
    log.info("==================================================================================");
  }

  @Mod.EventHandler
  public void _init(FMLInitializationEvent evt) {
    log.info("==================================================================================");
    log.info("Start Initialization");
    long tM = System.currentTimeMillis();

    //TODO: init
    NetworkRegistry.INSTANCE.registerGuiHandler(TestBridge.INSTANCE, new GuiHandler());
    TBDataFixer.INSTANCE.init();

    loadRecipes();

    log.info("Initialization took in {} milliseconds", (System.currentTimeMillis() - tM));
    log.info("==================================================================================");
  }

  @Mod.EventHandler
  public void _postInit(FMLPostInitializationEvent event) {
    log.info("==================================================================================");
    log.info("Start Post Initialization");
    long tM = System.currentTimeMillis();

    //TODO: onPostInit

    log.info("Post Initialization took in {} milliseconds", (System.currentTimeMillis() - tM));
    log.info("==================================================================================");
  }

  @SubscribeEvent
  @Override
  public void initItems(RegistryEvent.Register<Item> event) {
    IForgeRegistry<Item> registry = event.getRegistry();

    // LP Register
    // Pipe
    registerPipe(registry, "result", ResultPipe::new);
    registerPipe(registry, "crafting_manager", PipeCraftingManager::new);
    // Upgrade
    ItemUpgrade.registerUpgrade(registry, BufferCMUpgrade.getName(), BufferCMUpgrade::new);
  }

  @SubscribeEvent
  @Override
  public void initBlocks(RegistryEvent.Register<Block> event) {
    IForgeRegistry<Block> registry = event.getRegistry();

    // TODO Block
  }

  @Mod.EventHandler
  @Override
  public void cleanup(FMLServerStoppingEvent event) {
    ResultPipe.cleanup();
    PartSatelliteBus.cleanup();
  }

  @Mod.EventHandler
  public void onServerLoad(FMLServerStartingEvent event) {
  // TODO
  }

  private static void loadRecipes() {
    ResourceLocation resultPipe = TBItems.pipeResult.delegate.name();
    ResourceLocation craftingMgrPipe = TBItems.pipeCraftingManager.delegate.name();
    ResourceLocation bufferUpgrage = TBItems.upgradeBuffer.delegate.name();

    LogisticsProgramCompilerTileEntity.programByCategory.get(ProgrammCategories.MODDED).add(resultPipe);
    LogisticsProgramCompilerTileEntity.programByCategory.get(ProgrammCategories.MODDED).add(craftingMgrPipe);
    LogisticsProgramCompilerTileEntity.programByCategory.get(ProgrammCategories.MODDED).add(bufferUpgrage);
    ResourceLocation group = new ResourceLocation(ID, "recipes");

    if (isAELoaded())
      AE2Plugin.loadRecipes(group);

    //  Result Pipe
    RecipeManager.craftingManager.addRecipe(new ItemStack(TBItems.pipeResult),
        new RecipeManager.RecipeLayout(
            " p ",
            "rfr",
            " s "
        ),
        new RecipeManager.RecipeIndex('p', getIngredientForProgrammer(TBItems.pipeResult)),
        new RecipeManager.RecipeIndex('r', "dustRedstone"),
        new RecipeManager.RecipeIndex('f', LPItems.chipFPGA),
        new RecipeManager.RecipeIndex('s', LPItems.pipeBasic)
    );

    // Crafting Manager pipe
    RecipeManager.craftingManager.addRecipe(new ItemStack(TBItems.pipeCraftingManager),
        new RecipeManager.RecipeLayout(
            "fpf",
            "bsb",
            "fdf"
        ),
        new RecipeManager.RecipeIndex('b', LPItems.chipAdvanced),
        new RecipeManager.RecipeIndex('p', getIngredientForProgrammer(TBItems.pipeCraftingManager)),
        new RecipeManager.RecipeIndex('s', LPItems.pipeCrafting),
        new RecipeManager.RecipeIndex('d', "gemDiamond"),
        new RecipeManager.RecipeIndex('f', LPItems.chipFPGA)
    );

    // Buffer Upgrade
    RecipeManager.craftingManager.addRecipe(new ItemStack(TBItems.upgradeBuffer, 1),
        new RecipeManager.RecipeLayout(
            "rpr",
            "gag",
            "qnq"
        ),
        new RecipeManager.RecipeIndex('r', "dustRedstone"),
        new RecipeManager.RecipeIndex('p', getIngredientForProgrammer(TBItems.upgradeBuffer)),
        new RecipeManager.RecipeIndex('g', "ingotGold"),
        new RecipeManager.RecipeIndex('a', LPItems.chipAdvanced),
        new RecipeManager.RecipeIndex('q', "paper"),
        new RecipeManager.RecipeIndex('n', "nuggetGold")
    );
  }

  private static Ingredient getIngredientForProgrammer(Item targetPipe) {
    ItemStack programmerStack = new ItemStack(LPItems.logisticsProgrammer);
    programmerStack.setTagCompound(new NBTTagCompound());
    programmerStack.getTagCompound().setString("LogisticsRecipeTarget", targetPipe.toString());
    return NBTIngredient.fromStacks(programmerStack);
  }

}
