package net.jp.hellparadise.testbridge.client;

import com.cleanroommc.modularui.drawable.UITexture;
import net.jp.hellparadise.testbridge.core.Reference;

public class TB_Textures {
    public static final UITexture UI_TICK = UITexture.builder()
            .location(Reference.MODID, "icons/tick")
            .imageSize(32, 32)
            .adaptable(2)
            .registerAsIcon("tick")
            .build();

    public static final UITexture UI_CROSS = UITexture.builder()
            .location(Reference.MODID, "icons/cross")
            .imageSize(32, 32)
            .adaptable(2)
            .registerAsIcon("cross")
            .build();
}
