package net.jp.hellparadise.testbridge.network.packets.implementation;

import appeng.api.util.AEPartLocation;
import appeng.core.sync.GuiBridge;
import appeng.util.Platform;
import com.cleanroommc.modularui.manager.GuiInfos;
import io.netty.buffer.ByteBuf;
import net.jp.hellparadise.testbridge.block.tile.TileEntityCraftingManager;
import net.jp.hellparadise.testbridge.core.TestBridge;
import net.jp.hellparadise.testbridge.network.guis.GuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class CManagerMenuSwitch implements IMessage {

    private BlockPos pos;

    private int side = -1;

    private boolean isSatSwitchMenu = false;

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        side = buf.readInt();
        isSatSwitchMenu = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX())
            .writeInt(pos.getY())
            .writeInt(pos.getZ())
            .writeInt(side)
            .writeBoolean(isSatSwitchMenu);
    }

    public CManagerMenuSwitch setPos(BlockPos pos) {
        this.pos = pos;
        return this;
    }

    public CManagerMenuSwitch setSide(int side) {
        this.side = side;
        return this;
    }

    public CManagerMenuSwitch isSatSwitchMenu(boolean value) {
        this.isSatSwitchMenu = value;
        return this;
    }

    public static class Handler implements IMessageHandler<CManagerMenuSwitch, IMessage> {

        @Override
        public IMessage onMessage(CManagerMenuSwitch packet, MessageContext ctx) {
            TestBridge.getProxy()
                    .getThreadListener(ctx)
                    .addScheduledTask(() -> {
                        World world = TestBridge.getProxy().getWorld(ctx);
                        EntityPlayer player = TestBridge.getProxy().getPlayer(ctx);
                        if (world != null && player != null && packet != null) {
                            TileEntity tile = world.getTileEntity(packet.pos);
                            if (packet.isSatSwitchMenu) {
                                if (tile instanceof TileEntityCraftingManager) {
                                    GuiInfos.TILE_ENTITY.open(player, world, packet.pos);
                                } else {
                                    GuiHandler.getCoverUiInfo(EnumFacing.values()[packet.side])
                                            .open(player, world, packet.pos);
                                }
                            } else {
                                Platform.openGUI(player, tile, AEPartLocation.fromOrdinal(packet.side), GuiBridge.valueOf("GUI_CRAFTING_MANAGER"));
                            }
                        }
                    });
            return null;
        }
    }
}
