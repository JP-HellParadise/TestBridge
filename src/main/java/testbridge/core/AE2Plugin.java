package testbridge.core;

import appeng.api.AEPlugin;
import appeng.api.IAppEngApi;
import appeng.api.definitions.IMaterials;
import appeng.core.Api;
import appeng.core.features.AEFeature;
import appeng.core.features.ItemStackSrc;
import appeng.integration.IntegrationType;
import appeng.items.parts.ItemPart;
import appeng.items.parts.PartType;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.util.EnumHelper;

import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.ShapedOreRecipe;

import testbridge.part.PartSatelliteBus;

import java.util.EnumSet;
import java.util.Set;

@AEPlugin
public class AE2Plugin {
  public static AE2Plugin INSTANCE;
  public final IAppEngApi api;
  public static PartType SATELLITE_BUS;
  public static ItemStackSrc SATELLITE_BUS_SRC;

  public AE2Plugin(IAppEngApi api) {
    this.api = api;
    INSTANCE = this;
  }
  public static void preInit() {
    AE2Plugin.SATELLITE_BUS = EnumHelper.addEnum(PartType.class, "SATELLITE_BUS", new Class[]{int.class, String.class, Set.class, Set.class, Class.class},
        1024, "satellite_bus", EnumSet.of( AEFeature.CRAFTING_CPU ), EnumSet.noneOf( IntegrationType.class ), PartSatelliteBus.class);
    Api.INSTANCE.getPartModels().registerModels(AE2Plugin.SATELLITE_BUS.getModels());
    AE2Plugin.SATELLITE_BUS_SRC = ItemPart.instance.createPart(AE2Plugin.SATELLITE_BUS);
  }

  public static void loadRecipes(ResourceLocation group) {
    IMaterials materials = AE2Plugin.INSTANCE.api.definitions().materials();
    // Satellite Bus
    ForgeRegistries.RECIPES.register(new ShapedOreRecipe(group, AE2Plugin.SATELLITE_BUS_SRC.stack(1),
        " c ",
                "ifi",
                " p ",
        'p', Blocks.PISTON,
        'f', materials.formationCore().maybeStack(1).orElse(ItemStack.EMPTY),
        'i', "ingotIron",
        'c', materials.calcProcessor().maybeStack(1).orElse(ItemStack.EMPTY)).
        setRegistryName(new ResourceLocation(TestBridge.ID, "recipes/satellite_bus")));

    // Item Package
    ForgeRegistries.RECIPES.register(new ShapedOreRecipe(group, new ItemStack(TBItems.itemPackage),
        "pw",
        'p', Items.PAPER,
        'w', "plankWood").
        setRegistryName(new ResourceLocation(TestBridge.ID, "recipes/item_package")));
  }

  @SideOnly(Side.CLIENT)
  public static void loadModels() {
    Minecraft meme = Minecraft.getMinecraft();
    meme.getRenderItem().getItemModelMesher().register(ItemPart.instance, 1024, AE2Plugin.SATELLITE_BUS.getItemModels().get(0));
    ModelLoader.setCustomModelResourceLocation(ItemPart.instance, 1024, AE2Plugin.SATELLITE_BUS.getItemModels().get(0));
  }
}
