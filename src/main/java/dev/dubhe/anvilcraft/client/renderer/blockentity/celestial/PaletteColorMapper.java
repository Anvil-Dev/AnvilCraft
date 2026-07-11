package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import com.mojang.blaze3d.platform.NativeImage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 天体渲染使用的色板贴图映射器，适配 26.1 中采用 ARGB 像素格式的原生图像接口。
 */
@SuppressWarnings(
    {
        "checkstyle:MultipleVariableDeclarations",
        "checkstyle:WhitespaceAfter",
        "checkstyle:LeftCurly",
        "checkstyle:WhitespaceAround",
        "checkstyle:OneStatementPerLine",
        "checkstyle:RightCurly",
        "checkstyle:Indentation",
        "checkstyle:NeedBraces"
    }
)
public class PaletteColorMapper {

    /**
     * 提取色板指定行中的全部非黑色颜色，像素格式为 ARGB。
     */
    public static int[] extractRowColors(NativeImage palette, int row) {
        if (row < 0 || row >= palette.getHeight()) return new int[0];
        int count = 0;
        for (int col = 0; col < palette.getWidth(); col++) {
            int c = palette.getPixel(col, row); // 按 ARGB 格式读取像素。
            if ((c & 0xFF) != 0 || ((c >> 8) & 0xFF) != 0 || ((c >> 16) & 0xFF) != 0) count++;
        }
        if (count == 0) return new int[0];
        int[] colors = new int[count];
        int idx = 0;
        for (int col = 0; col < palette.getWidth(); col++) {
            int c = palette.getPixel(col, row); // 按 ARGB 格式读取像素。
            if ((c & 0xFF) != 0 || ((c >> 8) & 0xFF) != 0 || ((c >> 16) & 0xFF) != 0) {
                colors[idx++] = c;
            }
        }
        return colors;
    }

    private static boolean isBlackRow(NativeImage palette, int row) {
        for (int col = 0; col < palette.getWidth(); col++) {
            int c = palette.getPixel(col, row); // 按 ARGB 格式读取像素。
            if ((c & 0xFF) != 0 || ((c >> 8) & 0xFF) != 0 || ((c >> 16) & 0xFF) != 0) return true;
        }
        return false;
    }

    private static int findSplitRow(NativeImage palette) {
        for (int row = 1; row < palette.getHeight() - 1; row++) {
            if (isBlackRow(palette, row)) continue;
            boolean hasAbove = false, hasBelow = false;
            for (int r = 0; r < row; r++) { if (isBlackRow(palette, r)) { hasAbove = true; break; } }
            for (int r = row + 1; r < palette.getHeight(); r++) { if (isBlackRow(palette, r)) { hasBelow = true; break; } }
            if (hasAbove && hasBelow) return row;
        }
        return -1;
    }

    public static int[] getPaletteColors(NativeImage palette, int rowIndex, boolean isBase) {
        int splitRow = findSplitRow(palette);
        int start, end;
        // NativeImage 与色板 PNG 均以顶部为第 0 行；保持与 1.21 相同的自上而下行号，不进行 Y 翻转。
        if (splitRow > 0) {
            start = isBase ? 0 : splitRow + 1;
            end = isBase ? splitRow : palette.getHeight();
        } else {
            start = 0;
            end = palette.getHeight();
        }
        int validCount = 0;
        for (int r = start; r < end; r++) { if (isBlackRow(palette, r)) validCount++; }
        if (validCount == 0) return new int[0];
        int targetRow = rowIndex % validCount;
        int found = 0;
        for (int r = start; r < end; r++) {
            if (isBlackRow(palette, r)) {
                if (found == targetRow) return extractRowColors(palette, r);
                found++;
            }
        }
        return new int[0];
    }

