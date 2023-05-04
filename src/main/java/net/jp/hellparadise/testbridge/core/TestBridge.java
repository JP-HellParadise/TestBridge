package net.jp.hellparadise.testbridge.core;

import java.io.File;
import java.util.concurrent.TimeUnit;

import logisticspipes.items.ItemUpgrade;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;

import net.jp.hellparadise.testbridge.datafixer.TBDataFixer;
import net.jp.hellparadise.testbridge.integration.IntegrationRegistry;
import net.jp.hellparadise.testbridge.integration.IntegrationType;
import net.jp.hellparadise.testbridge.network.guis.GuiHandler;
import net.jp.hellparadise.testbridge.network.packets.MessagePlayer;
import net.jp.hellparadise.testbridge.part.PartSatelliteBus;
import net.jp.hellparadise.testbridge.pipes.PipeCraftingManager;
import net.jp.hellparadise.testbridge.pipes.ResultPipe;
import net.jp.hellparadise.testbridge.pipes.upgrades.BufferCMUpgrade;
import net.jp.hellparadise.testbridge.proxy.Proxy;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.registries.IForgeRegistry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Stopwatch;

@Mod(
    modid = Reference.MODID,
    name = Reference.MODNAME,
    version = Reference.VERSION,
    dependencies = TestBridge.DEPS,
    acceptedMinecraftVersions = "1.12.2")
public class TestBridge {

    public static final String DEPS = "after:appliedenergistics2;after:refinedstorage@[1.6.15,);required-after:mixinbooter@[4.2,);required-after:logisticspipes;";

    @SidedProxy(
        modId = Reference.MODID,
        clientSide = "net.jp.hellparadise.testbridge.proxy.ClientProxy",
        serverSide = "net.jp.hellparadise.testbridge.proxy.ServerProxy")
    private static Proxy PROXY = null;

    public static Proxy getProxy() {
        return PROXY;
    }

    @Mod.Instance(Reference.MODID)
    public static TestBridge INSTANCE;

    public TestBridge() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static final Logger log = LogManager.getLogger(Reference.MODNAME);
    private SimpleNetworkWrapper network;

    public static SimpleNetworkWrapper getNetwork() {
        return INSTANCE.network;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        final Stopwatch watch = Stopwatch.createStarted();
        log.info("==================================================================================");
        log.info("Start Pre Initialization");

        log.info("Initial config file");
        final File configFile = new File(
            event.getModConfigurationDirectory()
                .getPath(),
            "TestBridge.cfg");
        TB_Config.init(configFile);

        log.info("Register network channel");
        this.initialNetwork();

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
        TB_Config.instance()
            .save();

        log.info("Post Initialization took in {} ms", watch.elapsed(TimeUnit.MILLISECONDS));
        log.info("==================================================================================");
    }

    @SubscribeEvent
    public void initItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();
        // Items
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

    private void initialNetwork() {
        network = NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MODID);
        int id = 0;

        // Register network stuff from now on
        network.registerMessage(MessagePlayer.Handler.class, MessagePlayer.class, id, Side.CLIENT);
    }
}
