package testbridge.integration.modules.appliedenergistics2;

import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import testbridge.core.TB_ItemHandlers;
import testbridge.helpers.RegisterHelper;

public class AE2EventHandler {
  public static class preInit {
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void textureLoad(TextureStitchEvent.Pre event) {
      RegisterHelper.registerItemModel(TB_ItemHandlers.itemPackage, "testbridge:item_package");
      RegisterHelper.registerItemModel(TB_ItemHandlers.itemHolder, "testbridge:item_placeholder");
      RegisterHelper.registerItemModel(TB_ItemHandlers.virtualPattern, "testbridge:item_virtualpattern");
    }
  }

  public static class init {
    @SubscribeEvent
    public void onDrawBackgroundEventPost(GuiScreenEvent.BackgroundDrawnEvent event) {
      AE2Module.hideFakeItems(event);
    }
  }
}
