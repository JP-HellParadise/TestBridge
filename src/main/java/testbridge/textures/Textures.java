package testbridge.textures;

import testbridge.core.TestBridge;
import logisticspipes.proxy.MainProxy;
import logisticspipes.textures.provider.LPPipeIconTransformerProvider;

public class Textures extends logisticspipes.textures.Textures {

  public static LPPipeIconTransformerProvider LPnewPipeIconProvider;

  public Textures() {

  }
  public static TextureType TESTBRIDGE_RESULT_TEXTURE = empty;

  // Standalone pipes
  public static String TESTBRIDGE_RESULT_TEXTURE_FILE = "pipes/result";

  private int index = 0;
  private int newTextureIndex = 0;

  @Override
  public void registerBlockIcons(Object par1IIconRegister) {
    index = 3;
    newTextureIndex = 0;

    // Standalone pipes
    Textures.TESTBRIDGE_RESULT_TEXTURE = registerTexture(par1IIconRegister, Textures.TESTBRIDGE_RESULT_TEXTURE_FILE);

    if (TestBridge.isDebug()) {
      System.out.println("LP: pipetextures " + index);
    }
  }


  //Reflect method
  private TextureType registerTexture(Object par1IIconRegister, String fileName) {
    return registerTexture(par1IIconRegister, fileName, 1);
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

}
