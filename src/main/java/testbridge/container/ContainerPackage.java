package testbridge.container;

import lombok.Getter;
import lombok.Setter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemStackHandler;

import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotFake;

import testbridge.items.FakeItem;

public class ContainerPackage extends AEBaseContainer {

  @SideOnly(Side.CLIENT)
  private MEGuiTextField textField;

  private final int slotId;
  @Getter
  @Setter
  private String satelliteName;

  @Getter
  private final ItemStackHandler fakeSlot = new ItemStackHandler(1);

  public ContainerPackage(final EntityPlayer ip) {
    super(ip.inventory, null, null);
    this.slotId = ip.inventory.currentItem;

    ItemStack is = ip.inventory.getStackInSlot(slotId);

    final int y = 30;
    final int x = 8;

    if(is.getItem() instanceof FakeItem) {
      this.addSlotToContainer(new SlotFake(fakeSlot, 0, x, y));
      if (is.hasTagCompound()) {
        satelliteName = is.getTagCompound().getString("__pkgDest");
        fakeSlot.setStackInSlot(0, new ItemStack(is.getTagCompound().getCompoundTag("__itemHold")));
      }
    }

    this.lockPlayerInventorySlot(slotId);

    this.bindPlayerInventory(ip.inventory, 0, this.getHeight() - /* height of player inventory */82);
  }

  @SideOnly(Side.CLIENT)
  public void setTextField(final MEGuiTextField name) {
    this.textField = name;
    if (satelliteName != null) textField.setText(satelliteName);
    else textField.setText("");
  }

  @Override
  public boolean canInteractWith(EntityPlayer entityPlayer) {
    if (this.isValidContainer()) {
      return entityPlayer.inventory.getStackInSlot(slotId).getItem() instanceof FakeItem;
    }
    return false;
  }

  protected int getHeight() {
    return 184;
  }
}
