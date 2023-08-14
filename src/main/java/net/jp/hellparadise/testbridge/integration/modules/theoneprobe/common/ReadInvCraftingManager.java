package net.jp.hellparadise.testbridge.integration.modules.theoneprobe.common;

import appeng.api.implementations.tiles.ISegmentedInventory;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.config.Config;
import net.jp.hellparadise.testbridge.helpers.TranslationKey;
import net.jp.hellparadise.testbridge.helpers.interfaces.ae2.ICraftingManagerHost;
import net.jp.hellparadise.testbridge.utils.TextUtil;
import net.minecraftforge.items.IItemHandler;

public interface ReadInvCraftingManager {
    default void addConnectInfo(ICraftingManagerHost cmHost, IProbeInfo probeInfo) {
        String satName = cmHost.getSatelliteName();
        probeInfo.text(
                TextUtil.translate(TranslationKey.top$cm_prefix + "select_sat",
                        TextUtil.translate(satName.isEmpty() ?
                                        TranslationKey.top$cm_prefix + "none" : cmHost.getSatellitePart() != null ?
                                        TranslationKey.top$cm_prefix + "valid" : TranslationKey.top$cm_prefix + "router_error"
                                , satName)));
    }

    default void addInfo(Object crafting_manager, IProbeInfo probeInfo) {
        IItemHandler patternInv = ((ISegmentedInventory) crafting_manager).getInventoryByName("patterns");
        IntArrayList patternSlotList = new IntArrayList();

        for (int i = 0; i < patternInv.getSlots(); i++) {
            if (!patternInv.getStackInSlot(i)
                    .isEmpty()) {
                patternSlotList.add(i);
            }
        }

        if (!patternSlotList.isEmpty()) {
            IProbeInfo inv = probeInfo.vertical(
                    probeInfo.defaultLayoutStyle()
                            .borderColor(Config.chestContentsBorderColor)
                            .spacing(0));
            IProbeInfo infoRow = null;
            for (int i = 0; i < patternSlotList.size(); i++) {
                if (i % 9 == 0) infoRow = inv.horizontal();
                infoRow.item(patternInv.getStackInSlot(patternSlotList.getInt(i)));
            }
        } else
            probeInfo.text(TextUtil.translate(TranslationKey.top$cm_prefix + "no_patterns"));
    }
}
