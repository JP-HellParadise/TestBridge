package net.jp.hellparadise.testbridge.helpers;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import logisticspipes.kotlin.text.Regex;
import net.jp.hellparadise.testbridge.utils.TextUtil;
import net.minecraft.util.text.TextFormatting;

public class TextHelper {

    public final Regex translationKeyRegex = new Regex("([a-z]+\\.)+[a-z]+");
    private String append = "";
    private String prepend = "";
    public final EnumSet<TextFormatting> baseFormatting = EnumSet.noneOf(TextFormatting.class);
    public final List<String> arguments = new ArrayList<>();
    private String key;

    public TextHelper(String key) {
        this.key = key;
    }

    public String getTranslated() {
        return this.key == null ? ""
            : TextUtil.translate(
                this.key,
                this.baseFormatting,
                translateIfApplicable(prepend),
                translateIfApplicable(append),
                arguments.stream()
                    .map(this::translateIfApplicable)
                    .toArray(String[]::new));
    }

    public TextHelper addArgument(String arg) {
        if (arg != null && !arg.isEmpty()) arguments.add(arg);
        return this;
    }

    private String translateIfApplicable(String text) {
        return translationKeyRegex.matches(text) ? TextUtil.translate(text) : text;
    }

    public String getAppend() {
        return append;
    }

    public TextHelper setAppend(String append) {
        this.append = append;
        return this;
    }

    public String getPrepend() {
        return prepend;
    }

    public TextHelper setPrepend(String prepend) {
        this.prepend = prepend;
        return this;
    }

    public String getKey() {
        return key;
    }

    public TextHelper setKey(String key) {
        this.key = key;
        return this;
    }
}
