package testbridge.proxy;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
import net.minecraft.client.Minecraft;

import testbridge.core.AE2Plugin;
import testbridge.core.TestBridge;



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
      addRenderToRegistry(item, 0, item.getTranslationKey().substring(5));
    }
    renderers = null;
    if(TestBridge.isAELoaded()){
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
          if(iBootstrapComponent instanceof ItemVariantsComponent){
            Item item = (Item) ItemVariantsComponent_item.get(iBootstrapComponent);
            if(item == ItemPart.instance){
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

  @Override
  public void addRenderer(ItemStack is, String name) {
    addRenderToRegistry(is.getItem(), is.getItemDamage(), name);
  }

  @Override
  public void addRenderer(Item item, String name) {
    addRenderToRegistry(item, 0, name);
  }

  private static void addRenderToRegistry(Item item, int meta, String name) {
    ModelLoader.setCustomModelResourceLocation(item, meta, new ModelResourceLocation(new ResourceLocation(TestBridge.ID, name), "inventory"));
  }

  @Override
  public void addRenderer(Item item) {
    renderers.add(item);
  }

  @Override
  public void registerTextures() {
    TestBridge.TBTextures.registerBlockIcons(Minecraft.getMinecraft().getTextureMapBlocks());
  }

  @SubscribeEvent
  public void loadModels(ModelRegistryEvent event) {
    if (TestBridge.isAELoaded()) {
      AE2Plugin.loadModels();
    }
  }
}
