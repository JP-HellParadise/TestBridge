package testbridge.proxy;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import testbridge.core.TestBridge;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

  public void preInit(FMLPreInitializationEvent event) {}

  public void init(FMLInitializationEvent event) {}

  public void postInit(FMLPostInitializationEvent event) {}

  @Override
  public void registerTextures() {
    TestBridge.textures.registerBlockIcons(Minecraft.getMinecraft().getTextureMapBlocks());
  }

}
