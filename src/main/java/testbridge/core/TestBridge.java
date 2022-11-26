package testbridge.core;

import javax.annotation.Nonnull;

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

import logisticspipes.LPItems;
import logisticspipes.LogisticsPipes;
import logisticspipes.items.ItemUpgrade;

import testbridge.datafixer.TBDataFixer;
import testbridge.network.GuiHandler;
import testbridge.pipes.PipeCraftingManager;
import testbridge.pipes.ResultPipe;
import testbridge.pipes.upgrades.BufferCMUpgrade;
import testbridge.proxy.CommonProxy;
import testbridge.textures.Textures;

@Mod(modid = TestBridge.ID, name = TestBridge.NAME, version = TestBridge.VERSION, dependencies = TestBridge.DEPS, guiFactory = "", acceptedMinecraftVersions = "1.12.2")
public class TestBridge extends LogisticsPipes {

  public static final String ID = "testbridge";
  public static final String NAME = "Test Bridge";
  public static final String VERSION = "@VERSION@";
  public static final String DEPS = "after:appliedenergistics2;after:refinedstorage@[1.6.15,);required-after:logisticspipes@[0.10.4.21,)";

  @Getter
  private static boolean debug = true;

  public TestBridge() {
    MinecraftForge.EVENT_BUS.register(this);
  }

  @Mod.Instance("testbridge")
  public static TestBridge instance;

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

  public static Textures textures = new Textures();

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

    proxy.preInit(event);

    AELoaded = Loader.isModLoaded("appliedenergistics2");
    RSLoaded = Loader.isModLoaded("refinedstorage");
    TOPLoaded = Loader.isModLoaded("theoneprobe");



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
    NetworkRegistry.INSTANCE.registerGuiHandler(TestBridge.instance, new GuiHandler());
    TBDataFixer.INSTANCE.init();

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
  }

  @Mod.EventHandler
  public void onServerLoad(FMLServerStartingEvent event) {
  // TODO
  }

}
