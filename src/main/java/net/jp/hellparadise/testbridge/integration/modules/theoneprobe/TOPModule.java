package net.jp.hellparadise.testbridge.integration.modules.theoneprobe;

import java.util.function.Function;
import mcjty.theoneprobe.api.ITheOneProbe;
import net.jp.hellparadise.testbridge.integration.IIntegrationModule;
import net.jp.hellparadise.testbridge.integration.IntegrationRegistry;
import net.jp.hellparadise.testbridge.integration.IntegrationType;
import net.minecraftforge.fml.common.event.FMLInterModComms;

public class TOPModule implements IIntegrationModule, Function<ITheOneProbe, Void> {

    @Override
    public void preInit() {
        FMLInterModComms.sendFunctionMessage(
            "theoneprobe",
            "getTheOneProbe",
            this.getClass()
                .getName());
    }

    @Override
    public Void apply(ITheOneProbe input) {
        if (IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.APPLIED_ENERGISTICS_2)) {
            input.registerProvider(new TileInfoProvider());
            input.registerProvider(new PartInfoProvider());
        }

        return null;
    }
}
