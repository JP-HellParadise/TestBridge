package net.jp.hellparadise.testbridge.container;

import net.jp.hellparadise.testbridge.helpers.interfaces.ICraftingManagerHost;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;

import appeng.api.config.SecurityPermissions;
import appeng.api.parts.IPart;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.util.Platform;

public class ContainerSatelliteSelect extends AEBaseContainer {

    private final ICraftingManagerHost cmHost;
    @GuiSync(2)
    public String satName = "";

    public ContainerSatelliteSelect(final InventoryPlayer ip, final ICraftingManagerHost te) {
        super(ip, (TileEntity) (te instanceof TileEntity ? te : null), (IPart) (te instanceof IPart ? te : null));
        this.cmHost = te;
    }

    public void setSatName(final String newValue) {
        this.cmHost.setSatellite(newValue);
        this.satName = newValue;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        this.verifyPermissions(SecurityPermissions.BUILD, false);

        if (Platform.isServer()) {
            this.satName = this.cmHost.getSatelliteName();
        }
    }

    @Override
    public void onUpdate(final String field, final Object oldValue, final Object newValue) {
        super.onUpdate(field, oldValue, newValue);
    }

    public ICraftingManagerHost getCMHost() {
        return this.cmHost;
    }
}
