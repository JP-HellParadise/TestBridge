package testbridge.utils;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicReference;

import logisticspipes.kotlin.text.MatchResult;
import logisticspipes.kotlin.text.Regex;
import logisticspipes.kotlin.text.StringsKt;

import net.minecraft.util.text.translation.I18n;
import net.minecraft.util.text.TextFormatting;

public final class TextUtil {
  private static final EnumSet<TextFormatting> formattingState = EnumSet.noneOf(TextFormatting.class);
  private static final EnumSet<TextFormatting> baseFormattingState = EnumSet.noneOf(TextFormatting.class);
  private static final Regex regexPattern = new Regex("(\\$)(" +
      String.join("|", Arrays.stream(TextFormatting.values()).map(TextFormatting::getFriendlyName).map(String::toUpperCase).toArray(String[]::new)) +
      ")" );

  public static String translate(String key, String... args) {
    return translate(key, EnumSet.noneOf(TextFormatting.class), "", "", args );
  }

  @SuppressWarnings("deprecation")
  public static String translate(String key, EnumSet<TextFormatting> baseFormatting, String prepend, String append, Object[] args) {
    return transform(prepend + I18n.translateToLocalFormatted(key, args) + append, baseFormatting);
  }

  public static String transform(String text, EnumSet<TextFormatting> baseFormatting) {
    baseFormattingState.clear();
    baseFormattingState.addAll(baseFormatting);
    formattingState.clear();
    String result = StringsKt.prependIndent(text, getColorTag(baseFormattingState) + getFormattingTags(baseFormattingState));
    while (regexPattern.containsMatchIn(result)) {
      result = regexPattern.replace(result, matchResult -> getReplacementString(getTextFormatting(matchResult)));
    }
    return result;
  }

  private static String getReplacementString(TextFormatting formatting) {
    if (formatting == TextFormatting.RESET) {
      formattingState.clear();
      return formatting + getColorTag(baseFormattingState) + getFormattingTags(baseFormattingState);
    }
    if (formatting != null ) {
      if (formatting.isColor()) {
        formattingState.removeIf(TextFormatting::isColor);
      }
      formattingState.add(formatting);
    }
    return getColorTag(formattingState) + getFormattingTags(formattingState);
  }

  private static TextFormatting getTextFormatting(MatchResult matchResult) {
    return TextFormatting.getValueByName(matchResult.getValue().toLowerCase());
  }

  private static String getColorTag(EnumSet<TextFormatting> baseFormatting) {
    AtomicReference<String> result = new AtomicReference<>("");
    baseFormatting.stream().filter(TextFormatting::isColor).findFirst().ifPresent(it -> result.set(it.toString()));
    if (result.get().isEmpty())
      baseFormattingState.stream().filter(TextFormatting::isColor).findFirst().ifPresent(it -> result.set(it.toString()));
    return result.get();
  }

  private static String getFormattingTags(EnumSet<TextFormatting> baseFormatting) {
    EnumSet<TextFormatting> enumSet = EnumSet.copyOf(baseFormatting);
    enumSet.addAll(baseFormattingState);
    return String.join("",
        enumSet.stream().filter(TextFormatting::isFancyStyling).map(TextFormatting::toString).toArray(String[]::new));
  }
}
