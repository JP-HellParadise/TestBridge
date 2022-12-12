package testbridge.helpers.interfaces;

import net.minecraft.item.Item;

public interface IProxy {

  void registerRenderers();
  void registerTextures();
  void addRenderer(Item item);

}
