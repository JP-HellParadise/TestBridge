package testbridge.integration;

import appeng.integration.IIntegrationModule;

import testbridge.integration.modules.theoneprobe.TOPModule;

public enum IntegrationType {

  THE_ONE_PROBE(IntegrationSide.BOTH, "TheOneProbe", "theoneprobe") {
    @Override
    public IIntegrationModule createInstance() {
      return new TOPModule();
    }
  };

  public final IntegrationSide side;
  public final String dspName;
  public final String modID;

  IntegrationType(final IntegrationSide side, final String name, final String modid) {
    this.side = side;
    this.dspName = name;
    this.modID = modid;
  }

  public IIntegrationModule createInstance() {
    return new IIntegrationModule() {
    };
  }
}
