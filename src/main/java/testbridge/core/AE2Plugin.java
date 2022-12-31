package testbridge.core;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import appeng.api.AEPlugin;
import appeng.api.config.SecurityPermissions;
import appeng.api.definitions.IBlocks;
import appeng.api.definitions.IMaterials;
import appeng.api.IAppEngApi;
import appeng.api.storage.data.IAEItemStack;
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

import testbridge.container.ContainerCraftingManager;
import testbridge.container.ContainerSatelliteSelect;
import testbridge.helpers.HideFakeItem;
import testbridge.helpers.interfaces.ICraftingManagerHost;
import testbridge.part.PartCraftingManager;
import testbridge.part.PartSatelliteBus;
import testbridge.proxy.ClientProxy;

@AEPlugin
public class AE2Plugin {
  public static AE2Plugin INSTANCE;
  public final IAppEngApi api;
  public static HideFakeItem HIDE_FAKE_ITEM;
  public static Field MergedPriorityList_negative;
  public static PartType SATELLITE_BUS;
  public static ItemStackSrc SATELLITE_BUS_SRC;
  public static PartType CRAFTINGMANAGER_PART;
  public static ItemStackSrc CRAFTINGMANAGER_PART_SRC;
  public static GuiBridge GUI_CRAFTINGMANAGER;
  public static GuiBridge GUI_SATELLITESELECT;

  public AE2Plugin(IAppEngApi api) {
    this.api = api;
    INSTANCE = this;
  }
  public static void preInit() {
    try {
      AE2Plugin.MergedPriorityList_negative = MergedPriorityList.class.getDeclaredField("negative");
      AE2Plugin.MergedPriorityList_negative.setAccessible(true);
    } catch (NoSuchFieldException | SecurityException e) {
      e.printStackTrace();
    }
    // Register Part
    AE2Plugin.SATELLITE_BUS = EnumHelper.addEnum(PartType.class, "SATELLITE_BUS", new Class[]{int.class, String.class, Set.class, Set.class, Class.class},
        1024, "satellite_bus", EnumSet.of( AEFeature.CRAFTING_CPU ), EnumSet.noneOf( IntegrationType.class ), PartSatelliteBus.class);
    Api.INSTANCE.getPartModels().registerModels(AE2Plugin.SATELLITE_BUS.getModels());
    AE2Plugin.SATELLITE_BUS_SRC = ItemPart.instance.createPart(AE2Plugin.SATELLITE_BUS);

    AE2Plugin.CRAFTINGMANAGER_PART = EnumHelper.addEnum(PartType.class, "CRAFTINGMANAGER_PART", new Class[]{int.class, String.class, Set.class, Set.class, Class.class},
        1025, "craftingmanager_part", EnumSet.of( AEFeature.INTERFACE ), EnumSet.noneOf( IntegrationType.class ), PartCraftingManager.class);
    Api.INSTANCE.getPartModels().registerModels(AE2Plugin.CRAFTINGMANAGER_PART.getModels());
    AE2Plugin.CRAFTINGMANAGER_PART_SRC = ItemPart.instance.createPart(AE2Plugin.CRAFTINGMANAGER_PART);

    // Register GUI
    AE2Plugin.GUI_CRAFTINGMANAGER = EnumHelper.addEnum(GuiBridge.class, "GUI_CRAFTINGMANAGER", new Class[]{Class.class, Class.class, GuiHostType.class, SecurityPermissions.class},
        ContainerCraftingManager.class, ICraftingManagerHost.class, GuiHostType.WORLD, SecurityPermissions.BUILD);

    AE2Plugin.GUI_SATELLITESELECT = EnumHelper.addEnum(GuiBridge.class, "GUI_SATELLITESELECT", new Class[]{Class.class, Class.class, GuiHostType.class, SecurityPermissions.class},
        ContainerSatelliteSelect.class, ICraftingManagerHost.class, GuiHostType.WORLD, SecurityPermissions.BUILD);
  }

