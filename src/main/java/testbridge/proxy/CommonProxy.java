package testbridge.proxy;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import testbridge.helpers.interfaces.IProxy;

public class CommonProxy implements IProxy {
  public void preInit(FMLPreInitializationEvent event) {};

  public void init(FMLInitializationEvent event) {};

  public void postInit(FMLPostInitializationEvent event) {};

  @Override
  public void registerRenderers() {}

  @Override
  public void registerTextures() {}
}
