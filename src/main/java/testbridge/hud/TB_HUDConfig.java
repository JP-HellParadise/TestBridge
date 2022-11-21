package testbridge.hud;

import logisticspipes.hud.HUDConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import testbridge.interfaces.TB_IHUDConfig;

import javax.annotation.Nonnull;

public class TB_HUDConfig extends HUDConfig implements TB_IHUDConfig {

  private NBTTagCompound configTag;

  public TB_HUDConfig(@Nonnull ItemStack stack) {
    this(stack.getTagCompound());
    stack.setTagCompound(configTag);
  }

  public TB_HUDConfig(NBTTagCompound tag) {
    super(tag);
    configTag = tag;
    if (configTag == null) {
      configTag = new NBTTagCompound();
    }

    if (configTag.isEmpty()) {
      configTag.setBoolean("HUDCrafting", true);
    }
  }

  @Override
  public boolean isHUDCraftingManager() {
    return configTag.getBoolean("HUDCraftingManager");
  }

  @Override
  public void setHUDCraftingManager(boolean flag) {
    configTag.setBoolean("HUDCraftingManager", flag);
  }
}
