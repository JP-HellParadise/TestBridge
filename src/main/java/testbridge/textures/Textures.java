package testbridge.textures;

import logisticspipes.proxy.MainProxy;

import testbridge.core.TestBridge;

public class Textures extends logisticspipes.textures.Textures {

  public Textures() {
  }
  public static TextureType TESTBRIDGE_RESULT_TEXTURE = empty;
  public static TextureType TESTBRIDGE_CMPIPE_TEXTURE = empty;

  // Standalone pipes
  public static String TESTBRIDGE_RESULT_TEXTURE_FILE = "pipes/result";
  public static String TESTBRIDGE_CMPIPE_TEXTURE_FILE = "pipes/crafting_manager";

  private int index = 0;
  private int newTextureIndex = 0;

  @Override
  public void registerBlockIcons(Object par1IIconRegister) {
    index = 3;
    newTextureIndex = 0;

    // Standalone pipes
    TESTBRIDGE_RESULT_TEXTURE = registerTexture(par1IIconRegister, Textures.TESTBRIDGE_RESULT_TEXTURE_FILE, "", "", "");
    TESTBRIDGE_CMPIPE_TEXTURE = registerTexture(par1IIconRegister, Textures.TESTBRIDGE_CMPIPE_TEXTURE_FILE, "", "", "");


    if (TestBridge.isDebug()) {
      System.out.println("LP: pipetextures " + index);
    }
  }

  private TextureType registerTexture(Object par1IIconRegister, String fileName, String unOverlay, String poweredOverlay, String unpoweredOverlay) {
    if (unOverlay.equals("") && poweredOverlay.equals("") && unpoweredOverlay.equals("")) {
      return registerTexture(par1IIconRegister, fileName, 1);
    }
    return registerTexture(par1IIconRegister, fileName, 1, unOverlay, poweredOverlay, unpoweredOverlay);
  }

  private TextureType registerTexture(Object par1IIconRegister, String fileName, int flag) {
    TextureType texture = new TextureType();
    texture.normal = index++;
    texture.powered = texture.normal;
    texture.unpowered = texture.normal;
    texture.fileName = fileName;
    boolean isClient = MainProxy.isClient();
    if (isClient) {
      MainProxy.proxy.addLogisticsPipesOverride(par1IIconRegister, texture.normal, fileName, logisticspipes.textures.Textures.LOGISTICSPIPE_UN_OVERLAY_TEXTURE_FILE, (flag == 2));
    }
    if (flag == 1) {
      texture.powered = index++;
      texture.unpowered = index++;
      if (isClient) {
        MainProxy.proxy.addLogisticsPipesOverride(par1IIconRegister, texture.powered, fileName, logisticspipes.textures.Textures.LOGISTICSPIPE_OVERLAY_POWERED_TEXTURE_FILE, false);
        MainProxy.proxy.addLogisticsPipesOverride(par1IIconRegister, texture.unpowered, fileName, logisticspipes.textures.Textures.LOGISTICSPIPE_OVERLAY_UNPOWERED_TEXTURE_FILE, false);
      }
      if (!fileName.contains("status_overlay")) {
        texture.newTexture = newTextureIndex++;
        MainProxy.proxy.addLogisticsPipesOverride(par1IIconRegister, texture.newTexture, fileName, "NewPipeTexture", true);
      }
    }
    return texture;
  }

  private TextureType registerTexture(Object par1IIconRegister, String fileName, int flag, String unOverlay, String poweredOverlay, String unpoweredOverlay) {
    TextureType texture = new TextureType();
    texture.normal = index++;
    texture.powered = texture.normal;
    texture.unpowered = texture.normal;
    texture.fileName = fileName;
    boolean isClient = MainProxy.isClient();
    if (isClient) {
      MainProxy.proxy.addLogisticsPipesOverride(par1IIconRegister, texture.normal, fileName, unOverlay, (flag == 2));
    }
    if (flag == 1) {
      texture.powered = index++;
      texture.unpowered = index++;
      if (isClient) {
        MainProxy.proxy.addLogisticsPipesOverride(par1IIconRegister, texture.powered, fileName, poweredOverlay, false);
        MainProxy.proxy.addLogisticsPipesOverride(par1IIconRegister, texture.unpowered, fileName, unpoweredOverlay, false);
      }
      if (!fileName.contains("status_overlay")) {
        texture.newTexture = newTextureIndex++;
        MainProxy.proxy.addLogisticsPipesOverride(par1IIconRegister, texture.newTexture, fileName, "NewPipeTexture", true);
      }
    }
    return texture;
  }

}
