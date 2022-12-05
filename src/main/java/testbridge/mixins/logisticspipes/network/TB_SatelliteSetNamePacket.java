package testbridge.mixins.logisticspipes.network;

import logisticspipes.network.packets.satpipe.SatelliteSetNamePacket;

import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(SatelliteSetNamePacket.class)
public abstract class TB_SatelliteSetNamePacket {
  @Inject(method = "processPacket", at = @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT)
  private void markPipeDirty(EntityPlayer player, CallbackInfo ci, LogisticsTileGenericPipe pipe) {
    pipe.getTile().markDirty();
  }
}
