package testbridge.integration.modules.theoneprobe;

import java.util.function.Function;

import net.minecraftforge.fml.common.event.FMLInterModComms;

import mcjty.theoneprobe.api.ITheOneProbe;

import testbridge.core.TBConfig;
import testbridge.core.TestBridge;
import testbridge.integration.IIntegrationModule;

public class TOPModule implements IIntegrationModule, Function<ITheOneProbe, Void> {

  @Override
  public void preInit() {
    FMLInterModComms.sendFunctionMessage("theoneprobe", "getTheOneProbe", this.getClass().getName());
  }

  @Override
  public Void apply(ITheOneProbe input) {
    input.registerProvider(new TileInfoProvider());
    input.registerProvider(new PartInfoProvider());
    if (TBConfig.instance().isFeatureEnabled(TBConfig.TBFeature.LOGGING))
      TestBridge.log.info("The One Probe integration loaded.");
    return null;
  }

}
