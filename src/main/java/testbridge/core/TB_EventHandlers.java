package testbridge.core;

import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber
public class TB_EventHandlers {
  @SubscribeEvent
  @SideOnly(Side.CLIENT)
  public static void textureLoad(TextureStitchEvent.Pre event) {
    TestBridge.proxy.registerTextures();
  }
}
