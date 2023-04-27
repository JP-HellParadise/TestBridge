package net.jp.hellparadise.testbridge.mixins.logisticspipes.compat;

import java.util.*;

import javax.annotation.Nonnull;

import logisticspipes.jetbrains.annotations.NotNull;
import logisticspipes.kotlin.jvm.internal.Intrinsics;
import logisticspipes.kotlin.text.StringsKt;
import logisticspipes.modules.*;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import mcjty.theoneprobe.api.*;
import mcjty.theoneprobe.config.Config;

import net.jp.hellparadise.testbridge.core.TB_ItemHandlers;
import net.jp.hellparadise.testbridge.helpers.TextHelper;
import net.jp.hellparadise.testbridge.helpers.interfaces.ITranslationKey;
import net.jp.hellparadise.testbridge.modules.TB_ModuleCM;
import net.jp.hellparadise.testbridge.pipes.PipeCraftingManager;
import net.jp.hellparadise.testbridge.pipes.ResultPipe;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(targets = "network/rs485/logisticspipes/compat/TheOneProbeIntegration$PipeInfoProvider", remap = false)
public abstract class TB_MixinTOPAddon implements IProbeInfoProvider, ITranslationKey {

    @Shadow(remap = false)
    @Final
    protected abstract void defaultInfo(CoreUnroutedPipe pipe, IProbeInfo probeInfo, ProbeMode mode);

    @Shadow(remap = false)
    @Final
    public abstract IProbeInfo addItemWithText(@NotNull IProbeInfo $this$addItemWithText, @NotNull ItemStack itemStack,
        @NotNull String text);

    @Shadow(remap = false)
    @Final
    protected abstract void addCraftingModuleInfo(ModuleCrafter module, IProbeInfo probeInfo, ProbeMode mode,
        boolean isModule);

    @Inject(
        method = "addProbeInfo",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "logisticspipes/pipes/basic/LogisticsBlockGenericPipe.getPipe(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)Llogisticspipes/pipes/basic/CoreUnroutedPipe;",
            shift = At.Shift.BY,
            by = 2),
        locals = LocalCapture.CAPTURE_FAILSOFT,
        cancellable = true,
        remap = false)
    private void preAddProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world,
        IBlockState blockState, IProbeHitData data, CallbackInfo ci, boolean isModule, CoreUnroutedPipe pipe) {
        if (pipe instanceof ResultPipe) {
            Intrinsics.checkNotNullExpressionValue(pipe, "pipe");
            this.addResultPipeInfo((ResultPipe) pipe, probeInfo);
            this.defaultInfo(pipe, probeInfo, mode);
            ci.cancel();
        } else if (pipe instanceof PipeCraftingManager) {
            Intrinsics.checkNotNullExpressionValue(pipe, "pipe");
            this.addCMPipeInfo((PipeCraftingManager) pipe, probeInfo, mode);
            this.defaultInfo(pipe, probeInfo, mode);
            ci.cancel();
        }
    }

    @Unique
    private void addResultPipeInfo(@Nonnull ResultPipe pipe, IProbeInfo probeInfo) {
        String resultPipeName = pipe.getSatellitePipeName();
        if (!StringsKt.isBlank(resultPipeName)) {
            probeInfo.text(
                new TextHelper(top$result_prefix + "name").addArgument(resultPipeName)
                    .getTranslated());
        } else {
            probeInfo.text(new TextHelper(top$result_prefix + "no_name").getTranslated());
        }
    }

    @Unique
    private void addCMPipeInfo(PipeCraftingManager pipe, IProbeInfo probeInfo, ProbeMode mode) {
        this.addConnectInfo(pipe.getModules(), probeInfo);
        if (pipe.hasBufferUpgrade()) {
            this.addBlockModeInfo(pipe, probeInfo);
        }

        IProbeInfo chassisColumn = probeInfo.vertical();
        List<LogisticsModule> modules = new ArrayList<>();
        ItemStack stack = TB_ItemHandlers.getCrafterModule();

        for (int slotID = 0; slotID < pipe.getChassisSize(); slotID++) {
            LogisticsModule module = pipe.getSubModule(slotID);
            if (module != null) {
                modules.add(module);
            }
        }

        if (!modules.isEmpty()) {
            if (mode == ProbeMode.EXTENDED) {
                modules.forEach((module) -> {
                    IProbeInfo infoCol = this.addItemWithText(chassisColumn, stack, "");
                    addCraftingModuleInfo((ModuleCrafter) module, infoCol, mode, true);
                });
            } else if (!stack.isEmpty()) {
                IProbeInfo inv = probeInfo.vertical(
                    probeInfo.defaultLayoutStyle()
                        .borderColor(Config.chestContentsBorderColor)
                        .spacing(0));
                IProbeInfo infoRow = null;
                for (int i = 0; i < modules.size(); i++) {
                    if (i % 9 == 0) {
                        infoRow = inv.horizontal();
                    }
                    infoRow.item(stack);
                }
            }
        } else {
            chassisColumn.text(new TextHelper(top$cm_prefix + "no_modules").getTranslated());
        }
    }

    @Unique
    private void addConnectInfo(TB_ModuleCM module, IProbeInfo probeInfo) {
        for (int i = 0; i < 2; i++) {
            String result = module
                .getNameByUUID(i == 0 ? module.satelliteUUID.getValue() : module.resultUUID.getValue(), i != 0);
            probeInfo.text(
                new TextHelper(top$cm_prefix + (i == 0 ? "select_sat" : "select_result")).addArgument(result)
                    .getTranslated());
        }
    }

    @Unique
    private void addBlockModeInfo(PipeCraftingManager pipe, IProbeInfo probeInfo) {
        String mode = new TextHelper(
            pipe.getAvailableAdjacent()
                .inventories()
                .isEmpty() ? gui$cm_prefix + "NoContainer" : pipe.getKeyBlockMode()).getTranslated();
        probeInfo.text(
            new TextHelper(top$cm_prefix + "blocking").addArgument(mode)
                .getTranslated());
    }
}
