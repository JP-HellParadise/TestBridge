package testbridge.gui;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

import org.lwjgl.input.Keyboard;

import lombok.Getter;

import mezz.jei.api.gui.IGhostIngredientHandler;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.interfaces.IJEIGhostIngredients;
import appeng.container.slot.IJEITargetSlot;
import appeng.container.slot.SlotFake;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.helpers.InventoryAction;
import appeng.util.item.AEItemStack;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.items.ItemStackHandler;

import logisticspipes.utils.gui.GuiGraphics;

import testbridge.container.ContainerPackage;
import testbridge.items.FakeItem;

public class GuiPackage extends AEBaseGui implements IJEIGhostIngredients {

  private final Map<IGhostIngredientHandler.Target<?>, Object> mapTargetSlot = new HashMap<>();
  private MEGuiTextField textField;
  private GuiButton saveButton;
  @Getter
  private final Item itemPackage;
  private final EntityPlayer player;
  private final ItemStackHandler fakeSlot;
  public GuiPackage(Item item, EntityPlayer player, ContainerPackage container) {
    super(container = new ContainerPackage(player));
    this.itemPackage = item;
    this.player = player;
    this.fakeSlot = container.getFakeSlot();
  }

  @Override
  public void initGui() {
    super.initGui();

    this.textField = new MEGuiTextField(this.fontRenderer, this.guiLeft + 26, this.guiTop + 33, 113, 12);

    this.textField.setEnableBackgroundDrawing(false);

    this.textField.setFocused(true);

    this.buttonList.add(this.saveButton = new GuiButton(0, this.guiLeft + 140, this.guiTop + 32, 30, 14, I18n.format("gui.item_package.Save")));

    ((ContainerPackage) this.inventorySlots).setTextField(this.textField);
  }

  @Override
  public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
    this.fontRenderer.drawString(I18n.format("item.testbridge.item_package.name"), 12, 8, 4210752);
  }

  @Override
  public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
    // Package inventory
    GuiGraphics.drawGuiBackGround(mc, guiLeft, guiTop + ySize - 166, guiLeft + 177, guiTop + ySize - 110, zLevel, true);
    GuiGraphics.drawSlotBackground(mc, guiLeft + 7, guiTop + 29); //FakeSlot

    // Player inventory
    GuiGraphics.drawGuiBackGround(mc, guiLeft - 1, guiTop + ySize - 80, guiLeft + 177, guiTop + ySize + 18, zLevel, true);
    GuiGraphics.drawPlayerInventoryBackground(mc, guiLeft + 8, guiTop + ySize - 64);
    this.textField.drawTextBox();
  }

  @Override
  protected void mouseClicked(final int xCoord, final int yCoord, final int btn) throws IOException {
    if (this.textField.isMouseIn(xCoord, yCoord)) {
      if (btn == 1) {
        this.textField.setText("");
      }
      this.textField.mouseClicked(xCoord, yCoord, btn);
    }
    super.mouseClicked(xCoord, yCoord, btn);
  }

  @Override
  protected void keyTyped(final char character, final int key) throws IOException {
    if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { // Enter
      updatePkg();
      this.mc.player.closeScreen();
    } else if (!this.textField.textboxKeyTyped(character, key)) {
      super.keyTyped(character, key);
    }
  }

  @Override
  protected void actionPerformed(final GuiButton btn) throws IOException {
    super.actionPerformed(btn);
    if (btn == this.saveButton) {
      updatePkg();
      this.mc.player.closeScreen();
    }
  }

  @Override
  public void bindTexture(String file) {
    ResourceLocation loc = new ResourceLocation("testbridge", "textures/" + file);
    this.mc.getTextureManager().bindTexture(loc);
  }

  @Override
  public List<IGhostIngredientHandler.Target<?>> getPhantomTargets(Object catalyst) {
    mapTargetSlot.clear();

    ItemStack itemStack = ItemStack.EMPTY;

    if (catalyst instanceof ItemStack) {
      itemStack = (ItemStack) catalyst;
    }

    if (!(catalyst instanceof ItemStack)) {
      return Collections.emptyList();
    }

    List<IGhostIngredientHandler.Target<?>> targets = new ArrayList<>();

    List<IJEITargetSlot> slots = new ArrayList<>();

    if (this.inventorySlots.inventorySlots.size() > 0) {
      for (Slot slot : this.inventorySlots.inventorySlots) {
        if (slot instanceof SlotFake && (!itemStack.isEmpty())) {
          slots.add((IJEITargetSlot) slot);
        }
      }
    }

    for (Object slot : slots) {
      ItemStack finalItemStack = itemStack;
      IGhostIngredientHandler.Target<Object> targetItem = new IGhostIngredientHandler.Target<Object>() {
        @Override
        public Rectangle getArea() {
          if (slot instanceof SlotFake && ((SlotFake) slot).isSlotEnabled()) {
            return new Rectangle(getGuiLeft() + ((SlotFake) slot).xPos, getGuiTop() + ((SlotFake) slot).yPos, 16, 16);
          }
          return new Rectangle();
        }

        @Override
        public void accept(Object ingredient) {
          PacketInventoryAction p = null;
          try {
            if (slot instanceof SlotFake && ((SlotFake) slot).isSlotEnabled()) {
              if (!finalItemStack.isEmpty()) {
                p = new PacketInventoryAction(InventoryAction.PLACE_JEI_GHOST_ITEM, (IJEITargetSlot) slot, AEItemStack.fromItemStack(finalItemStack));
              }
            } else return;
            NetworkHandler.instance().sendToServer(p);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      };
      targets.add(targetItem);
      mapTargetSlot.putIfAbsent(targetItem, slot);
    }
    return targets;
  }

  @Override
  public Map<IGhostIngredientHandler.Target<?>, Object> getFakeSlotTargetMap() {
    return mapTargetSlot;
  }

  private void updatePkg() {
    ItemStack is = player.inventory.getStackInSlot(player.inventory.currentItem);
    if (is.getItem() instanceof FakeItem && (!is.hasTagCompound() || (is.hasTagCompound() && !is.getTagCompound().getBoolean("__actContainer")))) {
      is.setTagCompound(new NBTTagCompound());
      is.getTagCompound().setBoolean("__actContainer", false);
      if (!this.textField.getText().equals(""))
        is.getTagCompound().setString("__pkgDest", this.textField.getText());
      is.getTagCompound().setTag("__itemHold", fakeSlot.getStackInSlot(0).writeToNBT(new NBTTagCompound()));
    }
  }
}
