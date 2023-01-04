package testbridge.integration.modules.theoneprobe;

import java.util.function.Function;

import net.minecraftforge.fml.common.event.FMLInterModComms;

import mcjty.theoneprobe.api.ITheOneProbe;

import appeng.integration.IIntegrationModule;

import testbridge.core.TestBridge;

public class TOPModule implements IIntegrationModule, Function<ITheOneProbe, Void> {

  @Override
  public void preInit() {
    FMLInterModComms.sendFunctionMessage("theoneprobe", "getTheOneProbe", this.getClass().getName());
  }

  @Override
  public Void apply(ITheOneProbe input) {
    input.registerProvider(new TileInfoProvider());
    input.registerProvider(new PartInfoProvider());
        TestBridge.log.info("The One Probe integration loaded.");
    return null;
  }

}
