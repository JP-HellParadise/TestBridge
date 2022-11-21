package testbridge.gui;

import logisticspipes.LPItems;
import logisticspipes.gui.modules.ModuleBaseGui;
import logisticspipes.kotlin.Unit;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.gui.GuiClosePacket;
import logisticspipes.network.packets.module.ModulePropertiesUpdate;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.GuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.extention.GuiExtention;

import network.rs485.logisticspipes.property.Property;
import network.rs485.logisticspipes.property.PropertyLayer;
import network.rs485.logisticspipes.util.TextUtil;

import lombok.Getter;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import testbridge.core.TBItems;
import testbridge.gui.popup.GuiSelectResultPopup;
import testbridge.modules.CMPipeSetSatResultPacket;
import testbridge.modules.TB_ModuleCM;
import testbridge.network.packets.craftingmanager.CMGui;
import testbridge.pipes.PipeCraftingManager;
import testbridge.utils.gui.DummyContainer;
import testbridge.utils.gui.ModuleSlot;

import java.io.IOException;

public class GuiCMPipe extends ModuleBaseGui {

  private static final String PREFIX = "gui.craftingmanager.";

  //Basic modules slot
  @Getter
  private final PipeCraftingManager _cmPipe;

  @Getter
  private final TB_ModuleCM module;

  private final IInventory _moduleInventory;

  //Advanced stuff
  private final EntityPlayer _player;
  private final PropertyLayer propertyLayer;

  public GuiCMPipe(EntityPlayer player, TB_ModuleCM module, PipeCraftingManager chassis, boolean bufferExclude) {
    //Classis chassis
    super(null, module);
    _cmPipe = chassis;
    _moduleInventory = chassis.getModuleInventory();
    _player = player;

    xSize = 177;
    ySize = 167;

    DummyContainer dummy = new DummyContainer(player.inventory, _moduleInventory);
    dummy.addNormalSlotsForPlayerInventory(8, 16 + 18 * 3 + 15);

    for (int i = 0; i < 3; i++)
      for (int j = 0; j < 9; j++)
        dummy.addCMModuleSlot(9*i+j, _moduleInventory, 8 + 18*j, 16 + 18*i, _cmPipe);

    inventorySlots = dummy;

    //Advanced stuff
    this.module = module;

    module.bufferModeIsExclude.setValue(bufferExclude);

    propertyLayer = new PropertyLayer(module.getProperties());
    propertyLayer.addObserver(module.bufferModeIsExclude, this::updateBufferModeButton);

    inventorySlots = dummy;
  }

  private boolean isCrafterOnHand(Slot slotID) {
    ItemStack onHand = this.mc.player.inventory.getItemStack();
    return (onHand.getItem() == Item.REGISTRY.getObject(LPItems.modules.get(ModuleCrafter.getName())));
  }

  protected void windowClick(int id, int btn, ClickType clickType) {
    mc.playerController.windowClick(this.inventorySlots.windowId, id, btn, clickType, _player);
  }

  @Override
  protected void mouseClicked(int X, int Y, int mouseButton) throws IOException {
    // Fetch current interactive slot and get slot id
    Slot currentSlot = getSlotUnderMouse();
    if (!(currentSlot instanceof ModuleSlot) || mouseButton == 0) {
      super.mouseClicked(X, Y, mouseButton);
      return;
    }
    int slotID = currentSlot.getSlotIndex();
    LogisticsModule module = _cmPipe.getSubModule(slotID);
    if (mouseButton == 1) {
      if (module instanceof ModuleCrafter && isCrafterOnHand(currentSlot)) {
        ModernPacket packet = PacketHandler.getPacket(CMGui.class).setSlotID(slotID).setPosX(_cmPipe.getX()).setPosY(_cmPipe.getY()).setPosZ(_cmPipe.getZ());
        MainProxy.sendPacketToServer(packet);
      }
    }
  }

  @Override
  public void initGui() {
    super.initGui();

    GuiButton normalButtonArray = new SmallGuiButton(0, guiLeft - 40 / 2 - 18, guiTop + 158, 37, 10, TextUtil.translate(PREFIX + "Select"));

    buttonList.clear();
    extentionControllerLeft.clear();

    CMExtention extention = new CMExtention("gui.craftingManager.satellite" , TB_ModuleCM.clientSideSatResultNames.CM$satelliteName, new ItemStack(LPItems.pipeSatellite));
    extention.registerButton(extentionControllerLeft.registerControlledButton(addButton(normalButtonArray)));
    extentionControllerLeft.addExtention(extention);
    extention = new CMExtention("gui.craftingManager.result" , TB_ModuleCM.clientSideSatResultNames.resultName, new ItemStack(TBItems.pipeResult));
    extention.registerButton(extentionControllerLeft.registerControlledButton(addButton(normalButtonArray)));
    extentionControllerLeft.addExtention(extention);

  }

