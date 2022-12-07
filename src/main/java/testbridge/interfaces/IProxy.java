package testbridge.interfaces;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public interface IProxy {

  void registerRenderers();
  void registerTextures();
  void addRenderer(Item item);
  void addRenderer(ItemStack item, String name);
  void addRenderer(Item item, String name);
}
