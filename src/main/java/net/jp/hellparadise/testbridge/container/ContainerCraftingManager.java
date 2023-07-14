package net.jp.hellparadise.testbridge.container;

import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.util.IConfigManager;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerUpgradeable;
import appeng.container.slot.SlotRestrictedInput;
import appeng.util.Platform;
import net.jp.hellparadise.testbridge.helpers.interfaces.ICraftingManagerHost;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerCraftingManager extends ContainerUpgradeable {

    @GuiSync(2)
    public YesNo bMode = YesNo.NO;

    public ContainerCraftingManager(final InventoryPlayer ip, final ICraftingManagerHost te) {
        super(
            ip,
            te.getCMDuality()
                .getHost());

        for (int row = 0; row < 3; ++row) {
            for (int x = 0; x < 9; x++) {
                this.addSlotToContainer(
                    new SlotRestrictedInput(
                        SlotRestrictedInput.PlacableItemType.ENCODED_PATTERN,
                        te.getCMDuality()
                            .getPatterns(),
                        x + row * 9,
                        8 + 18 * x,
                        36 + (18 * row),
                        this.getInventoryPlayer()).setStackLimit(1));
            }
        }
    }

    @Override
    protected int getHeight() {
        return 184;
    }

    @Override
    protected void setupConfig() {}

    @Override
    public void detectAndSendChanges() {
        this.verifyPermissions(SecurityPermissions.BUILD, false);

        if (Platform.isServer()) {
            final IConfigManager cm = this.getUpgradeable()
                .getConfigManager();
            this.loadSettingsFromHost(cm);
        }

        super.standardDetectAndSendChanges();
    }

    @Override
    protected void loadSettingsFromHost(final IConfigManager cm) {
        this.setBlockingMode((YesNo) cm.getSetting(Settings.BLOCK));
    }

    public YesNo getBlockingMode() {
        return this.bMode;
    }

    private void setBlockingMode(final YesNo bMode) {
        this.bMode = bMode;
    }
}
