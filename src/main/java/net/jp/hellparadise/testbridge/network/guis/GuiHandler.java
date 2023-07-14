package net.jp.hellparadise.testbridge.network.guis;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import com.cleanroommc.modularui.manager.GuiInfo;
import java.util.EnumMap;
import net.jp.hellparadise.testbridge.client.gui.SatelliteGuiHolder;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

public class GuiHandler {

    private static final EnumMap<EnumFacing, GuiInfo> SIDES = new EnumMap<>(EnumFacing.class);

    public static GuiInfo getCoverUiInfo(EnumFacing facing) {
        return SIDES.get(facing);
    }

    static {
        for (EnumFacing facing : EnumFacing.values()) {
            SIDES.put(facing, makeCoverUiInfo(facing));
        }
    }

    private static GuiInfo makeCoverUiInfo(EnumFacing facing) {
        return GuiInfo.builder()
            .clientGui(context -> {
                TileEntity te = context.getTileEntity();
                if (!(te instanceof IPartHost)) throw new IllegalStateException();
                IPart part = ((IPartHost) te).getPart(facing);
                if (!(part instanceof SatelliteGuiHolder)) throw new IllegalStateException();
                return ((SatelliteGuiHolder) part).createClientGui(context.getPlayer());
            })
            .serverGui((context, syncHandler) -> {
                TileEntity te = context.getTileEntity();
                if (!(te instanceof IPartHost)) throw new IllegalStateException();
                IPart part = ((IPartHost) te).getPart(facing);
                if (!(part instanceof SatelliteGuiHolder)) throw new IllegalStateException();
                ((SatelliteGuiHolder) part).buildSyncHandler(syncHandler, context.getPlayer());
            })
            .build();
    }
}
