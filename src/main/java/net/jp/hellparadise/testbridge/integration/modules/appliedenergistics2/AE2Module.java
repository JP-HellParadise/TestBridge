package net.jp.hellparadise.testbridge.integration.modules.appliedenergistics2;

import java.lang.invoke.MethodHandle;
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
import net.jp.hellparadise.testbridge.utils.Reflector;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
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
    public static PartType SATELLITE_BUS, CRAFTINGMANAGER_PART;
    public static ItemStackSrc SATELLITE_BUS_SRC, CRAFTINGMANAGER_PART_SRC;
    public static GuiBridge GUI_CRAFTINGMANAGER, GUI_SATELLITESELECT;
    public static ItemStack tile_cm;

    public static final MethodHandle GuiMEMonitorable_RepoGetter;
    public static final MethodHandle ItemRepo_MyPartitionListGetter;
    public static final MethodHandle ItemRepo_MyPartitionListSetter;
    public static final MethodHandle bootstrapComponentsF;
    public static final MethodHandle ItemVariantsComponent_Item;
    public static final MethodHandle ItemVariantsComponent_Resources;

    static {
        try {
            // "I reject performance" - Korewa_Li
            GuiMEMonitorable_RepoGetter = Reflector.resolveFieldGetter(GuiMEMonitorable.class, "repo");
            ItemRepo_MyPartitionListGetter = Reflector.resolveFieldGetter(ItemRepo.class, "myPartitionList");
            ItemRepo_MyPartitionListSetter = Reflector.resolveFieldSetter(ItemRepo.class, "myPartitionList");
            bootstrapComponentsF = Reflector.resolveFieldGetter(FeatureFactory.class, "bootstrapComponents");
            ItemVariantsComponent_Item = Reflector.resolveFieldGetter(ItemVariantsComponent.class, "item");
            ItemVariantsComponent_Resources = Reflector.resolveFieldGetter(ItemVariantsComponent.class, "resources");
        } catch (SecurityException se) {
            throw new RuntimeException(se);
        }
    }

    public AE2Module(IAppEngApi api) {
        this.api = api;
        INSTANCE = this;
    }

    public void preInit() {
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
            Map<Class<? extends IBootstrapComponent>, List<IBootstrapComponent>> bootstrapComponents = (Map<Class<? extends IBootstrapComponent>, List<IBootstrapComponent>>) bootstrapComponentsF
                .invoke(ff);
            List<IBootstrapComponent> itemRegComps = bootstrapComponents.get(IModelRegistrationComponent.class);
            ItemVariantsComponent partReg = null;
            for (IBootstrapComponent iBootstrapComponent : itemRegComps) {
                if (iBootstrapComponent instanceof ItemVariantsComponent) {
                    Item item = (Item) ItemVariantsComponent_Item.invoke(iBootstrapComponent);
                    if (item == ItemPart.instance) {
                        partReg = (ItemVariantsComponent) iBootstrapComponent;
                        break;
                    }
                }
            }
            HashSet<ResourceLocation> resources = (HashSet<ResourceLocation>) ItemVariantsComponent_Resources
                .invoke(partReg);
            resources.addAll(AE2Module.SATELLITE_BUS.getItemModels());
            resources.addAll(AE2Module.CRAFTINGMANAGER_PART.getItemModels());
        } catch (Throwable e) {
            throw new RuntimeException("Error registering part model", e);
        }
    }

    // Hacking stuff
    @SuppressWarnings("unchecked")
    @SideOnly(Side.CLIENT)
    static void hideFakeItems() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen instanceof GuiMEMonitorable) {
            GuiMEMonitorable gui = (GuiMEMonitorable) mc.currentScreen;
            if (AE2Module.HIDE_FAKE_ITEM == null) AE2Module.HIDE_FAKE_ITEM = new HideFakeItem();
            try {
                ItemRepo guiItemRepo = (ItemRepo) GuiMEMonitorable_RepoGetter.invoke(gui);
                IPartitionList<IAEItemStack> partList = (IPartitionList<IAEItemStack>) ItemRepo_MyPartitionListGetter
                    .invoke(guiItemRepo);
                if (partList instanceof MergedPriorityList) {
                    MergedPriorityList<IAEItemStack> ml = (MergedPriorityList<IAEItemStack>) partList;
                    if (AE2Module.HIDE_FAKE_ITEM.getStreams()
                        .allMatch(ml::isListed)) {
                        ml.addNewList(AE2Module.HIDE_FAKE_ITEM, false);
                        guiItemRepo.updateView();
                    }
                } else {
                    MergedPriorityList<IAEItemStack> newMList = new MergedPriorityList<>();
                    ItemRepo_MyPartitionListSetter.invoke(guiItemRepo, newMList);
                    if (partList != null) newMList.addNewList(partList, true);
                    newMList.addNewList(AE2Module.HIDE_FAKE_ITEM, false);
                    guiItemRepo.updateView();
                }
            } catch (Throwable ignored) {}
        }
    }
}
