package testbridge.core;

import java.io.File;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import lombok.Getter;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.registries.IForgeRegistry;

import logisticspipes.items.ItemUpgrade;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;

import testbridge.helpers.datafixer.TBDataFixer;
import testbridge.integration.IntegrationRegistry;
import testbridge.integration.IntegrationType;
import testbridge.network.GuiHandler;
import testbridge.part.PartSatelliteBus;
import testbridge.pipes.PipeCraftingManager;
import testbridge.pipes.ResultPipe;
import testbridge.pipes.upgrades.BufferCMUpgrade;

@Mod(modid = TestBridge.MODID, name = TestBridge.NAME, version = TestBridge.VERSION, dependencies = TestBridge.DEPS, guiFactory = "testbridge.client.gui.config.ConfigGuiFactory", acceptedMinecraftVersions = "1.12.2")
public class TestBridge {

  public static final String MODID = "testbridge";
  public static final String NAME = "Test Bridge";
  public static final String VERSION = "@VERSION@";
  public static final String DEPS = "after:appliedenergistics2;after:refinedstorage@[1.6.15,);required-after:mixinbooter@[4.2,);required-after:logisticspipes@[0.10.4.,);";

  @Getter
  private static final boolean debug = Boolean.getBoolean("tb.debugging");

  public TestBridge() {
    MinecraftForge.EVENT_BUS.register(this);
  }

  @Mod.Instance(TestBridge.MODID)
  public static TestBridge INSTANCE;

  public static final Logger log = LogManager.getLogger(NAME);

  @Mod.EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    final Stopwatch watch = Stopwatch.createStarted();
    log.info("==================================================================================");
    log.info("Start Pre Initialization");

    log.info("Initial config file");
    final File configFile = new File(event.getModConfigurationDirectory().getPath(), "TestBridge.cfg");
    TB_Config.init(configFile);

    for (final IntegrationType type : IntegrationType.values()) {
      IntegrationRegistry.INSTANCE.add(type);
    }

    IntegrationRegistry.INSTANCE.preInit();

    log.info("Pre Initialization took in {} ms", watch.elapsed(TimeUnit.MILLISECONDS));
    log.info("==================================================================================");
  }

  @Mod.EventHandler
  public void init(FMLInitializationEvent evt) {
    final Stopwatch watch = Stopwatch.createStarted();
    log.info("==================================================================================");
    log.info("Start Initialization");

    NetworkRegistry.INSTANCE.registerGuiHandler(TestBridge.INSTANCE, new GuiHandler());
    TBDataFixer.INSTANCE.init();
    IntegrationRegistry.INSTANCE.init();

    log.info("Initialization took in {} ms", watch.elapsed(TimeUnit.MILLISECONDS));
    log.info("==================================================================================");
  }

  @Mod.EventHandler
  public void postInit(FMLPostInitializationEvent event) {
    final Stopwatch watch = Stopwatch.createStarted();
    log.info("==================================================================================");
    log.info("Start Post Initialization");

    IntegrationRegistry.INSTANCE.postInit();

    log.info("Saving Config file");
    TB_Config.instance().save();

    log.info("Post Initialization took in {} ms", watch.elapsed(TimeUnit.MILLISECONDS));
    log.info("==================================================================================");
  }

  @SubscribeEvent
  public void initItems(RegistryEvent.Register<Item> event) {
    IForgeRegistry<Item> registry = event.getRegistry();
    //Items
    if (IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.APPLIED_ENERGISTICS_2)) {
      registry.register(TB_ItemHandlers.itemHolder);
      registry.register(TB_ItemHandlers.itemPackage);
      registry.register(TB_ItemHandlers.virtualPattern);
    }
    // Pipe
    LogisticsBlockGenericPipe.registerPipe(registry, "result", ResultPipe::new);
    LogisticsBlockGenericPipe.registerPipe(registry, "crafting_manager", PipeCraftingManager::new);
    // Upgrade
    ItemUpgrade.registerUpgrade(registry, BufferCMUpgrade.getName(), BufferCMUpgrade::new);
  }

  @SubscribeEvent
  public void initBlocks(RegistryEvent.Register<Block> event) {
    IForgeRegistry<Block> registry = event.getRegistry();
    // TODO Block
  }

  @Mod.EventHandler
  public void cleanup(FMLServerStoppingEvent event) {
    ResultPipe.cleanup();
    if (IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.APPLIED_ENERGISTICS_2)) {
      PartSatelliteBus.cleanup();
    }
  }
}
