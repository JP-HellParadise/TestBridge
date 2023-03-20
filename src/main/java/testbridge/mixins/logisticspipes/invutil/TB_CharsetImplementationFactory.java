package testbridge.mixins.logisticspipes.invutil;

import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import pl.asie.charset.api.storage.IBarrel;

import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.compat.BarrelInventoryHandler;

import testbridge.helpers.interfaces.TB_IInventoryUtil;

@Mixin(value = BarrelInventoryHandler.class, remap = false)
public abstract class TB_CharsetImplementationFactory implements TB_IInventoryUtil {
  @Shadow(remap = false)
  @Final
  private IBarrel tile;

  @Shadow(remap = false)
  protected abstract boolean isValidItem(ItemIdentifier itemIdent);

  @Shadow(remap = false)
  public abstract int itemCount(ItemIdentifier itemIdent);

  @Override
  public boolean roomForItem(@Nonnull List<ItemStack> list) {
    if (list.size() > 1)
      return false;
    return list.stream().map(ItemIdentifier::get)
        .noneMatch(it -> isValidItem(it) && tile.getMaxItemCount() < itemCount(it));
  }
}
