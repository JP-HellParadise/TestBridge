package net.jp.hellparadise.testbridge.client;

import com.cleanroommc.modularui.drawable.UITexture;
import net.jp.hellparadise.testbridge.core.Reference;
import net.minecraft.util.ResourceLocation;

/**
 * ModularUI type icon
 */
public class TB_Textures {

    public static final ResourceLocation ICONS_LOCATION = new ResourceLocation(Reference.MODID, "textures/guis/icons.png");

    public static final UITexture UI_TICK = icon("tick", 0, 0, 32, 32);

    public static final UITexture UI_CROSS = icon("cross", 32, 0, 32, 32);

    private static UITexture icon(String name, int x, int y, int w, int h) {
        return UITexture.builder()
                .location(ICONS_LOCATION)
                .imageSize(256, 256)
                .uv(x, y, w, h)
                .registerAsIcon(name)
                .build();
    }

    private static UITexture icon(String name, int x, int y) {
        return icon(name, x, y, 16, 16);
    }
}
