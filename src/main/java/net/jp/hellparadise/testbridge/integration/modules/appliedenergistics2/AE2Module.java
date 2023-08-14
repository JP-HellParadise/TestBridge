package net.jp.hellparadise.testbridge.integration.modules.appliedenergistics2;

import appeng.api.AEPlugin;
import appeng.api.IAppEngApi;
import appeng.api.definitions.IBlocks;
import appeng.api.definitions.IMaterials;
import appeng.api.definitions.IParts;
import net.jp.hellparadise.testbridge.core.Reference;
import net.jp.hellparadise.testbridge.helpers.interfaces.ae2.AccessorApiBlocks;
import net.jp.hellparadise.testbridge.helpers.interfaces.ae2.AccessorApiParts;
import net.jp.hellparadise.testbridge.helpers.inventory.HideFakeItem;
import net.jp.hellparadise.testbridge.integration.IIntegrationModule;
import net.jp.hellparadise.testbridge.items.FakeItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

@AEPlugin
public class AE2Module implements IIntegrationModule {

    public static AE2Module INSTANCE;
    public final IAppEngApi api;
    public static HideFakeItem HIDE_FAKE_ITEM;

    public AE2Module(IAppEngApi api) {
        this.api = api;
        INSTANCE = this;
    }

    public void preInit() {
        // PreInit event handler
        MinecraftForge.EVENT_BUS.register(AE2EventHandler.PreInit.class);
    }

    public void init() {
        // Init event handler
        MinecraftForge.EVENT_BUS.register(AE2EventHandler.Init.class);

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
        IParts parts = AE2Module.INSTANCE.api.definitions().parts();
        // Satellite Bus
        ForgeRegistries.RECIPES.register(
            new ShapedOreRecipe(
                group,
                ((AccessorApiParts) AE2Module.INSTANCE.api.definitions()
                        .parts()).satelliteBus()
                        .maybeStack(1)
                        .orElse(ItemStack.EMPTY),
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
            new ShapelessOreRecipe(group, ((AccessorApiParts) parts).craftingManager()
                    .maybeStack(1)
                    .orElse(ItemStack.EMPTY), /* Input */ ((AccessorApiBlocks) blocks).cmBlock().maybeStack(1)
                    .orElse(ItemStack.EMPTY))
                .setRegistryName(new ResourceLocation(Reference.MODID, "recipes/cm_block_to_part")));

        ForgeRegistries.RECIPES.register(
            new ShapelessOreRecipe(group, ((AccessorApiBlocks) blocks).cmBlock().maybeStack(1)
                    .orElse(ItemStack.EMPTY), /* Input */ ((AccessorApiParts) parts).craftingManager()
                .maybeStack(1)
                .orElse(ItemStack.EMPTY))
                .setRegistryName(new ResourceLocation(Reference.MODID, "recipes/cm_part_to_block")));

        // ME Crafting Manager block
        ForgeRegistries.RECIPES.register(
            new ShapedOreRecipe(
                group,
                    ((AccessorApiBlocks) blocks).cmBlock().maybeStack(1)
                            .orElse(ItemStack.EMPTY),
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
                new ItemStack(FakeItem.ITEM_PACKAGE),
                "pw",
                'p',
                Items.PAPER,
                'w',
                "plankWood").setRegistryName(new ResourceLocation(Reference.MODID, "recipes/item_package")));
    }
}
