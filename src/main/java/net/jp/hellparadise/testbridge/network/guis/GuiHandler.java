package net.jp.hellparadise.testbridge.network.guis;

import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.gui.DummyContainer;

import net.jp.hellparadise.testbridge.client.gui.GuiPackage;
import net.jp.hellparadise.testbridge.client.gui.GuiResultPipe;
import net.jp.hellparadise.testbridge.container.ContainerPackage;
import net.jp.hellparadise.testbridge.items.FakeItem;
import net.jp.hellparadise.testbridge.part.PartSatelliteBus;
import net.jp.hellparadise.testbridge.pipes.ResultPipe;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;

public class GuiHandler implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, final int x, final int y, final int z) {
        // Satellite Bus checker
        if (ID >= GuiEnum.SATELLITE_BUS.begin() && ID < GuiEnum.SATELLITE_BUS.end()) {
            AEPartLocation side = AEPartLocation.fromOrdinal(ID - GuiEnum.SATELLITE_BUS.begin());
            TileEntity TE = world.getTileEntity(new BlockPos(x, y, z));
            if (TE instanceof IPartHost) {
                IPart part = ((IPartHost) TE).getPart(side);
                if (part instanceof PartSatelliteBus) {
                    return new DummyContainer(player.inventory, null);
                }
            }
            return null;
        }

        if (ID < 100 && ID > 0) {
            TileEntity tile = null;
            if (y != -1) {
                tile = world.getTileEntity(new BlockPos(x, y, z));
            }
            LogisticsTileGenericPipe pipe = null;
            if (tile instanceof LogisticsTileGenericPipe) {
                pipe = (LogisticsTileGenericPipe) tile;
            }
            switch (GuiEnum.values()[ID]) {
                case RESULT_PIPE:
                    if (pipe != null && pipe.pipe instanceof ResultPipe) {
                        return new DummyContainer(player.inventory, null);
                    }
                    return null;

                case TEMPLATE_PKG:
                    Item onHand = player.getHeldItemMainhand()
                        .getItem();
                    if (onHand instanceof FakeItem) {
                        return new ContainerPackage(player);
                    }
                    return null;

                default:
                    break;
            }
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int guiID, EntityPlayer player, final World world, int x, int y, int z) {
        // Satellite Bus checker
        if (guiID >= GuiEnum.SATELLITE_BUS.begin() && guiID < GuiEnum.SATELLITE_BUS.end()) {
            TileEntity TE = world.getTileEntity(new BlockPos(x, y, z));
            if (TE instanceof IPartHost) {
                IPart part = ((IPartHost) TE)
                    .getPart(AEPartLocation.fromOrdinal(guiID - GuiEnum.SATELLITE_BUS.begin()));
                if (part instanceof PartSatelliteBus) {
                    return new GuiResultPipe<>((PartSatelliteBus) part, "gui.satellite.");
                }
            }
            return null;
        }

        if (guiID < 100 && guiID >= 0) {
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            LogisticsTileGenericPipe pipe = null;
            if (tile instanceof LogisticsTileGenericPipe) {
                pipe = (LogisticsTileGenericPipe) tile;
            }
            switch (GuiEnum.values()[guiID]) {
                case RESULT_PIPE:
                    if (pipe != null && pipe.pipe instanceof ResultPipe) {
                        return new GuiResultPipe<>(((ResultPipe) pipe.pipe), "gui.result.");
                    }
                    return null;

                case TEMPLATE_PKG:
                    ItemStack onHand = player.getHeldItemMainhand();
                    if (onHand.getItem() instanceof FakeItem) {
                        return new GuiPackage(player, null);
                    }
                    return null;

                default:
                    break;
            }
        }
        return null;
    }
}
