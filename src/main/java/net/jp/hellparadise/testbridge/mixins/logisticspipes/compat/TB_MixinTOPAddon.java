package net.jp.hellparadise.testbridge.mixins.logisticspipes.compat;

import java.util.*;
import javax.annotation.Nonnull;
import logisticspipes.modules.*;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import mcjty.theoneprobe.api.*;
import mcjty.theoneprobe.config.Config;
import net.jp.hellparadise.testbridge.core.TB_ItemHandlers;
import net.jp.hellparadise.testbridge.helpers.TranslationKey;
import net.jp.hellparadise.testbridge.modules.TB_ModuleCM;
import net.jp.hellparadise.testbridge.pipes.PipeCraftingManager;
import net.jp.hellparadise.testbridge.pipes.ResultPipe;
import net.jp.hellparadise.testbridge.utils.TextUtil;
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
public abstract class TB_MixinTOPAddon implements IProbeInfoProvider {

    @Shadow(remap = false)
    @Final
    protected abstract void defaultInfo(CoreUnroutedPipe pipe, IProbeInfo probeInfo, ProbeMode mode);

    @Shadow(remap = false)
    @Final
    public abstract IProbeInfo addItemWithText(@Nonnull IProbeInfo $this$addItemWithText, @Nonnull ItemStack itemStack,
        @Nonnull String text);

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
            this.addResultPipeInfo((ResultPipe) pipe, probeInfo);
            this.defaultInfo(pipe, probeInfo, mode);
            ci.cancel();
        } else if (pipe instanceof PipeCraftingManager) {
            this.addCMPipeInfo((PipeCraftingManager) pipe, probeInfo, mode);
            this.defaultInfo(pipe, probeInfo, mode);
            ci.cancel();
        }
    }

    @Unique private void addResultPipeInfo(@Nonnull ResultPipe pipe, IProbeInfo probeInfo) {
        String resultPipeName = pipe.getSatelliteName();
        probeInfo.text(TextUtil.translate(
                TranslationKey.top$result_prefix + (resultPipeName.isEmpty() ? "no_name" : "name"), resultPipeName));
    }

    @Unique private void addCMPipeInfo(PipeCraftingManager pipe, IProbeInfo probeInfo, ProbeMode mode) {
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
            chassisColumn.text(TextUtil.translate(TranslationKey.top$cm_prefix + "no_modules"));
        }
    }

    @Unique private void addConnectInfo(TB_ModuleCM module, IProbeInfo probeInfo) {
        for (int i = 0; i < 2; i++) {
            String result = module
                .getNameByUUID(i == 0 ? module.satelliteUUID.getValue() : module.resultUUID.getValue(), i != 0);
            probeInfo.text(TextUtil.translate(TranslationKey.top$cm_prefix + (i == 0 ? "select_sat" : "select_result"), result));
        }
    }

    @Unique private void addBlockModeInfo(PipeCraftingManager pipe, IProbeInfo probeInfo) {
        String mode = TextUtil.translate(
                pipe.getAvailableAdjacent()
                        .inventories()
                        .isEmpty() ? TranslationKey.gui$cm_prefix + "NoContainer" : pipe.getKeyBlockMode());
        probeInfo.text(TextUtil.translate(TranslationKey.top$cm_prefix + "blocking", mode));
    }
}
