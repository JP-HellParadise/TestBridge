package testbridge.core;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;
import java.util.Collection;
import java.util.EnumSet;

public class TBConfig extends Configuration {

  private static TBConfig instance;
  private final EnumSet<TBFeature> loggingFlags = EnumSet.noneOf(TBFeature.class);
  private final File configFile;

  private TBConfig(final File configFile) {
    super(configFile);
    this.configFile = configFile;

    MinecraftForge.EVENT_BUS.register(this);

    this.addCustomCategoryComment("logging", "Warning: Disable Logging will disable other features depending on it.");
    for (final TBFeature feature : TBFeature.values()) {
      if (feature.isVisible()) {
        final Property option = this.get("Logging", feature.key(), feature.isEnabled(), feature.comment());

        if (option.getBoolean(feature.isEnabled())) {
          this.loggingFlags.add(feature);
        }
      }
    }
  }

  public static void init(final File configFile) {
    instance = new TBConfig(configFile);
  }

  public static TBConfig instance() {
    return instance;
  }

  private String getListComment(final Enum value) {
    String comment = null;

    if (value != null) {
      final EnumSet set = EnumSet.allOf(value.getClass());

      for (final Object Oeg : set) {
        final Enum eg = (Enum) Oeg;
        if (comment == null) {
          comment = "Possible Values: " + eg.name();
        } else {
          comment += ", " + eg.name();
        }
      }
    }

    return comment;
  }

  public boolean isFeatureEnabled(final TBFeature f) {
    return this.loggingFlags.contains(f);
  }

  public boolean areFeaturesEnabled(Collection<TBFeature> features) {
    return this.loggingFlags.containsAll(features);
  }

  @Override
  public void save() {
    if (this.hasChanged()) {
      super.save();
    }
  }

  public String getFilePath() {
    return this.configFile.toString();
  }

  public enum TBFeature {
    LOGGING("Logging"),
    INTEGRATION_LOGGING("IntegrationLogging", false);

    final String key;
    final boolean enabled;
    final String comment;

    TBFeature(final String key) {
      this(key, true);
    }

    TBFeature(final String key, final String comment) {
      this(key, true, comment);
    }

    TBFeature(final String key, final boolean enabled) {
      this(key, enabled, null);
    }

    TBFeature(final String key, final boolean enabled, final String comment) {
      this.key = key;
      this.enabled = enabled;
      this.comment = comment;
    }

    /**
     * override to set visibility
     *
     * @return default true
     */
    public boolean isVisible() {
      return true;
    }

    public String key() {
      return this.key;
    }

    public boolean isEnabled() {
      return this.enabled;
    }

    public String comment() {
      return this.comment;
    }
  }
}
