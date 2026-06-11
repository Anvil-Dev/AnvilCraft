package dev.dubhe.anvilcraft.util;

import com.google.common.base.CaseFormat;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 格式化工具
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FormattingUtil {
    /**
     * 与 .to(LOWER_UNDERSCORE, string) 几乎相同，但它也会在单词和数字之间插入下划线。
     *
     * @param string 任何带有 ASCII 字符的字符串。
     * @return 全小写的字符串，在单词/数字边界前插入下划线：“maragingSteel300” -> “maraging_steel_300”
     */
    public static String toLowerCaseUnderscore(String string) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            if (
                i != 0
                && (Character.isUpperCase(string.charAt(i))
                || (Character.isDigit(string.charAt(i - 1)) ^ Character.isDigit(string.charAt(i))))
            ) {
                result.append("_");
            }
            result.append(Character.toLowerCase(string.charAt(i)));
        }
        return result.toString();
    }

    /**
     * 与 .to(LOWER_UNDERSCORE, string) 几乎相同，但它也会在单词和数字之间插入下划线。
     *
     * @param string 任何带有 ASCII 字符的字符串。
     * @return 全小写的字符串，在单词/数字边界前插入下划线：“maragingSteel300” -> “maraging_steel_300”
     */
    public static String toLowerCaseUnder(String string) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, string);
    }

    /**
     * apple_orange.juice => Apple Orange (Juice)
     */
    public static String toEnglishName(Object internalName) {
        return Arrays.stream(internalName.toString().toLowerCase(Locale.ROOT).split("_"))
            .map(StringUtils::capitalize)
            .collect(Collectors.joining(" "));
    }

    /**
     * 格式化时间。
     *
     * @param total 总tick数
     * @return 格式化后的时间字符串，格式为 Xm Xs
     */
    public static String toFormattedTime(int total) {
        int minutes = total / 20 / 60;
        int seconds = (total / 20) % 60;
        return minutes + "m " + seconds + "s";
    }

    /**
     * 格式化时间，保留阈值参数的重载。
     *
     * @deprecated 保留阈值参数的重载，内部始终输出 Xm Xs 格式
     */
    @Deprecated
    public static String toFormattedTime(int total, int thresholdInSec) {
        return toFormattedTime(total);
    }

    /**
     * 根据进度生成一个给定长度的进度条。
     *
     * @param progress 进度，0-1
     * @param length   进度条长度
     * @param format   文本格式
     * @return 进度条文本
     */
    public static Component toShadeProgress(double progress, int length, ChatFormatting... format) {
        double eachShade = 1.0 / length;
        double alreadyUsed = 0;
        StringBuilder progressStr = new StringBuilder();
        for (int i = 0; i < length; i++) {
            double partProgress = Math.min(progress - alreadyUsed, eachShade);
            double partProgressPercent = partProgress / eachShade;
            char part = '░';
            if (partProgressPercent > 0.99) {
                part = '█';
            } else if (partProgressPercent > 0.66) {
                part = '▓';
            } else if (partProgressPercent > 0.33) {
                part = '▒';
            }
            progressStr.append(part);
            alreadyUsed += partProgress;
        }
        return Component.literal(progressStr.toString()).withStyle(format);
    }
}
