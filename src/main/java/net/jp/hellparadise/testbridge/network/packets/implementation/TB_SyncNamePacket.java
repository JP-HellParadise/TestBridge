package net.jp.hellparadise.testbridge.network.packets.implementation;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import appeng.tile.AEBaseTile;
import appeng.tile.networking.TileCableBus;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.StringCoordinatesPacket;
import logisticspipes.network.exception.TargetNotFoundException;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import net.jp.hellparadise.testbridge.block.tile.TileEntityCraftingManager;
import net.jp.hellparadise.testbridge.helpers.interfaces.ICraftingManagerHost;
import net.jp.hellparadise.testbridge.helpers.interfaces.SatelliteInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class TB_SyncNamePacket extends StringCoordinatesPacket {

    private int side;

    public TB_SyncNamePacket(int id) {
        super(id);
    }

    @Override
    public void processPacket(EntityPlayer player) {
        try {
            LogisticsTileGenericPipe pipe = getPipe(player.world, LTGPCompletionCheck.PIPE);
            // No need to check if pipe null since it will throw Exception on LP side
            if (pipe.pipe instanceof SatelliteInfo) {
                ((SatelliteInfo) pipe.pipe).setSatelliteName(getString());
            }
        } catch (TargetNotFoundException tnfe1) {
            try {
                TileEntity TE = getTileAs(player.getEntityWorld(), IPartHost.class).getTile();
                if (TE instanceof TileCableBus) {
                    IPart part = ((TileCableBus) TE).getPart(AEPartLocation.fromOrdinal(this.side));
                    if (part instanceof SatelliteInfo) {
                        ((SatelliteInfo) part).setSatelliteName(getString());
                    } else if (part instanceof ICraftingManagerHost) {
                        ((ICraftingManagerHost) part).setSatellite(getString());
                    }
                }
            } catch (TargetNotFoundException tnfe2) {
                TileEntity TE = getTileAs(player.getEntityWorld(), AEBaseTile.class).getTile();
                if (TE instanceof TileEntityCraftingManager) {
                    ((TileEntityCraftingManager) TE).setSatellite(getString());
                }
            }
        }
    }

    @Override
    public void writeData(LPDataOutput output) {
        super.writeData(output);
        output.writeInt(side);
    }

    @Override
    public void readData(LPDataInput input) {
        super.readData(input);
        side = input.readInt();
    }

    @Override
    public ModernPacket template() {
        return new TB_SyncNamePacket(getId());
    }

    public TB_SyncNamePacket setSide(int side) {
        this.side = side;
        return this;
    }
}
