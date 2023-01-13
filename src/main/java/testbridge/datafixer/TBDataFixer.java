package testbridge.datafixer;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ModFixs;
import net.minecraftforge.fml.common.FMLCommonHandler;

import testbridge.core.TestBridge;

public class TBDataFixer {

  public static final TBDataFixer INSTANCE = new TBDataFixer();

  public static final int VERSION = 1;

  private TBDataFixer() {}

  public void init() {
    ModFixs mf = FMLCommonHandler.instance().getDataFixer().init(TestBridge.MODID, VERSION);
    mf.registerFix(DataFixerTE.TYPE, new DataFixerTE());
    mf.registerFix(DataFixerItem.TYPE, new DataFixerItem());
    MinecraftForge.EVENT_BUS.register(new MissingMappingHandler());
  }

}
