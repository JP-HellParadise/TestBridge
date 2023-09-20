package net.jp.hellparadise.testbridge.utils;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;

public final class TextUtil {

    private static final EnumSet<TextFormatting> formattingState = EnumSet.noneOf(TextFormatting.class);
    private static final EnumSet<TextFormatting> baseFormattingState = EnumSet.noneOf(TextFormatting.class);
    private static final Pattern regexPattern = Pattern.compile("(\\$)(" + String.join(
            "|",
            Arrays.stream(TextFormatting.values())
                    .map(TextFormatting::getFriendlyName)
                    .map(String::toUpperCase)
                    .toArray(String[]::new))
            + ")");

    public static String translate(String key, String... args) {
        return translate(key, EnumSet.noneOf(TextFormatting.class), "", "", args);
    }

    @SuppressWarnings("deprecation")
    public static String translate(String key, EnumSet<TextFormatting> baseFormatting, String prepend, String append,
        Object[] args) {
        return transform(prepend + I18n.translateToLocalFormatted(key, args) + append, baseFormatting);
    }

    public static String transform(String text, EnumSet<TextFormatting> baseFormatting) {
        baseFormattingState.clear();
        baseFormattingState.addAll(baseFormatting);
        formattingState.clear();
        String result = TextUtil.prependIndent(text, getColorTag(baseFormattingState) + getFormattingTags(baseFormattingState));
        while (regexPattern.matcher(result).find()) {
            result = TextUtil.replace(result, matchResult -> getReplacementString(getTextFormatting(matchResult)));
        }
        return result;
    }

    private static String getReplacementString(TextFormatting formatting) {
        if (formatting == TextFormatting.RESET) {
            formattingState.clear();
            return formatting + getColorTag(baseFormattingState) + getFormattingTags(baseFormattingState);
        }
        if (formatting != null) {
            if (formatting.isColor()) {
                formattingState.removeIf(TextFormatting::isColor);
            }
            formattingState.add(formatting);
        }
        return getColorTag(formattingState) + getFormattingTags(formattingState);
    }

    private static TextFormatting getTextFormatting(String matchResult) {
        return TextFormatting.getValueByName(matchResult);
    }

    private static String getColorTag(EnumSet<TextFormatting> baseFormatting) {
        final String[] result = new String[]{""};
        baseFormatting.stream()
            .filter(TextFormatting::isColor)
            .findFirst()
            .ifPresent(it -> result[0] = it.toString());
        if (result[0].isEmpty())
            baseFormattingState.stream()
                .filter(TextFormatting::isColor)
                .findFirst()
                .ifPresent(it -> result[0] = it.toString());
        return result[0];
    }

    private static String getFormattingTags(EnumSet<TextFormatting> baseFormatting) {
        EnumSet<TextFormatting> enumSet = EnumSet.copyOf(baseFormatting);
        enumSet.addAll(baseFormattingState);
        return String.join(
            "",
            enumSet.stream()
                .filter(TextFormatting::isFancyStyling)
                .map(TextFormatting::toString)
                .toArray(String[]::new));
    }

    private static String replace(@Nonnull String input, @Nonnull Function<String, String> transform) {
        Matcher match = regexPattern.matcher(input);
        int lastStart = 0;
        int length = input.length();
        StringBuilder sb = new StringBuilder();

        while (lastStart < length && match.find()) {
            sb.append(input, lastStart, match.start());
            sb.append(transform.apply(match.group()));
            lastStart = match.end();
        }

        if (lastStart < length) {
            sb.append(input, lastStart, length);
        }

        return sb.toString();
    }

    private static String prependIndent(@Nonnull String prependIndent, @Nonnull final String indent) {
        StringBuilder result = new StringBuilder();
        String[] lines = prependIndent.split("\n");

        for (int i = 0 ; i < lines.length ; i++) {
            result.append(indent).append(lines[i]);
            if (i>0) result.append("\n");
        }

        return result.toString();
    }
}
