package testbridge.client.gui.config;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import testbridge.core.TBConfig;
import testbridge.core.TestBridge;

public class ConfigGui extends GuiConfig {
  public ConfigGui(final GuiScreen parent) {
    super(parent, getConfigElements(), TestBridge.MODID, false, false, GuiConfig.getAbridgedConfigPath(TBConfig.instance().getFilePath()));
  }

  private static List<IConfigElement> getConfigElements() {
    final List<IConfigElement> list = new ArrayList<>();

    final ConfigCategory cc = TBConfig.instance().getCategory("Logging");

    final ConfigElement ce = new ConfigElement(cc);
    list.add(ce);

    return list;
  }
}
