package testbridge.client.gui.config;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import net.minecraftforge.fml.client.IModGuiFactory;

public class ConfigGuiFactory implements IModGuiFactory {
  @Override
  public void initialize(final Minecraft minecraftInstance) {

  }

  @Override
  public boolean hasConfigGui() {
    return false;
  }

  @Override
  public GuiScreen createConfigGui(GuiScreen parentScreen) {
    return new ConfigGui(parentScreen);
  }

  @Override
  public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
    return null;
  }
}
