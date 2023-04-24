package testbridge.helpers.interfaces;

import logisticspipes.LPConstants;

import io.github.korewali.Tags;

public interface ITranslationKey {
  String top$sat_prefix = "top." + LPConstants.LP_MOD_ID + ".pipe.satellite.";
  String top$result_prefix = "top." + Tags.MODID + ".pipe.result.";
  String top$cm_prefix = "top." + Tags.MODID + ".crafting_manager.";

  String gui$cm_prefix = "gui.crafting_manager.";
  String gui$satselect_prefix = "gui.popup.selectsatellite.";
}
