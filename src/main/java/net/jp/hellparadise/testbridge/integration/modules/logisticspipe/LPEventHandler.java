package net.jp.hellparadise.testbridge.integration.modules.logisticspipe;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class LPEventHandler {

    public static class preInit {

        @SubscribeEvent
        @SideOnly(Side.CLIENT)
        public static void textureLoad(TextureStitchEvent.Pre event) {
            LPModule.TBTextures.registerBlockIcons(
                Minecraft.getMinecraft()
                    .getTextureMapBlocks());
        }
    }
}
