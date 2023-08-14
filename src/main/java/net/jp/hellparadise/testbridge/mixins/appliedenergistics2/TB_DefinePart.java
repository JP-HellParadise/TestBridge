package net.jp.hellparadise.testbridge.mixins.appliedenergistics2;

import appeng.core.features.AEFeature;
import appeng.integration.IntegrationType;
import appeng.items.parts.PartType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import net.jp.hellparadise.testbridge.part.PartCraftingManager;
import net.jp.hellparadise.testbridge.part.PartSatelliteBus;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Define part, use {@link PartType#valueOf(String)} to retrieve it
 */
@Mixin(value = PartType.class, remap = false)
@Unique public abstract class TB_DefinePart {

    @Shadow
    @Final
    @Mutable
    private static PartType[] $VALUES;

    @Unique private static final PartType SATELLITE_BUS = partType$addPart("SATELLITE_BUS", 1024,
            "satellite_bus",
            EnumSet.of(AEFeature.CRAFTING_CPU),
            EnumSet.noneOf(IntegrationType.class),
            PartSatelliteBus.class);
    @Unique private static final PartType CRAFTING_MANAGER = partType$addPart("CRAFTING_MANAGER", 1025,
            "crafting_manager",
            EnumSet.of(AEFeature.INTERFACE),
            EnumSet.noneOf(IntegrationType.class),
            PartCraftingManager.class);

    @Invoker(value = "<init>", remap = false)
    public static PartType partType$invokeInit(String internalName, int internalId, int baseMetaValue2, String itemModel, Set features, Set integrations, Class c) {
        throw new AssertionError();
    }

    @Unique private static PartType partType$addPart(String internalName, int baseMetaValue, String itemModel, Set features, Set integrations, Class c) {
        ArrayList<PartType> variants = new ArrayList<>(Arrays.asList(TB_DefinePart.$VALUES));
        PartType instrument = partType$invokeInit(internalName, variants.get(variants.size() - 1).ordinal() + 1, baseMetaValue, itemModel, features, integrations, c);
        variants.add(instrument);
        TB_DefinePart.$VALUES = variants.toArray(new PartType[0]);
        return instrument;
    }
}