  protected void actionPerformed(GuiButton guibutton) throws IOException {
    switch (guibutton.id) {
      case 0:
        openSubGuiForSatResultSelection(0);
        break;
      case 2:
        openSubGuiForSatResultSelection(1);
        break;
//      case 24:
//        bufferModeIsExcludeOverlay.write(BooleanProperty::toggle);
//        break;
//      case 25:
//        bufferModeIsExcludeOverlay.set(false);
//        MainProxy.sendPacketToServer(PacketHandler.getPacket(CPipeCleanupImport.class).setModulePos(craftingModule));
//        break;
      default:
        super.actionPerformed(guibutton);
    }
  }

  @Override
  protected void drawGuiContainerForegroundLayer(int par1, int par2) {
    super.drawGuiContainerForegroundLayer(par1, par2);
    drawCenteredString("Crafting Manager", xSize / 2, 5, 0x404040);
    mc.fontRenderer.drawString("Inventory", 3, ySize - 93, 0x404040);
  }

  @Override
  protected void drawGuiContainerBackgroundLayer(float f, int x, int y) {
    GuiGraphics.drawGuiBackGround(mc, guiLeft, guiTop, right, bottom, zLevel, true);
    for (int i = 0; i < 3; i++)
      for (int j = 0; j < 9; j++)
        GuiGraphics.drawSlotBackground(mc, guiLeft + 7 + 18 * j, guiTop + 15 + 18 * i);
    GuiGraphics.drawPlayerInventoryBackground(mc, guiLeft + 8, guiTop + ySize - 82);
  }

  private void openSubGuiForSatResultSelection(int id) {
    if (module.getSlot().isInWorld()) {
      this.setSubGui(new GuiSelectResultPopup(module.getBlockPos(), uuid ->
          MainProxy.sendPacketToServer(PacketHandler.getPacket(CMPipeSetSatResultPacket.class).setPipeID(uuid).setInteger(id).setModulePos(module))));
    }
  }

  @Override
  public void onGuiClosed() {
    MainProxy.sendPacketToServer(PacketHandler.getPacket(GuiClosePacket.class).setTilePos(_cmPipe.container));
    super.onGuiClosed();
    propertyLayer.unregister();
    if (this.mc.player != null && !propertyLayer.getProperties().isEmpty()) {
      // send update to server, when there are changed properties
      MainProxy.sendPacketToServer(ModulePropertiesUpdate.fromPropertyHolder(propertyLayer).setModulePos(module));
    }
  }

  private Unit updateBufferModeButton(Property<Boolean> prop) {
    return null;
  }

  private final class CMExtention extends GuiExtention {
    private final ItemStack showItem;
    private final String translationKey;
    private final String pipeID;

    public CMExtention(String translationKey, String pipeID, ItemStack showItem) {
      this.translationKey = translationKey;
      this.pipeID = pipeID;
      this.showItem = showItem;
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
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240 / 1.0F, 240 / 1.0F);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        RenderHelper.enableGUIStandardItemLighting();
        itemRender.renderItemAndEffectIntoGUI(showItem, left + 5, top + 5);
        itemRender.renderItemOverlayIntoGUI(fontRenderer, showItem, left + 5, top + 5, "");
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        mc.fontRenderer.drawStringWithShadow("2", left + 22 - fontRenderer.getStringWidth("2"), top + 14, 16777215);
      }
      itemRender.zLevel = 0.0F;

      if (isFullyExtended()) {
        mc.fontRenderer.drawString(TextUtil.translate(translationKey), left + 9, top + 8, 0x404040);
        if (pipeID == null || pipeID.isEmpty()) {
          mc.fontRenderer.drawString(TextUtil.translate(GuiCMPipe.PREFIX + "Off"), left + 40 / 2 - 5, top + 145, 0x404040);
        } else {
          mc.fontRenderer.drawString(pipeID, left + 40 / 2 + 3 - (fontRenderer.getStringWidth(pipeID) / 2), top + 145, 0x404040);
        }
      }
    }
  }
}

