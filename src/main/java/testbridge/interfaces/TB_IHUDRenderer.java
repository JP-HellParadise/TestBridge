package testbridge.interfaces;

import net.minecraft.client.Minecraft;

public interface TB_IHUDRenderer {
  void renderHeadUpDisplay(double var1, boolean var3, boolean var4, Minecraft var5, TB_IHUDConfig var6);

  boolean display(TB_IHUDConfig var1);

  boolean cursorOnWindow(int var1, int var2);

  void handleCursor(int var1, int var2);
}
