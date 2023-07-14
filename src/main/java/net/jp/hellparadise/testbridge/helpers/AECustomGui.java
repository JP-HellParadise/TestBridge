package net.jp.hellparadise.testbridge.helpers;

import appeng.client.gui.AEBaseGui;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Container;

public abstract class AECustomGui extends AEBaseGui {

    protected int guiLeft;
    protected int guiTop;
    protected int xCenter;
    protected int yCenter;
    protected int right;
    protected int bottom;
    protected int xSize;
    protected int ySize;
    protected int xCenterOffset;
    protected int yCenterOffset;

    public AECustomGui(Container container, int xOffset, int yOffset) {
        super(container);
        this.xSize = 150;
        this.ySize = 200;
        this.xCenterOffset = xOffset;
        this.yCenterOffset = yOffset;
    }

    public void initGui() {
        super.initGui();
        this.guiLeft = this.width / 2 - this.xSize / 2 + this.xCenterOffset;
        this.guiTop = this.height / 2 - this.ySize / 2 + this.yCenterOffset;
        this.right = this.width / 2 + this.xSize / 2 + this.xCenterOffset;
        this.bottom = this.height / 2 + this.ySize / 2 + this.yCenterOffset;
        this.xCenter = (this.right + this.guiLeft) / 2;
        this.yCenter = (this.bottom + this.guiTop) / 2;
    }

    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
    }

    public Minecraft getMC() {
        return this.mc;
    }

    public int getGuiLeft() {
        return this.guiLeft;
    }

    public int getGuiTop() {
        return this.guiTop;
    }

    public int getRight() {
        return this.right;
    }

    public int getBottom() {
        return this.bottom;
    }

    public int getXSize() {
        return this.xSize;
    }

    public int getYSize() {
        return this.ySize;
    }
}
