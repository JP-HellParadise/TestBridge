package testbridge.network.packets.HandleResultPacket;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import appeng.tile.networking.TileCableBus;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.exception.TargetNotFoundException;
import logisticspipes.pipes.SatelliteNamingResult;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;

import testbridge.container.ContainerPackage;
import testbridge.core.AE2Plugin;
import testbridge.core.TestBridge;
import testbridge.network.abstractpackets.CustomCoordinatesPacket;
import testbridge.part.PartSatelliteBus;
import testbridge.pipes.ResultPipe;

import java.util.function.Consumer;

@StaticResolve
public class TB_SetNamePacket extends CustomCoordinatesPacket {
  private int id = 0;

  public TB_SetNamePacket(int id) {
    super(id);
  }

  @Override
  public void processPacket(EntityPlayer player) {
    if (getId() != id) {
      id = getId();
    }
    String newName = getString();
    SatelliteNamingResult result = null;
    if (id != 0) {
      processResIDMod(player, this);
      return;
    }
    if (newName.trim().isEmpty()) {
      result = SatelliteNamingResult.BLANK_NAME;
    } else {
      try {
        LogisticsTileGenericPipe pipe = this.getPipe(player.getEntityWorld(), LTGPCompletionCheck.PIPE);
        if (pipe != null && pipe.pipe instanceof ResultPipe) {
          ResultPipe resultPipe = (ResultPipe) pipe.pipe;
          if (resultPipe.getSatellitesOfType().stream().anyMatch(it -> it.getSatellitePipeName().equals(newName))) {
            result = SatelliteNamingResult.DUPLICATE_NAME;
          } else {
            result = SatelliteNamingResult.SUCCESS;
            resultPipe.setSatellitePipeName(newName);
            resultPipe.updateWatchers();
            resultPipe.ensureAllSatelliteStatus();
            pipe.getTile().markDirty();
          }
        }
      } catch (TargetNotFoundException e) {
        TileEntity TE = getTileAs(player.getEntityWorld(), IPartHost.class).getTile();
        if (TE instanceof TileCableBus) {
          IPart iPart = ((TileCableBus) TE).getPart(AEPartLocation.fromOrdinal(getSide()));
          if (iPart instanceof PartSatelliteBus) {
            if (((PartSatelliteBus) iPart).getSatellitesOfType().stream().anyMatch(it -> it.getSatellitePipeName().equals(newName))) {
              result = SatelliteNamingResult.DUPLICATE_NAME;
            } else {
              result = SatelliteNamingResult.SUCCESS;
              ((PartSatelliteBus) iPart).setSatellitePipeName(newName);
              ((PartSatelliteBus) iPart).updateWatchers();
              ((PartSatelliteBus) iPart).ensureAllSatelliteStatus();
              TE.markDirty();
            }
          }
        }
      }
    }
    if (result != null) {
      MainProxy.sendPacketToPlayer(PacketHandler.getPacket(TB_SetNameResult.class).setResult(result).setNewName(getString()), player);
    }
  }

  private void processResIDMod(EntityPlayer player, CustomCoordinatesPacket packet) {
    if(packet.getSide() == -1){
      if(player.openContainer instanceof Consumer){
        ((Consumer<String>) player.openContainer).accept(packet.getString());
      }
    }else if(TestBridge.isAELoaded()){
      AE2Plugin.processResIDMod(player, packet);
    }
  }

  @Override
  public ModernPacket template() {
    return new TB_SetNamePacket(getId());
  }

  public interface ICustomPacket {
    void setNamePacket(int id, String name, EntityPlayer player);
  }

}

