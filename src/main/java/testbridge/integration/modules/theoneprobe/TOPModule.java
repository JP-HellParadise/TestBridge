package testbridge.integration.modules.theoneprobe;

import java.util.function.Function;

import net.minecraftforge.fml.common.event.FMLInterModComms;

import mcjty.theoneprobe.api.ITheOneProbe;

import testbridge.core.TB_Config;
import testbridge.core.TestBridge;
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
    if (TB_Config.instance().isFeatureEnabled(TB_Config.TBFeature.LOGGING))
      TestBridge.log.info("The One Probe integration loaded.");
    return null;
  }

}
