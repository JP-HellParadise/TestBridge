package testbridge.helpers.interfaces;

import logisticspipes.LPConstants;

import testbridge.core.TestBridge;

public interface ITranslationKey {
  String top$sat_prefix = "top." + LPConstants.LP_MOD_ID + ".pipe.satellite.";
  String top$result_prefix = "top." + TestBridge.MODID + ".pipe.result.";
  String top$cm_prefix = "top." + TestBridge.MODID + ".crafting_manager.";

  String gui$cm_prefix = "gui.crafting_manager.";
}