  public static void loadRecipes(ResourceLocation group) {
    IMaterials materials = AE2Plugin.INSTANCE.api.definitions().materials();
    IBlocks blocks = AE2Plugin.INSTANCE.api.definitions().blocks();
    // Satellite Bus
    ForgeRegistries.RECIPES.register(new ShapedOreRecipe(group, AE2Plugin.SATELLITE_BUS_SRC.stack(1),
        " c ",
                "ifi",
                " p ",
        'p', Blocks.PISTON,
        'f', materials.formationCore().maybeStack(1).orElse(ItemStack.EMPTY),
        'i', "ingotIron",
        'c', materials.calcProcessor().maybeStack(1).orElse(ItemStack.EMPTY)).
        setRegistryName(new ResourceLocation(TestBridge.MODID, "recipes/satellite_bus")));

    // ME Crafting Manager part
    ForgeRegistries.RECIPES.register(new ShapelessOreRecipe(group, AE2Plugin.CRAFTINGMANAGER_PART_SRC.stack(1),
        /* Input */ TB_ItemHandlers.tile_cm).
        setRegistryName(new ResourceLocation(TestBridge.MODID, "recipes/cm_block_to_part")));

    ForgeRegistries.RECIPES.register(new ShapelessOreRecipe(group, TB_ItemHandlers.tile_cm,
        /* Input */ AE2Plugin.CRAFTINGMANAGER_PART_SRC.stack(1)).
        setRegistryName(new ResourceLocation(TestBridge.MODID, "recipes/cm_part_to_block")));

    // ME Crafting Manager block
    ForgeRegistries.RECIPES.register(new ShapedOreRecipe(group, TB_ItemHandlers.tile_cm,
        "dud",
                "fca",
                "lIl",
        'd', materials.engProcessor().maybeStack(1).orElse(ItemStack.EMPTY),
        'f', materials.formationCore().maybeStack(1).orElse(ItemStack.EMPTY),
        'a', materials.annihilationCore().maybeStack(1).orElse(ItemStack.EMPTY),
        'c', materials.calcProcessor().maybeStack(1).orElse(ItemStack.EMPTY),
        'u', materials.cardPatternExpansion().maybeStack(1).orElse(ItemStack.EMPTY),
        'I', blocks.iface().maybeStack(1).orElse(ItemStack.EMPTY),
        'l', materials.logicProcessor().maybeStack(1).orElse(ItemStack.EMPTY)).
        setRegistryName(new ResourceLocation(TestBridge.MODID, "recipes/craftingmanager_part")));

    // Item Package
    ForgeRegistries.RECIPES.register(new ShapedOreRecipe(group, new ItemStack(TB_ItemHandlers.itemPackage),
        "pw",
        'p', Items.PAPER,
        'w', "plankWood").
        setRegistryName(new ResourceLocation(TestBridge.MODID, "recipes/item_package")));
  }

  @SuppressWarnings("unchecked")
  @SideOnly(Side.CLIENT)
  public static void hideFakeItems(GuiScreenEvent.BackgroundDrawnEvent event){
    Minecraft mc = Minecraft.getMinecraft();
    if(mc.currentScreen instanceof GuiMEMonitorable){
      GuiMEMonitorable g = (GuiMEMonitorable) mc.currentScreen;
      if (AE2Plugin.HIDE_FAKE_ITEM == null) {
        AE2Plugin.HIDE_FAKE_ITEM = new HideFakeItem();
      }
      try {
        ItemRepo r = (ItemRepo) ClientProxy.GuiMEMonitorable_Repo.get(g);
        IPartitionList<IAEItemStack> pl = (IPartitionList<IAEItemStack>) ClientProxy.ItemRepo_myPartitionList.get(r);
        if(pl instanceof MergedPriorityList){
          MergedPriorityList<IAEItemStack> ml = (MergedPriorityList<IAEItemStack>) pl;
          Collection<IPartitionList<IAEItemStack>> negative = (Collection<IPartitionList<IAEItemStack>>) AE2Plugin.MergedPriorityList_negative.get(ml);
          if(!negative.contains(AE2Plugin.HIDE_FAKE_ITEM)){
            negative.add(AE2Plugin.HIDE_FAKE_ITEM);
            r.updateView();
          }
        }else{
          MergedPriorityList<IAEItemStack> mlist = new MergedPriorityList<>();
          ClientProxy.ItemRepo_myPartitionList.set(r, mlist);
          if(pl != null) mlist.addNewList(pl, true);
          mlist.addNewList(AE2Plugin.HIDE_FAKE_ITEM, false);
          r.updateView();
        }
      } catch (Exception ignore) {}
    }
  }
}
