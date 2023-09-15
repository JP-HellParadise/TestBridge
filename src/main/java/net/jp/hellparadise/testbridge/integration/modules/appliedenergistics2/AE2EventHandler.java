package net.jp.hellparadise.testbridge.integration.modules.appliedenergistics2;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.prioritylist.IPartitionList;
import appeng.util.prioritylist.MergedPriorityList;
import java.util.Collection;
import net.jp.hellparadise.testbridge.helpers.RegisterItemModelHelper;
import net.jp.hellparadise.testbridge.helpers.interfaces.ae2.AccessorGuiMEMonitorable;
import net.jp.hellparadise.testbridge.helpers.interfaces.ae2.AccessorItemRepo;
import net.jp.hellparadise.testbridge.helpers.interfaces.ae2.AccessorMergedPriorityList;
import net.jp.hellparadise.testbridge.helpers.inventory.HideFakeItem;
import net.jp.hellparadise.testbridge.items.FakeItem;
import net.jp.hellparadise.testbridge.items.VirtualPatternAE;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class AE2EventHandler {

    public static class PreInit {

        @SubscribeEvent
        @SideOnly(Side.CLIENT)
        public static void textureLoad(TextureStitchEvent.Pre event) {
            RegisterItemModelHelper.registerItemModel(FakeItem.ITEM_PACKAGE);
            RegisterItemModelHelper.registerItemModel(FakeItem.ITEM_HOLDER);
            RegisterItemModelHelper.registerItemModel(VirtualPatternAE.VIRTUAL_PATTERN);
        }
    }

    public static class Init {

        @SubscribeEvent
        @SideOnly(Side.CLIENT)
        public static void onDrawBackgroundEventPost(GuiScreenEvent.BackgroundDrawnEvent event) {
            Init.hideFakeItems();
        }

        // Hacking stuff
        @SideOnly(Side.CLIENT)
        static void hideFakeItems() {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen instanceof AccessorGuiMEMonitorable gui
                    && gui.getRepo() instanceof AccessorItemRepo itemRepo) {
                if (AE2Module.HIDE_FAKE_ITEM == null) AE2Module.HIDE_FAKE_ITEM = new HideFakeItem();
                try {
                    IPartitionList<IAEItemStack> partList = itemRepo.getMyPartitionList();
                    if (partList instanceof AccessorMergedPriorityList mergedPriorityList) {
                        Collection<IPartitionList<?>> negative = mergedPriorityList.getNegative();
                        if (!negative.contains(AE2Module.HIDE_FAKE_ITEM)) {
                            negative.add(AE2Module.HIDE_FAKE_ITEM);
                            itemRepo.setResort(true);
                        }
                    } else {
                        MergedPriorityList<IAEItemStack> newMList = new MergedPriorityList<>();
                        itemRepo.setMyPartitionList(newMList);
                        if (partList != null) newMList.addNewList(partList, true);
                        newMList.addNewList(AE2Module.HIDE_FAKE_ITEM, false);
                        itemRepo.setResort(true);
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