    /**
     * 使用色板为灰度源贴图着色，读取和写入均采用 ARGB 像素格式。
     */
    public static NativeImage colorTexture(NativeImage source, NativeImage palette, int paletteRow, boolean isBase) {
        int[] paletteColors = getPaletteColors(palette, paletteRow, isBase);
        if (paletteColors.length == 0) return copyGrayscale(source);

        int[] refGrays = extractReferenceGrays(source);
        if (refGrays.length == 0) return copyGrayscale(source);

        Map<Integer, Integer> grayToIndex = new HashMap<>();
        int mapCount = Math.min(refGrays.length, paletteColors.length);
        for (int i = 0; i < mapCount; i++) grayToIndex.put(refGrays[i], i);

        int w = source.getWidth(), h = source.getHeight();
        // 透明像素会在下方循环中跳过，因此必须清零底层内存，避免残留像素污染动态贴图。
        NativeImage result = new NativeImage(w, h, true);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int src = source.getPixel(x, y); // 按 ARGB 格式读取像素。
                int alpha = (src >> 24) & 0xFF;
                if (alpha == 0) continue;
                // 26.1 的 ARGB 格式中，蓝、绿、红分别位于第 0-7、8-15、16-23 位。
                int blue = src & 0xFF;
                int green = (src >> 8) & 0xFF;
                int red = (src >> 16) & 0xFF;
                if (red == 0 && green == 0 && blue == 0) continue;

                // 使用红色通道作为灰度参考值。
                int gray = red;
                Integer idx = grayToIndex.get(gray);
                if (idx == null) idx = findClosestGrayIndex(gray, refGrays);
                idx = Math.clamp(idx, 0, paletteColors.length - 1);

                int pc = paletteColors[idx]; // 色板像素同样采用 ARGB 格式。
                int pr = (pc >> 16) & 0xFF;
                int pg = (pc >> 8) & 0xFF;
                int pb = pc & 0xFF;
                // 按透明度、红、绿、蓝的顺序写回 ARGB 像素。
                result.setPixel(x, y, (alpha << 24) | (pr << 16) | (pg << 8) | pb);
            }
        }
        return result;
    }

    private static NativeImage copyGrayscale(NativeImage source) {
        int w = source.getWidth(), h = source.getHeight();
        NativeImage result = new NativeImage(w, h, false); // 创建目标图像。
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                result.setPixel(x, y, source.getPixel(x, y));
        return result;
    }

    /**
     * 从源贴图提取参考灰度值；ARGB 像素的红色通道位于第 16-23 位。
     */
    public static int[] extractReferenceGrays(NativeImage source) {
        Set<Integer> graySet = new LinkedHashSet<>();
        int w = source.getWidth(), h = source.getHeight();
        boolean fullImage = w <= 32 && w == h;
        int bw = fullImage ? w : Math.min(16, w);
        int bh = fullImage ? h : Math.min(16, h);
        int y0 = fullImage ? 0 : h - bh;
        for (int y = y0; y < y0 + bh; y++) {
            for (int x = 0; x < bw; x++) {
                int c = source.getPixel(x, y); // 按 ARGB 格式读取像素。
                if (((c >> 24) & 0xFF) == 0) continue;
                int red = (c >> 16) & 0xFF;
                int green = (c >> 8) & 0xFF;
                int blue = c & 0xFF;
                if (red == 0 && green == 0 && blue == 0) continue;
                graySet.add(red);
            }
        }
        int[] grays = new int[graySet.size()];
        int i = 0;
        for (int gv : graySet) grays[i++] = gv;
        Arrays.sort(grays);
        for (int j = 0; j < grays.length / 2; j++) {
            int tmp = grays[j];
            grays[j] = grays[grays.length - 1 - j];
            grays[grays.length - 1 - j] = tmp;
        }
        return grays;
    }

    private static int findClosestGrayIndex(int gray, int[] refGrays) {
        if (refGrays.length == 0) return 0;
        int bestIdx = 0, bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < refGrays.length; i++) {
            int dist = Math.abs(gray - refGrays[i]);
            if (dist < bestDist) { bestDist = dist; bestIdx = i; }
        }
        return bestIdx;
    }

    public static void composite(NativeImage base, NativeImage overlay) {
        int w = base.getWidth(), h = base.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int ov = overlay.getPixel(x, y); // 按 ARGB 格式读取叠加层像素。
                if ((ov >> 24 & 0xFF) > 0) base.setPixel(x, y, ov);
            }
        }
    }
}
