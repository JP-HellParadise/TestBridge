package testbridge.helpers;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import net.minecraft.util.text.TextFormatting;

import logisticspipes.kotlin.text.Regex;

import testbridge.utils.TextUtil;

public class TBText {

  private final Regex translationKeyRegex = new Regex("([a-z]+\\.)+[a-z]+");
  @Getter
  @Setter
  private String append = "";
  @Getter
  @Setter
  private String prepend = "";
  @Getter
  private final EnumSet<TextFormatting> baseFormatting = EnumSet.noneOf(TextFormatting.class);
  @Getter
  private final List<String> arguments = new ArrayList<>();
  @Getter
  @Setter
  private String key;

  public String getTranslated() {
    return this.key == null ? "" : TextUtil.translate(
        this.key,
        this.baseFormatting,
        translateIfApplicable(prepend),
        translateIfApplicable(append),
        arguments.stream().map(this::translateIfApplicable).toArray(String[]::new)
        );
  }

  public TBText addArgument(String arg) {
    if (arg != null && !arg.isEmpty()) arguments.add(arg);
    return this;
  }

  private String translateIfApplicable(String text) {
    return translationKeyRegex.matches(text) ? TextUtil.translate(text) : text;
  }

  public TBText(String key) {
    this.key = key;
  }
}
