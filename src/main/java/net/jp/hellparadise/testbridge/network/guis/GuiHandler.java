package net.jp.hellparadise.testbridge.network.guis;

import appeng.api.parts.IPartHost;
import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.manager.GuiInfo;
import java.util.EnumMap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

public class GuiHandler {

    private static final EnumMap<EnumFacing, GuiInfo> PART_SIDES = new EnumMap<>(EnumFacing.class);

    public static GuiInfo getAE2PartUiInfo(EnumFacing facing) {
        return PART_SIDES.get(facing);
    }

    static {
        for (EnumFacing facing : EnumFacing.values()) {
            PART_SIDES.put(facing, makeAE2PartUiInfo(facing));
        }
    }

    private static GuiInfo makeAE2PartUiInfo(EnumFacing facing) {
        return GuiInfo.builder()
            .clientGui((context, panel) -> {
                TileEntity tile = context.getTileEntity();
                if (tile instanceof IPartHost partHost
                        && partHost.getPart(facing) instanceof IGuiHolder guiHolder) {
                    return guiHolder.createScreen(context, panel);
                }
                throw new UnsupportedOperationException();
            })
            .commonGui((context, guiSyncHandler) -> {
                TileEntity tile = context.getTileEntity();
                if (tile instanceof IPartHost partHost
                        && partHost.getPart(facing) instanceof IGuiHolder guiHolder) {
                    return guiHolder.buildUI(context, guiSyncHandler, context.getWorld().isRemote);
                }
                throw new UnsupportedOperationException();
            })
            .build();
    }
}
