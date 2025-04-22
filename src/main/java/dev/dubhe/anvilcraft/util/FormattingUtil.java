package dev.dubhe.anvilcraft.util;

import com.google.common.base.CaseFormat;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

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
    public static @NotNull String toLowerCaseUnderscore(@NotNull String string) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            if (i != 0
                && (Character.isUpperCase(string.charAt(i))
                || (Character.isDigit(string.charAt(i - 1)) ^ Character.isDigit(string.charAt(i)))))
                result.append("_");
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
    public static @NotNull String toLowerCaseUnder(String string) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, string);
    }

    /**
     * apple_orange.juice => Apple Orange (Juice)
     */
    public static String toEnglishName(@NotNull Object internalName) {
        return Arrays.stream(internalName.toString().toLowerCase(Locale.ROOT).split("_"))
            .map(StringUtils::capitalize)
            .collect(Collectors.joining(" "));
    }

    /**
     * <table>
     *     <thead>
     *         <tr>
     *             <th>tick数</th>
     *             <th>显示效果</th>
     *         </tr>
     *     </thead>
     *     <tbody>
     *         <tr>
     *             <td>30gt</td>
     *             <td>30gt</td>
     *         </tr>
     *         <tr>
     *             <td>100gt</td>
     *             <td>5"</td>
     *         </tr>
     *         <tr>
     *             <td>150gt</td>
     *             <td>7"50</td>
     *         </tr>
     *         <tr>
     *             <td>1200gt</td>
     *             <td>1'</td>
     *         </tr>
     *         <tr>
     *             <td>1220gt</td>
     *             <td>1'01</td>
     *         </tr>
     *         <tr>
     *             <td>1635gt</td>
     *             <td>1'21"75</td>
     *         </tr>
     *     </tbody>
     * </table>
     */
    public static String toFormattedTime(int total) {
        int minutes = total / 20 / 60;
        int seconds = (total / 20) % 60;
        int ticks = total % 60 % 20;

        StringBuilder result = new StringBuilder();

        if (minutes > 0) {
            result.append(minutes).append("'");
        }

        if (!result.isEmpty()) {
            if (seconds != 0 || ticks != 0) {
                result.append(String.format("%02d", seconds));
            }
            if (ticks != 0) {
                result.append('"');
            }
        } else if (seconds >= 5) {
            result.append(String.format("%d", seconds)).append('"');
        }

        if (result.isEmpty()) {
            result.append(String.format("%dgt", ticks + seconds * 20));
        } else if (ticks != 0) {
            result.append(String.format("%02d", ticks * 5));
        }

        return result.toString();
    }
}
