package net.jp.hellparadise.testbridge.integration.modules.appliedenergistics2;

import java.lang.reflect.Field;
import java.util.*;

import net.jp.hellparadise.testbridge.container.ContainerCraftingManager;
import net.jp.hellparadise.testbridge.container.ContainerSatelliteSelect;
import net.jp.hellparadise.testbridge.core.Reference;
import net.jp.hellparadise.testbridge.core.TB_ItemHandlers;
import net.jp.hellparadise.testbridge.core.TestBridge;
import net.jp.hellparadise.testbridge.helpers.interfaces.IBlocks_TB;
import net.jp.hellparadise.testbridge.helpers.interfaces.ICraftingManagerHost;
import net.jp.hellparadise.testbridge.helpers.inventory.HideFakeItem;
import net.jp.hellparadise.testbridge.integration.IIntegrationModule;
import net.jp.hellparadise.testbridge.part.PartCraftingManager;
import net.jp.hellparadise.testbridge.part.PartSatelliteBus;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import appeng.api.AEPlugin;
import appeng.api.IAppEngApi;
import appeng.api.config.SecurityPermissions;
import appeng.api.definitions.IBlocks;
import appeng.api.definitions.IMaterials;
import appeng.api.storage.data.IAEItemStack;
import appeng.bootstrap.FeatureFactory;
import appeng.bootstrap.IBootstrapComponent;
import appeng.bootstrap.components.IModelRegistrationComponent;
import appeng.bootstrap.components.ItemVariantsComponent;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.me.ItemRepo;
import appeng.core.Api;
import appeng.core.features.AEFeature;
import appeng.core.features.ItemStackSrc;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.GuiHostType;
import appeng.integration.IntegrationType;
import appeng.items.parts.ItemPart;
import appeng.items.parts.PartType;
import appeng.util.prioritylist.IPartitionList;
import appeng.util.prioritylist.MergedPriorityList;

@AEPlugin
public class AE2Module implements IIntegrationModule {

    public static AE2Module INSTANCE;
    public final IAppEngApi api;
    public static HideFakeItem HIDE_FAKE_ITEM;
    public static Field MergedPriorityList_negative, GuiMEMonitorable_Repo, ItemRepo_myPartitionList;
    public static PartType SATELLITE_BUS, CRAFTINGMANAGER_PART;
    public static ItemStackSrc SATELLITE_BUS_SRC, CRAFTINGMANAGER_PART_SRC;
    public static GuiBridge GUI_CRAFTINGMANAGER, GUI_SATELLITESELECT;
    public static ItemStack tile_cm;

    public AE2Module(IAppEngApi api) {
        this.api = api;
        INSTANCE = this;
    }

    public void preInit() {
        try {
            AE2Module.MergedPriorityList_negative = MergedPriorityList.class.getDeclaredField("negative");
            AE2Module.MergedPriorityList_negative.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
        }

        // Register Part
        AE2Module.SATELLITE_BUS = EnumHelper.addEnum(
            PartType.class,
            "SATELLITE_BUS",
            new Class[] { int.class, String.class, Set.class, Set.class, Class.class },
            1024,
            "satellite_bus",
            EnumSet.of(AEFeature.CRAFTING_CPU),
            EnumSet.noneOf(IntegrationType.class),
            PartSatelliteBus.class);
        Api.INSTANCE.getPartModels()
            .registerModels(AE2Module.SATELLITE_BUS.getModels());
        AE2Module.SATELLITE_BUS_SRC = ItemPart.instance.createPart(AE2Module.SATELLITE_BUS);

        AE2Module.CRAFTINGMANAGER_PART = EnumHelper.addEnum(
            PartType.class,
            "CRAFTINGMANAGER_PART",
            new Class[] { int.class, String.class, Set.class, Set.class, Class.class },
            1025,
            "craftingmanager_part",
            EnumSet.of(AEFeature.INTERFACE),
            EnumSet.noneOf(IntegrationType.class),
            PartCraftingManager.class);
        Api.INSTANCE.getPartModels()
            .registerModels(AE2Module.CRAFTINGMANAGER_PART.getModels());
        AE2Module.CRAFTINGMANAGER_PART_SRC = ItemPart.instance.createPart(AE2Module.CRAFTINGMANAGER_PART);

        // Register GUI
        AE2Module.GUI_CRAFTINGMANAGER = EnumHelper.addEnum(
            GuiBridge.class,
            "GUI_CRAFTINGMANAGER",
            new Class[] { Class.class, Class.class, GuiHostType.class, SecurityPermissions.class },
            ContainerCraftingManager.class,
            ICraftingManagerHost.class,
            GuiHostType.WORLD,
            SecurityPermissions.BUILD);

        AE2Module.GUI_SATELLITESELECT = EnumHelper.addEnum(
            GuiBridge.class,
            "GUI_SATELLITESELECT",
            new Class[] { Class.class, Class.class, GuiHostType.class, SecurityPermissions.class },
            ContainerSatelliteSelect.class,
            ICraftingManagerHost.class,
            GuiHostType.WORLD,
            SecurityPermissions.BUILD);

        // PreInit event handler
        MinecraftForge.EVENT_BUS.register(AE2EventHandler.preInit.class);

        if (FMLLaunchHandler.side() == Side.CLIENT) { // Client only
            this.registerRenderers();
        }
    }

