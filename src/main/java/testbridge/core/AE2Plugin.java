package testbridge.core;

import appeng.api.AEPlugin;
import appeng.core.Api;
import appeng.core.features.AEFeature;
import appeng.core.features.ItemStackSrc;
import appeng.integration.IntegrationType;
import appeng.items.parts.ItemPart;
import appeng.items.parts.PartType;

import net.minecraftforge.common.util.EnumHelper;

import testbridge.part.PartSatelliteBus;

import java.util.EnumSet;
import java.util.Set;

@AEPlugin
public class AE2Plugin {
  public static PartType SATELLITE_BUS;
  public static ItemStackSrc SATELLITE_BUS_SRC;
  public static void preInit() {
    AE2Plugin.SATELLITE_BUS = EnumHelper.addEnum(PartType.class, "SATELLITE_BUS", new Class[]{int.class, String.class, Set.class, Set.class, Class.class},
        1024, "satellite_bus", EnumSet.of( AEFeature.CRAFTING_CPU ), EnumSet.noneOf( IntegrationType.class ), PartSatelliteBus.class);
    Api.INSTANCE.getPartModels().registerModels(AE2Plugin.SATELLITE_BUS.getModels());
    AE2Plugin.SATELLITE_BUS_SRC = ItemPart.instance.createPart(AE2Plugin.SATELLITE_BUS);
  }
}
