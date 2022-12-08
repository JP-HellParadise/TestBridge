package testbridge.proxy;

import java.lang.reflect.Field;
import java.util.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.bootstrap.FeatureFactory;
import appeng.bootstrap.IBootstrapComponent;
import appeng.bootstrap.components.IModelRegistrationComponent;
import appeng.bootstrap.components.ItemVariantsComponent;
import appeng.core.Api;
import appeng.items.parts.ItemPart;

import testbridge.core.AE2Plugin;
import testbridge.core.TBItems;
import testbridge.core.TestBridge;
import testbridge.utils.MeshDefinitionFix;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
  private List<Item> renderers = new ArrayList<>();

  public void preInit(FMLPreInitializationEvent event) {}

  public void init(FMLInitializationEvent event) {}

  public void postInit(FMLPostInitializationEvent event) {}

  //public static Field
  @SuppressWarnings("unchecked")
  @Override
  public void registerRenderers() {
    TestBridge.log.info("Loading Renderers");
    for (Item item : renderers) {
      ModelResourceLocation local = new ModelResourceLocation(new ResourceLocation(TestBridge.ID, item.getTranslationKey().substring(5)), "inventory");
      addRenderToRegistry(item, 0, local);
    }
    renderers = null;
    if (TestBridge.isAELoaded()) {
      try {
        FeatureFactory ff = Api.INSTANCE.definitions().getRegistry();
        Field bootstrapComponentsF = FeatureFactory.class.getDeclaredField("bootstrapComponents");
        bootstrapComponentsF.setAccessible(true);
        Map<Class<? extends IBootstrapComponent>, List<IBootstrapComponent>> bootstrapComponents = (Map<Class<? extends IBootstrapComponent>, List<IBootstrapComponent>>) bootstrapComponentsF.get(ff);
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
        HashSet<ResourceLocation> resources = (HashSet<ResourceLocation>) ItemVariantsComponent_resources.get(partReg);
        resources.addAll(AE2Plugin.SATELLITE_BUS.getItemModels());
      } catch (Exception e) {
        throw new RuntimeException("Error registering part model", e);
      }
    }
    //OBJLoader.INSTANCE.addDomain(LogisticsBridge.ID);
  }

  private static void addRenderToRegistry(Item item, int meta, ModelResourceLocation local) {
    ModelLoader.setCustomModelResourceLocation(item, meta, local);
    Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, 0, local);
  }

  /**
   * The {@link Item}s that have had models registered so far.
   */
  private final Set<Item> itemsRegistered = new HashSet<>();

  /**
   * Register a single model for an {@link Item}.
   * <p>
   * Uses the registry name as the domain/path and {@code "inventory"} as the variant.
   *
   * @param item The Item
   */
  private void registerItemModel(final Item item) {
    registerItemModel(item, item.getRegistryName().toString());
  }

  /**
   * Register a single model for an {@link Item}.
   * <p>
   * Uses {@code modelLocation} as the domain/path and {@link "inventory"} as the variant.
   *
   * @param item          The Item
   * @param modelLocation The model location
   */
  private void registerItemModel(final Item item, final String modelLocation) {
    final ModelResourceLocation fullModelLocation = new ModelResourceLocation(modelLocation, "inventory");
    registerItemModel(item, fullModelLocation);
  }

  /**
   * Register a single model for an {@link Item}.
   * <p>
   * Uses {@code fullModelLocation} as the domain, path and variant.
   *
   * @param item              The Item
   * @param fullModelLocation The full model location
   */
  private void registerItemModel(final Item item, final ModelResourceLocation fullModelLocation) {
    ModelBakery.registerItemVariants(item, fullModelLocation); // Ensure the custom model is loaded and prevent the default model from being loaded
    registerItemModel(item, MeshDefinitionFix.create(stack -> fullModelLocation));
  }

  /**
   * Register an {@link ItemMeshDefinition} for an {@link Item}.
   *
   * @param item           The Item
   * @param meshDefinition The ItemMeshDefinition
   */
  private void registerItemModel(final Item item, final ItemMeshDefinition meshDefinition) {
    itemsRegistered.add(item);
    ModelLoader.setCustomMeshDefinition(item, meshDefinition);
  }

  @Override
  public void registerTextures() {
    TestBridge.TBTextures.registerBlockIcons(Minecraft.getMinecraft().getTextureMapBlocks());
    registerItemModel(TBItems.itemPackage, "testbridge:item_package");
    registerItemModel(TBItems.itemHolder, "testbridge:item_placeholder");
  }

  @SubscribeEvent
  public void loadModels(ModelRegistryEvent event) {
    if (TestBridge.isAELoaded()) {
      AE2Plugin.loadModels();
    }
  }
}