    public void init() {
        if (FMLLaunchHandler.side() == Side.CLIENT) { // Client only
            // Hacking to the terminal
            try {
                GuiMEMonitorable_Repo = GuiMEMonitorable.class.getDeclaredField("repo");
                GuiMEMonitorable_Repo.setAccessible(true);
                ItemRepo_myPartitionList = ItemRepo.class.getDeclaredField("myPartitionList");
                ItemRepo_myPartitionList.setAccessible(true);
            } catch (SecurityException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        // Init event handler
        MinecraftForge.EVENT_BUS.register(AE2EventHandler.init.class);

        tile_cm = ((IBlocks_TB) AE2Module.INSTANCE.api.definitions()
            .blocks()).cmBlock()
                .maybeStack(1)
                .orElse(ItemStack.EMPTY);

        AE2Module.loadRecipes();
    }

    // Register recipe
    private static void loadRecipes() {
        // Recipe group
        ResourceLocation group = new ResourceLocation(Reference.MODID, "recipes");
        // AE2 API definitions
        IMaterials materials = AE2Module.INSTANCE.api.definitions()
            .materials();
        IBlocks blocks = AE2Module.INSTANCE.api.definitions()
            .blocks();
        // Satellite Bus
        ForgeRegistries.RECIPES.register(
            new ShapedOreRecipe(
                group,
                AE2Module.SATELLITE_BUS_SRC.stack(1),
                " c ",
                "ifi",
                " p ",
                'p',
                Blocks.PISTON,
                'f',
                materials.formationCore()
                    .maybeStack(1)
                    .orElse(ItemStack.EMPTY),
                'i',
                "ingotIron",
                'c',
                materials.calcProcessor()
                    .maybeStack(1)
                    .orElse(ItemStack.EMPTY))
                        .setRegistryName(new ResourceLocation(Reference.MODID, "recipes/satellite_bus")));

        // ME Crafting Manager part
        ForgeRegistries.RECIPES.register(
            new ShapelessOreRecipe(group, AE2Module.CRAFTINGMANAGER_PART_SRC.stack(1), /* Input */ tile_cm)
                .setRegistryName(new ResourceLocation(Reference.MODID, "recipes/cm_block_to_part")));

        ForgeRegistries.RECIPES.register(
            new ShapelessOreRecipe(group, tile_cm, /* Input */ AE2Module.CRAFTINGMANAGER_PART_SRC.stack(1))
                .setRegistryName(new ResourceLocation(Reference.MODID, "recipes/cm_part_to_block")));

        // ME Crafting Manager block
        ForgeRegistries.RECIPES.register(
            new ShapedOreRecipe(
                group,
                tile_cm,
                "dud",
                "fca",
                "lIl",
                'd',
                materials.engProcessor()
                    .maybeStack(1)
                    .orElse(ItemStack.EMPTY),
                'f',
                materials.formationCore()
                    .maybeStack(1)
                    .orElse(ItemStack.EMPTY),
                'a',
                materials.annihilationCore()
                    .maybeStack(1)
                    .orElse(ItemStack.EMPTY),
                'c',
                materials.calcProcessor()
                    .maybeStack(1)
                    .orElse(ItemStack.EMPTY),
                'u',
                materials.cardPatternExpansion()
                    .maybeStack(1)
                    .orElse(ItemStack.EMPTY),
                'I',
                blocks.iface()
                    .maybeStack(1)
                    .orElse(ItemStack.EMPTY),
                'l',
                materials.logicProcessor()
                    .maybeStack(1)
                    .orElse(ItemStack.EMPTY))
                        .setRegistryName(new ResourceLocation(Reference.MODID, "recipes/craftingmanager_part")));

        // Item Package
        ForgeRegistries.RECIPES.register(
            new ShapedOreRecipe(
                group,
                new ItemStack(TB_ItemHandlers.itemPackage),
                "pw",
                'p',
                Items.PAPER,
                'w',
                "plankWood").setRegistryName(new ResourceLocation(Reference.MODID, "recipes/item_package")));
    }

    // Register item model
    @SuppressWarnings("unchecked")
    private void registerRenderers() {
        TestBridge.log.info("Loading AE2 Renderers");
        try {
            FeatureFactory ff = Api.INSTANCE.definitions()
                .getRegistry();
            Field bootstrapComponentsF = FeatureFactory.class.getDeclaredField("bootstrapComponents");
            bootstrapComponentsF.setAccessible(true);
            Map<Class<? extends IBootstrapComponent>, List<IBootstrapComponent>> bootstrapComponents = (Map<Class<? extends IBootstrapComponent>, List<IBootstrapComponent>>) bootstrapComponentsF
                .get(ff);
            List<IBootstrapComponent> itemRegComps = bootstrapComponents.get(IModelRegistrationComponent.class);
            ItemVariantsComponent partReg = null;
            Field ItemVariantsComponent_item = ItemVariantsComponent.class.getDeclaredField("item");
            ItemVariantsComponent_item.setAccessible(true);
            for (IBootstrapComponent iBootstrapComponent : itemRegComps) {
                if (iBootstrapComponent instanceof ItemVariantsComponent) {
                    Item item = (Item) ItemVariantsComponent_item.get(iBootstrapComponent);
                    if (item == ItemPart.instance) {
                        partReg = (ItemVariantsComponent) iBootstrapComponent;
                        break;
                    }
                }
            }
            Field ItemVariantsComponent_resources = ItemVariantsComponent.class.getDeclaredField("resources");
            ItemVariantsComponent_resources.setAccessible(true);
            HashSet<ResourceLocation> resources = (HashSet<ResourceLocation>) ItemVariantsComponent_resources
                .get(partReg);
            resources.addAll(AE2Module.SATELLITE_BUS.getItemModels());
            resources.addAll(AE2Module.CRAFTINGMANAGER_PART.getItemModels());
        } catch (Exception e) {
            throw new RuntimeException("Error registering part model", e);
        }
    }

    // Hacking stuff
    @SuppressWarnings("unchecked")
    @SideOnly(Side.CLIENT)
    static void hideFakeItems(GuiScreenEvent.BackgroundDrawnEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen instanceof GuiMEMonitorable) {
            GuiMEMonitorable g = (GuiMEMonitorable) mc.currentScreen;
            if (AE2Module.HIDE_FAKE_ITEM == null) {
                AE2Module.HIDE_FAKE_ITEM = new HideFakeItem();
            }
            try {
                ItemRepo r = (ItemRepo) GuiMEMonitorable_Repo.get(g);
                IPartitionList<IAEItemStack> pl = (IPartitionList<IAEItemStack>) ItemRepo_myPartitionList.get(r);
                if (pl instanceof MergedPriorityList) {
                    MergedPriorityList<IAEItemStack> ml = (MergedPriorityList<IAEItemStack>) pl;
                    Collection<IPartitionList<IAEItemStack>> negative = (Collection<IPartitionList<IAEItemStack>>) AE2Module.MergedPriorityList_negative
                        .get(ml);
                    if (!negative.contains(AE2Module.HIDE_FAKE_ITEM)) {
                        negative.add(AE2Module.HIDE_FAKE_ITEM);
                        r.updateView();
                    }
                } else {
                    MergedPriorityList<IAEItemStack> mList = new MergedPriorityList<>();
                    ItemRepo_myPartitionList.set(r, mList);
                    if (pl != null) mList.addNewList(pl, true);
                    mList.addNewList(AE2Module.HIDE_FAKE_ITEM, false);
                    r.updateView();
                }
            } catch (Exception ignore) {}
        }
    }
}
