package testbridge.items;

import testbridge.core.TestBridge;
import logisticspipes.interfaces.ILogisticsItem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import network.rs485.logisticspipes.util.TextUtil;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class TBItem extends Item implements ILogisticsItem {

  public TBItem() {
    setCreativeTab(TestBridge.CREATIVE_TAB_TB);
  }

  @Override
  public String getModelPath() {
    String modelFile = getRegistryName().getPath();
    String dir = getModelSubdir();
    if (!dir.isEmpty()) {
      if (modelFile.startsWith(String.format("%s_", dir))) {
        modelFile = modelFile.substring(dir.length() + 1);
      }
      return String.format("%s/%s", dir, modelFile).replaceAll("/+", "/");
    }
    return modelFile;
  }

  public String getModelSubdir() {
    return "";
  }

  public int getModelCount() {
    return 1;
  }

  @Nonnull
  @Override
  public String getTranslationKey(@Nonnull ItemStack stack) {
    if (getHasSubtypes()) {
      return String.format("%s.%d", super.getTranslationKey(stack), stack.getMetadata());
    }
    return super.getTranslationKey(stack);
  }

  /**
   * Adds all keys from the translation file in the format:
   * item.className.tip([0-9]*) Tips start from 1 and increment. Sparse rows
   * should be left empty (ie empty line must still have a key present) Shift
   * shows full tooltip, without it you just get the first line.
   */
  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
    super.addInformation(stack, worldIn, tooltip, flagIn);
    if (addShiftInfo()) {
      TextUtil.addTooltipInformation(stack, tooltip, Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
    }
  }

  public boolean addShiftInfo() {
    return true;
  }

  @Nonnull
  @Override
  public String getItemStackDisplayName(@Nonnull ItemStack itemstack) {
    return I18n.translateToLocal(getTranslationKey(itemstack) + ".name").trim();
  }

}
