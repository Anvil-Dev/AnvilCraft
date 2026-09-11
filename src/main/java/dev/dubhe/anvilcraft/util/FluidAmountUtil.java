package dev.dubhe.anvilcraft.util;

import lombok.experimental.UtilityClass;
import net.neoforged.neoforge.fluids.FluidType;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * 流体数量的显示格式化。
 *
 * <p>少于 1 B 时用 mB 显示，达到 1 B 起用 B 显示；存在小数时保留三位有效数字。
 * 达到 1000 B 起改用与物品数量一致的 K/M 等缩写。</p>
 */
@UtilityClass
public class FluidAmountUtil {
    /** 改用缩写显示的 B 数阈值 */
    private static final long ABBR_THRESHOLD = 1000;

    /**
     * 把 mB 数量格式化为显示文本。
     *
     * @param amountMb 数量（mB）
     * @return 显示文本，例如 {@code 500 mB}、{@code 1.25 B}、{@code 128 B}、{@code 1K B}
     */
    public static String formatAmount(int amountMb) {
        if (amountMb < FluidType.BUCKET_VOLUME) {
            return amountMb + " mB";
        }
        double buckets = (double) amountMb / FluidType.BUCKET_VOLUME;
        if (buckets >= FluidAmountUtil.ABBR_THRESHOLD) {
            // 与物品数量一致：K/M/B/T 等缩写，保留一位小数
            return FormattingUtil.toAbbrNum((long) buckets) + " B";
        }
        if (buckets == Math.floor(buckets)) {
            return (long) buckets + " B";
        }
        // 有小数时保留三位有效数字（数学上下文只做舍入、不补零，故 1.25 仍是 1.25）
        BigDecimal rounded = BigDecimal.valueOf(buckets).round(new MathContext(3));
        return rounded.toPlainString() + " B";
    }

    /**
     * 精确数量文本，用于 tooltip 显示。
     *
     * <p>单位与 {@link #formatAmount(int)} 保持一致：不足 1 B 用 mB，否则用 B 的完整精度。</p>
     *
     * @param amountMb 数量（mB）
     * @return 精确文本，例如 {@code 500 mB}、{@code 1.575 B}、{@code 1536 B}
     */
    public static String formatExactAmount(int amountMb) {
        if (amountMb < FluidType.BUCKET_VOLUME) {
            return amountMb + " mB";
        }
        BigDecimal exact = BigDecimal.valueOf(amountMb, 3).stripTrailingZeros();
        return exact.toPlainString() + " B";
    }
}
