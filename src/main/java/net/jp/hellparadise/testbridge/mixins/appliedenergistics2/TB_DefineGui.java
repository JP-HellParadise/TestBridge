package net.jp.hellparadise.testbridge.mixins.appliedenergistics2;

import appeng.api.config.SecurityPermissions;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.GuiHostType;
import java.util.ArrayList;
import java.util.Arrays;
import net.jp.hellparadise.testbridge.container.ContainerCraftingManager;
import net.jp.hellparadise.testbridge.helpers.interfaces.ae2.ICraftingManagerHost;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = GuiBridge.class, remap = false)
@Unique public abstract class TB_DefineGui {
    @Shadow
    @Final
    @Mutable
    private static GuiBridge[] $VALUES;

    @Unique private static final GuiBridge CRAFTING_MANAGER = GuiBridge$addPart("GUI_CRAFTING_MANAGER", ContainerCraftingManager.class, ICraftingManagerHost.class, GuiHostType.WORLD, SecurityPermissions.BUILD);

    @Invoker(value = "<init>", remap = false)
    public static GuiBridge GuiBridge$invokeInit(String internalName, int internalId, Class containerClass, Class tileClass, GuiHostType type, SecurityPermissions requiredPermission) {
        throw new AssertionError();
    }

    @Unique private static GuiBridge GuiBridge$addPart(String internalName, Class containerClass, Class tileClass, GuiHostType type, SecurityPermissions requiredPermission) {
        ArrayList<GuiBridge> guiList = new ArrayList<>(Arrays.asList(TB_DefineGui.$VALUES));
        GuiBridge newGui = GuiBridge$invokeInit(internalName, guiList.get(guiList.size() - 1).ordinal() + 1, containerClass, tileClass, type, requiredPermission);
        guiList.add(newGui);
        TB_DefineGui.$VALUES = guiList.toArray(new GuiBridge[0]);
        return newGui;
    }
}
