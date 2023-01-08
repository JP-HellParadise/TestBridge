package testbridge.mixins.logisticspipes.compat;

import java.util.*;
import javax.annotation.Nonnull;

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

import mcjty.theoneprobe.api.*;
import mcjty.theoneprobe.config.Config;

import logisticspipes.jetbrains.annotations.NotNull;
import logisticspipes.kotlin.jvm.internal.Intrinsics;
import logisticspipes.kotlin.text.StringsKt;
import logisticspipes.modules.*;
import logisticspipes.pipes.basic.CoreUnroutedPipe;

import testbridge.client.gui.GuiCMPipe;
import testbridge.core.TB_ItemHandlers;
import testbridge.helpers.TBText;
import testbridge.interfaces.ITranslationKey;
import testbridge.modules.TB_ModuleCM;
import testbridge.pipes.PipeCraftingManager;
import testbridge.pipes.ResultPipe;

@Mixin(targets = "network.rs485.logisticspipes.compat.TheOneProbeIntegration$PipeInfoProvider", remap = false)
public abstract class TB_MixinTOPAddon implements IProbeInfoProvider, ITranslationKey {

  @Shadow
  @Final
  protected abstract void defaultInfo(CoreUnroutedPipe pipe, IProbeInfo probeInfo, ProbeMode mode);

  @Shadow
  @Final
  public abstract IProbeInfo addItemWithText(@NotNull IProbeInfo $this$addItemWithText, @NotNull ItemStack itemStack, @NotNull String text);

  @Shadow
  @Final
  protected abstract void addCraftingModuleInfo(ModuleCrafter module, IProbeInfo probeInfo, ProbeMode mode, boolean isModule);

  @Inject(
    method = "addProbeInfo",
    at = @At(
      value = "INVOKE_ASSIGN",
      target = "logisticspipes/pipes/basic/LogisticsBlockGenericPipe.getPipe(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)Llogisticspipes/pipes/basic/CoreUnroutedPipe;",
      shift = At.Shift.BY,
      by = 2),
    locals = LocalCapture.CAPTURE_FAILSOFT,
    cancellable = true)
  private void preAddProbeInfo(
      ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data, CallbackInfo ci, boolean isModule, CoreUnroutedPipe pipe) {
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
      probeInfo.text(new TBText(top$result_prefix + "name").addArgument(resultPipeName).getTranslated());
    } else {
      probeInfo.text(new TBText(top$result_prefix + "no_name").getTranslated());
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

    for (int slotID = 0 ; slotID < pipe.getChassisSize() ; slotID++) {
      LogisticsModule module = pipe.getSubModule(slotID);
      if (module != null) {
        modules.add(module);
      }
    }

    if (!modules.isEmpty()) {
      if (mode == ProbeMode.EXTENDED) {
        modules.forEach( (module) ->
        {
          IProbeInfo infoCol = this.addItemWithText(chassisColumn, stack, "");
          addCraftingModuleInfo((ModuleCrafter) module, infoCol, mode, true);
        });
      } else if (!stack.isEmpty()) {
        IProbeInfo inv = probeInfo.vertical(probeInfo.defaultLayoutStyle().borderColor(Config.chestContentsBorderColor).spacing(0));
        IProbeInfo infoRow = null;
        for (int i = 0 ; i < modules.size() ; i++) {
          if (i % 9 == 0) {
            infoRow = inv.horizontal();
          }
          infoRow.item(stack);
        }
      }
    } else {
      chassisColumn.text(new TBText(top$cm_prefix + "no_modules").getTranslated());
    }
  }

  @Unique
  private void addConnectInfo(TB_ModuleCM module, IProbeInfo probeInfo) {
    String name = module.getSatResultNameByUUID(module.getSatelliteUUID().getValue());
    probeInfo.text(new TBText(top$cm_prefix + "select_sat")
        .addArgument(name.isEmpty() ? new TBText(top$cm_prefix + "none").getTranslated() : name)
        .getTranslated());

    name = module.getSatResultNameByUUID(module.getResultUUID().getValue());
    probeInfo.text(new TBText(top$cm_prefix + "select_result")
        .addArgument(name.isEmpty() ? new TBText(top$cm_prefix + "none").getTranslated() : name)
        .getTranslated());
  }

  @Unique
  private void addBlockModeInfo(PipeCraftingManager pipe, IProbeInfo probeInfo) {
    String mode = new TBText(pipe.getAvailableAdjacent().inventories().isEmpty() ? gui$cm_prefix + "NoContainer" : pipe.getKeyBlockMode()).getTranslated();
    probeInfo.text(new TBText(top$cm_prefix + "blocking").addArgument(mode).getTranslated());
  }
}
