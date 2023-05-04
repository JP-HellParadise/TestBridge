package net.jp.hellparadise.testbridge.client.gui;

import java.io.IOException;

import logisticspipes.LPItems;
import logisticspipes.gui.popup.GuiSelectSatellitePopup;
import logisticspipes.items.ItemUpgrade;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.gui.GuiClosePacket;
import logisticspipes.network.packets.module.ModulePropertiesUpdate;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.GuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.UpgradeSlot;
import logisticspipes.utils.gui.extention.GuiExtention;

import net.jp.hellparadise.testbridge.client.popup.GuiSelectResultPopup;
import net.jp.hellparadise.testbridge.core.TB_ItemHandlers;
import net.jp.hellparadise.testbridge.helpers.interfaces.ITranslationKey;
import net.jp.hellparadise.testbridge.helpers.inventory.CrafterSlot;
import net.jp.hellparadise.testbridge.helpers.inventory.DummyContainer;
import net.jp.hellparadise.testbridge.modules.TB_ModuleCM;
import net.jp.hellparadise.testbridge.modules.TB_ModuleCM.BlockingMode;
import net.jp.hellparadise.testbridge.network.guis.pipe.CMGuiProvider;
import net.jp.hellparadise.testbridge.network.packets.pipe.cmpipe.CMGui;
import net.jp.hellparadise.testbridge.network.packets.pipe.cmpipe.SetSatResultPacket;
import net.jp.hellparadise.testbridge.pipes.PipeCraftingManager;
import net.jp.hellparadise.testbridge.pipes.upgrades.ModuleUpgradeManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import network.rs485.logisticspipes.property.EnumProperty;
import network.rs485.logisticspipes.property.PropertyLayer;
import network.rs485.logisticspipes.util.TextUtil;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class GuiCMPipe extends LogisticsBaseGuiScreen implements ITranslationKey {

    private final String PREFIX = "gui.crafting_manager.";
    private final boolean hasBufferUpgrade;
    private final boolean hasContainer;
    private final PipeCraftingManager pipeCM;
    private final Slot[] upgradeSlot;
    private final int[] excludedSlotIDs;
    private final PropertyLayer propertyLayer;
    private final PropertyLayer.ValuePropertyOverlay<BlockingMode, EnumProperty<BlockingMode>> blockingModeOverlay;
    private GuiButton blockingButton;
    private GuiButton extendedButton;
    private int extendedSlot = -1;

    private final Slot fakeSlot;
    private final InventoryBasic inv;

    public GuiCMPipe(EntityPlayer _player, PipeCraftingManager pipeCM, TB_ModuleCM module, boolean flag,
        boolean container) {
        super(null);
        hasBufferUpgrade = flag;
        hasContainer = container;
        this.pipeCM = pipeCM;
        IInventory _moduleInventory = pipeCM.getModuleInventory();

        propertyLayer = new PropertyLayer(module.getProperties());

        // Create dummy container
        DummyContainer dummy = new DummyContainer(_player, _moduleInventory, module);
        dummy.addNormalSlotsForPlayerInventory(8, 16 + 18 * 3 + 15);
        for (int i = 0; i < 3; i++) for (int j = 0; j < 9; j++)
            dummy.addCMModuleSlot(9 * i + j, _moduleInventory, 8 + 18 * j, 16 + 18 * i, this.pipeCM);

        xSize = 177;
        ySize = 167;

        // Create upgrade slot
        upgradeSlot = new Slot[2 * pipeCM.getChassisSize()];
        for (int i = 0; i < pipeCM.getChassisSize(); i++) {
            final int fI = i;
            ModuleUpgradeManager upgradeManager = this.pipeCM.getModuleUpgradeManager(i);
            upgradeSlot[i * 2] = dummy.addUpgradeSlot(
                0,
                upgradeManager,
                0,
                xSize,
                0,
                itemStack -> CMGuiProvider.checkStack(itemStack, this.pipeCM, fI));
            upgradeSlot[i * 2 + 1] = dummy.addUpgradeSlot(
                1,
                upgradeManager,
                1,
                xSize + 18,
                0,
                itemStack -> CMGuiProvider.checkStack(itemStack, this.pipeCM, fI));
        }

        excludedSlotIDs = new int[3];
        for (int x = 0; x < 3; x++) {
            excludedSlotIDs[x] = extentionControllerLeft
                .registerControlledSlot(dummy.addDummySlot(x, module.excludedInventory, x * 18 - 141, 55));
        }

        inventorySlots = dummy;

        blockingModeOverlay = propertyLayer.overlay(module.blockingMode);

        inv = new InventoryBasic("", false, 1);
        fakeSlot = new Slot(inv, 0, 0, 0);
    }

    @Override
    protected void drawSlot(Slot slotIn) {
        ItemStack stack = slotIn.getStack();
        // Either player inv or Crafter Slot
        if ((slotIn.slotNumber < 36 || slotIn instanceof CrafterSlot) && !stack.isEmpty()
            && stack.hasTagCompound()
            && isShiftKeyDown()) {
            NBTTagCompound info = stack.getTagCompound()
                .getCompoundTag("moduleInformation");
            NBTTagList list = info.getTagList("items", 10);
            NBTTagCompound output = null;
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound tag = list.getCompoundTagAt(i);
                if (tag.getInteger("index") == 9) {
                    output = tag;
                }
            }
            if (output != null) {
                inv.setInventorySlotContents(0, new ItemStack(output));
                fakeSlot.xPos = slotIn.xPos;
                fakeSlot.yPos = slotIn.yPos;
                super.drawSlot(fakeSlot);
                inv.setInventorySlotContents(0, ItemStack.EMPTY);
            } else super.drawSlot(slotIn);
        } else super.drawSlot(slotIn);
    }

    @Override
    protected void mouseClicked(int X, int Y, int mouseButton) throws IOException {
        Slot currentSlot = getSlotUnderMouse();
        if (currentSlot instanceof CrafterSlot) {
            switch (mouseButton) {
                case 1:
                    int slotID = currentSlot.getSlotIndex();
                    LogisticsModule module = pipeCM.getSubModule(slotID);
                    if (module instanceof ModuleCrafter) {
                        ModernPacket packet = PacketHandler.getPacket(CMGui.class)
                            .setModulePos(slotID)
                            .setPosX(pipeCM.getX())
                            .setPosY(pipeCM.getY())
                            .setPosZ(pipeCM.getZ());
                        MainProxy.sendPacketToServer(packet);
                    }
                    return;
                case 2:
                    showUpgrade(currentSlot.getSlotIndex());
                    return;
            }
        } else if (GuiScreen.isShiftKeyDown() && !(currentSlot instanceof UpgradeSlot)
            && currentSlot != null
            && currentSlot.getStack()
                .getItem() instanceof ItemUpgrade) {
                    return;
                }
        super.mouseClicked(X, Y, mouseButton);
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        extentionControllerLeft.clear();

        CMExtension extension = new CMExtension("gui.satellite.GuiName", new ItemStack(LPItems.pipeSatellite), 0);
        extension.registerButton(
            extentionControllerLeft.registerControlledButton(
                addButton(
                    new SmallGuiButton(
                        1,
                        guiLeft - 40 / 2 - 18,
                        guiTop + 25,
                        37,
                        10,
                        TextUtil.translate(PREFIX + "Select")))));
        extentionControllerLeft.addExtention(extension);
        extension = new CMExtension("gui.result.GuiName", new ItemStack(TB_ItemHandlers.pipeResult), 1);
        extension.registerButton(
            extentionControllerLeft.registerControlledButton(
                addButton(
                    new SmallGuiButton(
                        2,
                        guiLeft - 40 / 2 - 18,
                        guiTop + 25,
                        37,
                        10,
                        TextUtil.translate(PREFIX + "Select")))));
        extentionControllerLeft.addExtention(extension);

        if (hasBufferUpgrade) {
            BufferExtension buffered = new BufferExtension(new ItemStack(TB_ItemHandlers.upgradeBuffer));
            buffered.registerButton(
                extentionControllerLeft.registerControlledButton(
                    addButton(blockingButton = new GuiButton(4, guiLeft - 143, guiTop + 23, 140, 14, getModeText()))));
            for (int i = 0; i < 3; i++) {
                buffered.registerSlot(excludedSlotIDs[i]);
            }
            extentionControllerLeft.addExtention(buffered);
        }

        if (extendedSlot != -1) {
            extendedButton.x = guiLeft + 4;
            extendedButton.y = guiTop - 21;
            addButton(extendedButton);
        }
    }

    @Override
    protected void actionPerformed(GuiButton guibutton) throws IOException {
        switch (guibutton.id) {
            case 1:
                openSubGuiForSatResultSelection(1);
                break;
            case 2:
                openSubGuiForSatResultSelection(2);
                break;
            case 4:
                if (hasBufferUpgrade) {
                    final BlockingMode newMode = blockingModeOverlay.write(EnumProperty::next);
                    blockingButton.displayString = TextUtil.translate(
                        PREFIX + "blocking."
                            + newMode.toString()
                                .toLowerCase());
                }
                break;
            case 99:
                extendedSlot = -1;
                buttonList.remove(extendedButton);
            default:
                super.actionPerformed(guibutton);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int par1, int par2) {
        super.drawGuiContainerForegroundLayer(par1, par2);
        drawCenteredString(TextUtil.translate(PREFIX + "CMName"), xSize / 2, 5, 0x404040);
        mc.fontRenderer.drawString(TextUtil.translate("key.categories.inventory"), 7, ySize - 93, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int x, int y) {
        GuiGraphics.drawGuiBackGround(mc, guiLeft, guiTop, right, bottom, zLevel, true);
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++) GuiGraphics.drawSlotBackground(mc, guiLeft + 7 + 18 * j, guiTop + 15 + 18 * i);
        GuiGraphics.drawPlayerInventoryBackground(mc, guiLeft + 8, guiTop + ySize - 82);

        for (int i = 0; i < pipeCM.getChassisSize(); i++) {
            if (extendedSlot != i) {
                assert mc.currentScreen != null;
                upgradeSlot[i * 2].xPos = upgradeSlot[i * 2 + 1].xPos = mc.currentScreen.width;
                upgradeSlot[i * 2].yPos = upgradeSlot[i * 2 + 1].yPos = mc.currentScreen.height;
            } else {
                GuiGraphics.drawGuiBackGround(mc, guiLeft, guiTop - 27, guiLeft + 92, guiTop, zLevel, true);
                GuiGraphics.drawSlotBackground(mc, guiLeft + 50, guiTop - 22);
                GuiGraphics.drawSlotBackground(mc, guiLeft + 50 + 18, guiTop - 22);
            }
        }

        super.renderExtentions();
    }

    private void showUpgrade(int slotId) {
        int xPos = 21 + 30;
        int yPos = -21;
        if (extendedSlot == -1) {
            extendedButton = addButton(new GuiButton(99, guiLeft + 4, guiTop - 21, 42, 16, ""));
        }
        if (slotId != extendedSlot) {
            // Show current slot
            extendedButton.displayString = "Slot: " + (slotId < 9 ? "0" + (slotId + 1) : (slotId + 1));
            // Show up next slot
            upgradeSlot[slotId * 2].xPos = xPos;
            upgradeSlot[slotId * 2 + 1].xPos = xPos + 18;
            upgradeSlot[slotId * 2].yPos = upgradeSlot[slotId * 2 + 1].yPos = yPos;
            // Save it for later
            extendedSlot = slotId;
        }
    }

    private void openSubGuiForSatResultSelection(int id) {
        if (pipeCM.getModules()
            .getSlot()
            .isInWorld()) {
            if (id == 1) {
                this.setSubGui(
                    new GuiSelectSatellitePopup(
                        pipeCM.getModules()
                            .getBlockPos(),
                        false,
                        uuid -> MainProxy.sendPacketToServer(
                            PacketHandler.getPacket(SetSatResultPacket.class)
                                .setPipeUUID(uuid)
                                .setInteger(id)
                                .setModulePos(pipeCM.getModules()))));
            } else {
                this.setSubGui(
                    new GuiSelectResultPopup(
                        pipeCM.getModules()
                            .getBlockPos(),
                        uuid -> MainProxy.sendPacketToServer(
                            PacketHandler.getPacket(SetSatResultPacket.class)
                                .setPipeUUID(uuid)
                                .setInteger(id)
                                .setModulePos(pipeCM.getModules()))));
            }
        }
    }

    @Override
    public void onGuiClosed() {
        MainProxy.sendPacketToServer(
            PacketHandler.getPacket(GuiClosePacket.class)
                .setTilePos(pipeCM.container));
        super.onGuiClosed();
        propertyLayer.unregister();
        if (this.mc.player != null && !propertyLayer.getProperties()
            .isEmpty()) {
            // send update to server, when there are changed properties
            MainProxy.sendPacketToServer(
                ModulePropertiesUpdate.fromPropertyHolder(propertyLayer)
                    .setModulePos(pipeCM.getModules()));
        }
    }

    private String getModeText() {
        return TextUtil.translate(
            PREFIX + "blocking."
                + blockingModeOverlay.get()
                    .toString()
                    .toLowerCase());
    }

    private final class CMExtension extends GuiExtention {

        private final ItemStack showItem;
        private final String translationKey;
        private final int guiButton;

        public CMExtension(String translationKey, ItemStack showItem, int guiButton) {
            this.translationKey = translationKey;
            this.showItem = showItem;
            this.guiButton = guiButton;
        }

        @Override
        public int getFinalWidth() {
            return 120;
        }

        @Override
        public int getFinalHeight() {
            return 40;
        }

        @Override
        public void renderForground(int left, int top) {
            if (!isFullyExtended()) {
                GL11.glEnable(GL12.GL_RESCALE_NORMAL);
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
                GL11.glEnable(GL11.GL_LIGHTING);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                RenderHelper.enableGUIStandardItemLighting();
                itemRender.renderItemAndEffectIntoGUI(showItem, left + 5, top + 5);
                itemRender.renderItemOverlayIntoGUI(fontRenderer, showItem, left + 5, top + 5, "");
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                itemRender.zLevel = 0.0F;
            } else {
                mc.fontRenderer.drawString(TextUtil.translate(translationKey), left + 9, top + 8, 0x404040);
                String pipeID = guiButton == 0 ? pipeCM.getModules().clientSideSatResultNames.satelliteName
                    : pipeCM.getModules().clientSideSatResultNames.resultName;
                int maxWidth = 70;
                if (pipeID.isEmpty()) {
                    drawCenteredString(
                        TextUtil.translate(top$cm_prefix + "none"),
                        left + maxWidth / 2 + 7,
                        top + 23,
                        0x404040);
                } else {
                    String name = TextUtil.getTrimmedString(pipeID, maxWidth, mc.fontRenderer, "...");
                    drawCenteredString(name, left + maxWidth / 2 + 7, top + 23, 0x404040);
                }
            }
        }
    }

    private final class BufferExtension extends GuiExtention {

        private final ItemStack showItem;

        public BufferExtension(ItemStack showItem) {
            this.showItem = showItem;
        }

        @Override
        public int getFinalWidth() {
            return 150;
        }

        @Override
        public int getFinalHeight() {
            return 76;
        }

        @Override
        public void renderForground(int left, int top) {
            if (!isFullyExtended()) {
                GL11.glEnable(GL12.GL_RESCALE_NORMAL);
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
                GL11.glEnable(GL11.GL_LIGHTING);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                RenderHelper.enableGUIStandardItemLighting();
                itemRender.renderItemAndEffectIntoGUI(showItem, left + 5, top + 5);
                itemRender.renderItemOverlayIntoGUI(fontRenderer, showItem, left + 5, top + 5, "");
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                itemRender.zLevel = 0.0F;
            } else {
                mc.fontRenderer.drawString(TextUtil.translate(PREFIX + "blocking"), left + 9, top + 8, 0x404040);
                if (hasContainer) {
                    blockingButton.displayString = TextUtil.translate(PREFIX + "NoContainer");
                    blockingButton.enabled = false;
                } else {
                    blockingButton.enabled = true;
                }
                mc.fontRenderer.drawString(TextUtil.translate(PREFIX + "excluded"), left + 9, top + 39, 0x404040);
                for (int x = 0; x < 3; x++) {
                    GuiGraphics.drawSlotBackground(mc, left + 8 + x * 18, top + 50);
                }
            }
        }
    }
}
