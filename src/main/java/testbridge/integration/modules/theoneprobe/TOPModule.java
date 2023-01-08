package testbridge.integration.modules.theoneprobe;

import java.util.function.Function;

import net.minecraftforge.fml.common.event.FMLInterModComms;

import mcjty.theoneprobe.api.ITheOneProbe;

import testbridge.integration.IIntegrationModule;
import testbridge.integration.IntegrationRegistry;
import testbridge.integration.IntegrationType;

public class TOPModule implements IIntegrationModule, Function<ITheOneProbe, Void> {

  @Override
  public void preInit() {
    FMLInterModComms.sendFunctionMessage("theoneprobe", "getTheOneProbe", this.getClass().getName());
  }

  @Override
  public Void apply(ITheOneProbe input) {
    if (IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.APPLIED_ENERGISTICS_2)){
      input.registerProvider(new TileInfoProvider());
      input.registerProvider(new PartInfoProvider());
    }

    return null;
  }
}
