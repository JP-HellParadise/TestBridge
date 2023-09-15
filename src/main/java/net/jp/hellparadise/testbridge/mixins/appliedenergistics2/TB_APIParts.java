package net.jp.hellparadise.testbridge.mixins.appliedenergistics2;

import appeng.api.definitions.IItemDefinition;
import appeng.bootstrap.FeatureFactory;
import appeng.core.api.definitions.ApiParts;
import appeng.core.features.DamagedItemDefinition;
import appeng.core.features.registries.PartModels;
import appeng.items.parts.ItemPart;
import appeng.items.parts.PartType;
import com.llamalad7.mixinextras.sugar.Local;
import net.jp.hellparadise.testbridge.helpers.interfaces.ae2.AccessorApiParts;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ApiParts.class, remap = false)
public abstract class TB_APIParts implements AccessorApiParts {
    @Mutable
    @Final
    @Unique private IItemDefinition testBridge$SATELLITE;

    @Mutable
    @Final
    @Unique private IItemDefinition testBridge$CRAFTING_MANAGER;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void TB_inject$ApiParts (FeatureFactory registry, PartModels partModels, CallbackInfo ci, @Local ItemPart itemPart) {
        this.testBridge$SATELLITE = new DamagedItemDefinition("part.bus.satellite", itemPart.createPart(PartType.valueOf("SATELLITE_BUS")));
        this.testBridge$CRAFTING_MANAGER = new DamagedItemDefinition("part.bus.crafting_manager", itemPart.createPart(PartType.valueOf("CRAFTING_MANAGER")));
    }

    @Override
    public IItemDefinition satelliteBus() {
        return this.testBridge$SATELLITE;
    }

    @Override
    public IItemDefinition craftingManager() {
        return this.testBridge$CRAFTING_MANAGER;
    }
}
