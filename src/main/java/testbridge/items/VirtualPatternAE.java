package testbridge.items;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.AppEng;
import appeng.core.localization.GuiText;
import appeng.helpers.InvalidPatternHelper;
import appeng.util.Platform;

import testbridge.helpers.VirtualPatternHelper;

@Optional.Interface(iface = "appeng.api.implementations.ICraftingPatternItem", modid = "appliedenergistics2")
public class VirtualPatternAE extends Item implements ICraftingPatternItem {
  // rather simple client side caching.
  private static final Map<ItemStack, ItemStack> SIMPLE_CACHE = new WeakHashMap<>();

  public VirtualPatternAE() {
    this.setMaxStackSize(64);
  }

  @Override
  @SideOnly(Side.CLIENT)
  @Optional.Method(modid = "appliedenergistics2")
  public void addInformation(final ItemStack stack, final World world, final List<String> lines, final ITooltipFlag advancedTooltips) {
    final ICraftingPatternDetails details = this.getPatternForItem(stack, world);

    if (details == null) {
      if (!stack.hasTagCompound()) {
        return;
      }

      stack.setStackDisplayName(TextFormatting.RED + GuiText.InvalidPattern.getLocal());

      InvalidPatternHelper invalid = new InvalidPatternHelper(stack);

      final String label = (invalid.isCraftable() ? GuiText.Crafts.getLocal() : GuiText.Creates.getLocal()) + ": ";
      final String and = ' ' + GuiText.And.getLocal() + ' ';
      final String with = GuiText.With.getLocal() + ": ";

      boolean first = true;
      for (final InvalidPatternHelper.PatternIngredient output : invalid.getOutputs()) {
        lines.add((first ? label : and) + output.getFormattedToolTip());
        first = false;
      }

      first = true;
      for (final InvalidPatternHelper.PatternIngredient input : invalid.getInputs()) {
        lines.add((first ? with : and) + input.getFormattedToolTip());
        first = false;
      }

      if (invalid.isCraftable()) {
        final String substitutionLabel = GuiText.Substitute.getLocal() + " ";
        final String canSubstitute = invalid.canSubstitute() ? GuiText.Yes.getLocal() : GuiText.No.getLocal();

        lines.add(substitutionLabel + canSubstitute);
      }

      return;
    }

    if (stack.hasDisplayName()) {
      stack.removeSubCompound("display");
    }

    final boolean isCrafting = details.isCraftable();
    final boolean substitute = details.canSubstitute();

    final IAEItemStack[] in = details.getCondensedInputs();
    final IAEItemStack[] out = details.getCondensedOutputs();

    final String label = (isCrafting ? GuiText.Crafts.getLocal() : GuiText.Creates.getLocal()) + ": ";
    final String and = ' ' + GuiText.And.getLocal() + ' ';
    final String with = GuiText.With.getLocal() + ": ";

    boolean first = true;
    for (final IAEItemStack anOut : out) {
      if (anOut == null) {
        continue;
      }

      lines.add((first ? label : and) + anOut.getStackSize() + ' ' + Platform.getItemDisplayName(anOut));
      first = false;
    }

    first = true;
    for (final IAEItemStack anIn : in) {
      if (anIn == null) {
        continue;
      }

      lines.add((first ? with : and) + anIn.getStackSize() + ' ' + Platform.getItemDisplayName(anIn));
      first = false;
    }

    if (isCrafting) {
      final String substitutionLabel = GuiText.Substitute.getLocal() + " ";
      final String canSubstitute = substitute ? GuiText.Yes.getLocal() : GuiText.No.getLocal();

      lines.add(substitutionLabel + canSubstitute);
    }
  }

  @Override
  @Optional.Method(modid = "appliedenergistics2")
  public ICraftingPatternDetails getPatternForItem(final ItemStack is, final World w) {
    try {
      return new VirtualPatternHelper(is);
    } catch (final Throwable t) {
      return null;
    }
  }

  /**
   * Create new virtual pattern 1:1 to original AE2 version with custom handle.
   * <p>
   * Usage: To create {@link FakeItem} that contains ItemStack
   * @param in Input
   * @param out Output
   * @return {@link VirtualPatternHelper}
   */
  @Optional.Method(modid = "appliedenergistics2")
  public static ICraftingPatternDetails newPattern(final ItemStack in, final ItemStack out) {
    return new VirtualPatternHelper(in, out);
  }

  @Optional.Method(modid = "appliedenergistics2")
  public static ICraftingPatternDetails newPattern(final ItemStack is) {
    return new VirtualPatternHelper(is);
  }

  @Optional.Method(modid = "appliedenergistics2")
  public ItemStack getOutput(final ItemStack item) {
    ItemStack out = SIMPLE_CACHE.get(item);

    if (out != null) {
      return out;
    }

    final World w = AppEng.proxy.getWorld();
    if (w == null) {
      return ItemStack.EMPTY;
    }

    final ICraftingPatternDetails details = this.getPatternForItem(item, w);

    out = details != null ? details.getOutputs()[0].createItemStack() : ItemStack.EMPTY;

    SIMPLE_CACHE.put(item, out);
    return out;
  }
}
