package testbridge.client;

import logisticspipes.proxy.MainProxy;
import logisticspipes.textures.Textures;

import testbridge.core.TestBridge;

public class TB_Textures extends Textures {

  public static TextureType TESTBRIDGE_RESULT_TEXTURE = empty;
  public static TextureType TESTBRIDGE_CMPIPE_TEXTURE = empty;

  // Standalone pipes
  public static String TESTBRIDGE_RESULT_TEXTURE_FILE = "pipes/result";
  public static String TESTBRIDGE_CMPIPE_TEXTURE_FILE = "pipes/crafting_manager";

  // Just hidden
  private final static String TESTBRIDGE_OVERLAY_POWERED_TEXTURE_FILE = "pipes/status_overlay/powered-pipe";
  private final static String TESTBRIDGE_OVERLAY_UNPOWERED_TEXTURE_FILE = "pipes/status_overlay/un-powered-pipe";
  private final static String TESTBRIDGE_UN_OVERLAY_TEXTURE_FILE = "pipes/status_overlay/un-overlayed";

  private int index = 0;
  private int newTextureIndex = 0;

  @Override
  public void registerBlockIcons(Object par1IIconRegister) {
    index = 3;
    newTextureIndex = 100;

    // Standalone pipes
    TESTBRIDGE_RESULT_TEXTURE = registerTexture(par1IIconRegister, TB_Textures.TESTBRIDGE_RESULT_TEXTURE_FILE, 1);
    TESTBRIDGE_CMPIPE_TEXTURE = registerTexture(par1IIconRegister, TB_Textures.TESTBRIDGE_CMPIPE_TEXTURE_FILE, 1);

    if (TestBridge.isDebug()) {
      System.out.println("TB: pipetextures " + index);
    }
  }

  @SuppressWarnings("deprecation") // Removed if LP get update to newer version
  private TextureType registerTexture(Object par1IIconRegister, String fileName, int flag) {
    TextureType texture = new TextureType();
    texture.normal = index++;
    texture.powered = texture.normal;
    texture.unpowered = texture.normal;
    texture.fileName = fileName;
    boolean isClient = MainProxy.isClient();
    if (isClient) {
      MainProxy.proxy.addLogisticsPipesOverride(par1IIconRegister, texture.normal, fileName, TESTBRIDGE_UN_OVERLAY_TEXTURE_FILE, (flag == 2));
    }
    if (flag == 1) {
      texture.powered = index++;
      texture.unpowered = index++;
      if (isClient) {
        MainProxy.proxy.addLogisticsPipesOverride(par1IIconRegister, texture.powered, fileName, TESTBRIDGE_OVERLAY_POWERED_TEXTURE_FILE, false);
        MainProxy.proxy.addLogisticsPipesOverride(par1IIconRegister, texture.unpowered, fileName, TESTBRIDGE_OVERLAY_UNPOWERED_TEXTURE_FILE, false);
      }
      if (!fileName.contains("status_overlay")) {
        texture.newTexture = newTextureIndex++;
        MainProxy.proxy.addLogisticsPipesOverride(par1IIconRegister, texture.newTexture, fileName, "NewPipeTexture", true);
      }
    }
    return texture;
  }
}
