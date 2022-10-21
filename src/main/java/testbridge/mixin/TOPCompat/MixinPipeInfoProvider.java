package testbridge.mixin.TOPCompat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;

import logisticspipes.kotlin.jvm.internal.Intrinsics;
import logisticspipes.kotlin.text.StringsKt;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;

import network.rs485.logisticspipes.compat.TheOneProbeIntegration;

import testbridge.pipes.ResultPipe;

import javax.annotation.Nonnull;

@Mixin(targets = "network.rs485.logisticspipes.compat.TheOneProbeIntegration$PipeInfoProvider")
public class MixinPipeInfoProvider {

  @Inject(
      method = "addProbeInfo",
      at = @At(
          value = "INVOKE",
          target = "Llogisticspipes/kotlin/jvm/internal/Intrinsics;checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V",
          shift = At.Shift.AFTER)
          )
  public void preAddProbeInfo(
      ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data, CallbackInfo ci) {
    if (probeInfo != null && blockState != null && data != null) {
      if (blockState.getBlock() instanceof LogisticsBlockGenericPipe) {
        CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.getPipe(world, data.getPos());
        if (pipe instanceof ResultPipe) {
          Intrinsics.checkNotNullExpressionValue(pipe, "pipe");
          this.addResultPipeInfo((ResultPipe) pipe, probeInfo);
        }
      }
    }
  }

  public void addResultPipeInfo(@Nonnull ResultPipe pipe, IProbeInfo probeInfo) {
    String resultPipeName = pipe.getSatellitePipeName();
    if (!StringsKt.isBlank(resultPipeName)) {
      TheOneProbeIntegration.LPText var4 = new TheOneProbeIntegration().new LPText("top.testbridge.pipe.result.name");
      var4.getArguments().add(resultPipeName);
      probeInfo.element(var4);
    } else {
      probeInfo.element(new TheOneProbeIntegration().new LPText("top.testbridge.pipe.result.no_name"));
    }
  }

}
