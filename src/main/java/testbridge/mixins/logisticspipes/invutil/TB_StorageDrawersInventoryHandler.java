package testbridge.mixins.logisticspipes.invutil;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup;

import logisticspipes.kotlin.collections.CollectionsKt;
import network.rs485.logisticspipes.proxy.StorageDrawersInventoryHandler;

import testbridge.helpers.interfaces.TB_IInventoryUtil;

@Mixin(value = StorageDrawersInventoryHandler.class, remap = false)
public abstract class TB_StorageDrawersInventoryHandler implements TB_IInventoryUtil {
  @Shadow(remap = false)
  protected abstract List<Integer> accessibleDrawerSlots();

  @Shadow(remap = false)
  @Final
  private IDrawerGroup drawerGroup;

  @Override
  public boolean roomForItem(@Nonnull List<ItemStack> list) {
    for (ItemStack stack : list) {
      if (CollectionsKt.sumOfInt(
          accessibleDrawerSlots().stream()
              .map(slot -> drawerGroup.getDrawer(slot).isEnabled()
                  && drawerGroup.getDrawer(slot).canItemBeStored(stack)
                  ? drawerGroup.getDrawer(slot).getAcceptingRemainingCapacity() : 0).collect(Collectors.toList())) != stack.getCount())
        return false;
    }
    return true;
  }
}
