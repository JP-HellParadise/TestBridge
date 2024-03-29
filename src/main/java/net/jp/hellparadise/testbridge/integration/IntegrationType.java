package net.jp.hellparadise.testbridge.integration;

import net.jp.hellparadise.testbridge.integration.modules.appliedenergistics2.AE2Module;
import net.jp.hellparadise.testbridge.integration.modules.logisticspipe.LPModule;
import net.jp.hellparadise.testbridge.integration.modules.theoneprobe.TOPModule;

public enum IntegrationType {

    LOGISTICS_PIPES(IntegrationSide.BOTH, "LogisticsPipe", "logisticspipes") {

        @Override
        public IIntegrationModule createInstance() {
            return new LPModule();
        }
    },

    APPLIED_ENERGISTICS_2(IntegrationSide.BOTH, "AppliedEnergistics2", "appliedenergistics2") {

        @Override
        public IIntegrationModule createInstance() {
            return AE2Module.INSTANCE;
        }
    },

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
        return new IIntegrationModule() {};
    }
}
