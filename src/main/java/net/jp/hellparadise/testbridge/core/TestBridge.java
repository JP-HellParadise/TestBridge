package net.jp.hellparadise.testbridge.core;

import com.google.common.base.Stopwatch;
import java.io.File;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import logisticspipes.items.ItemUpgrade;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import net.jp.hellparadise.testbridge.datafixer.TBDataFixer;
import net.jp.hellparadise.testbridge.integration.IntegrationRegistry;
import net.jp.hellparadise.testbridge.integration.IntegrationType;
import net.jp.hellparadise.testbridge.items.FakeItem;
import net.jp.hellparadise.testbridge.items.VirtualPatternAE;
import net.jp.hellparadise.testbridge.network.packets.implementation.CManagerMenuSwitch;
import net.jp.hellparadise.testbridge.network.packets.implementation.MessagePlayer;
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
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = Reference.MOD_ID,
    name = Reference.MOD_NAME,
    version = Reference.VERSION,
    dependencies = TestBridge.DEPS)
public class TestBridge {

    public static final String DEPS =
            "after:logisticspipes@[0.10.4.44,);after:appliedenergistics2;after:refinedstorage@[1.6.15,);" +
                    "required-after:mixinbooter@[8.2,);required-after:modularui@[2.1,);";

    public static final boolean isVMOpenJ9 = SystemUtils.JAVA_VM_NAME.toLowerCase(Locale.ROOT)
        .contains("openj9");

    @SidedProxy(
        modId = Reference.MOD_ID,
        clientSide = "net.jp.hellparadise.testbridge.proxy.ClientProxy",
        serverSide = "net.jp.hellparadise.testbridge.proxy.ServerProxy")
    private static Proxy PROXY = null;

    public static Proxy getProxy() {
        return PROXY;
    }

    @Mod.Instance(Reference.MOD_ID)
    public static TestBridge INSTANCE;

    public TestBridge() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static final Logger log = LogManager.getLogger(Reference.MOD_NAME);
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
            registry.register(FakeItem.ITEM_HOLDER);
            registry.register(FakeItem.ITEM_PACKAGE);
            registry.register(VirtualPatternAE.VIRTUAL_PATTERN);
        }
        if (IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.LOGISTICS_PIPES)) {
            // Pipe
            LogisticsBlockGenericPipe.registerPipe(registry, "result", ResultPipe::new);
            LogisticsBlockGenericPipe.registerPipe(registry, "crafting_manager", PipeCraftingManager::new);
            // Upgrade
            ItemUpgrade.registerUpgrade(registry, BufferCMUpgrade.getName(), BufferCMUpgrade::new);
        }
    }

    @SubscribeEvent
    public void initBlocks(RegistryEvent.Register<Block> event) {
        IForgeRegistry<Block> registry = event.getRegistry();
        // TODO Block
    }

    @Mod.EventHandler
    public void cleanup(FMLServerStoppingEvent event) {
        if (IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.LOGISTICS_PIPES)) {
            ResultPipe.cleanup();
        }
        if (IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.APPLIED_ENERGISTICS_2)) {
            PartSatelliteBus.cleanup();
        }
    }

    private void initialNetwork() {
        network = NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MOD_ID);
        int id = 0;

        // Client packet
        network.registerMessage(MessagePlayer.Handler.class, MessagePlayer.class, id++, Side.CLIENT);

        // Server packet
        network.registerMessage(CManagerMenuSwitch.Handler.class, CManagerMenuSwitch.class, id++, Side.SERVER);
    }
}
