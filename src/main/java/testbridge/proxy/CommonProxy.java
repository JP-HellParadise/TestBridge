package testbridge.proxy;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import testbridge.interfaces.IProxy;

public abstract class CommonProxy implements IProxy {
  public abstract void preInit(FMLPreInitializationEvent event);

  public abstract void init(FMLInitializationEvent event);

  public abstract void postInit(FMLPostInitializationEvent event);
}
