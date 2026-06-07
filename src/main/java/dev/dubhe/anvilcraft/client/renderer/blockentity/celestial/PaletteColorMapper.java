package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import com.mojang.blaze3d.platform.NativeImage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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

    public static int[] extractRowColors(NativeImage palette, int row) {
        if (row < 0 || row >= palette.getHeight()) return new int[0];
        int count = 0;
        for (int col = 0; col < palette.getWidth(); col++) {
            int c = palette.getPixelRGBA(col, row);
            if ((c & 0xFF) != 0 || ((c >> 8) & 0xFF) != 0 || ((c >> 16) & 0xFF) != 0) count++;
        }
        if (count == 0) return new int[0];
        int[] colors = new int[count];
        int idx = 0;
        for (int col = 0; col < palette.getWidth(); col++) {
            int c = palette.getPixelRGBA(col, row);
            if ((c & 0xFF) != 0 || ((c >> 8) & 0xFF) != 0 || ((c >> 16) & 0xFF) != 0) {
                colors[idx++] = c;
            }
        }
        return colors;
    }

    private static boolean isBlackRow(NativeImage palette, int row) {
        for (int col = 0; col < palette.getWidth(); col++) {
            int c = palette.getPixelRGBA(col, row);
            if ((c & 0xFF) != 0 || ((c >> 8) & 0xFF) != 0 || ((c >> 16) & 0xFF) != 0) return true;
        }
        return false;
    }

    private static int findSplitRow(NativeImage palette) {
        for (int row = 1; row < palette.getHeight() - 1; row++) {
            if (isBlackRow(palette, row)) continue;
            boolean hasAbove = false, hasBelow = false;
            for (int r = 0; r < row; r++) { if (isBlackRow(palette, r)){hasAbove = true; break;} }
            for (int r = row + 1; r < palette.getHeight(); r++) { if (isBlackRow(palette, r)) { hasBelow = true; break; } }
            if (hasAbove && hasBelow) return row;
        }
        return -1;
    }

    public static int[] getPaletteColors(NativeImage palette, int rowIndex, boolean isBase) {
        int splitRow = findSplitRow(palette);
        int start, end;
        if (splitRow > 0) {
            start = isBase ? 0 : splitRow + 1;
            end = isBase ? splitRow : palette.getHeight();
        } else {
            start = 0;
            end = palette.getHeight();
        }
        int validCount = 0;
        for (int r = start; r < end; r++) { if (isBlackRow(palette, r)) validCount++;}
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

    public static NativeImage colorTexture(NativeImage source, NativeImage palette, int paletteRow, boolean isBase) {
        int[] paletteColors = getPaletteColors(palette, paletteRow, isBase);
        if (paletteColors.length == 0) return copyGrayscale(source);

        int[] refGrays = extractReferenceGrays(source);
        if (refGrays.length == 0) return copyGrayscale(source);

        Map<Integer, Integer> grayToIndex = new HashMap<>();
        int mapCount = Math.min(refGrays.length, paletteColors.length);
        for (int i = 0; i < mapCount; i++) grayToIndex.put(refGrays[i], i);

        int w = source.getWidth(), h = source.getHeight();
        NativeImage result = new NativeImage(w, h, true);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int src = source.getPixelRGBA(x, y);
                int alpha = (src >> 24) & 0xFF;
                if (alpha == 0) continue;
                int gray = src & 0xFF;
                int g = (src >> 8) & 0xFF, b = (src >> 16) & 0xFF;
                if (gray == 0 && g == 0 && b == 0) continue;

                Integer idx = grayToIndex.get(gray);
                if (idx == null) idx = findClosestGrayIndex(gray, refGrays);
                idx = Math.clamp(idx, 0, paletteColors.length - 1);

                int pc = paletteColors[idx];
                int pr = pc & 0xFF, pg = (pc >> 8) & 0xFF, pb = (pc >> 16) & 0xFF;
                result.setPixelRGBA(x, y, (alpha << 24) | (pb << 16) | (pg << 8) | pr);
            }
        }
        return result;
    }

    private static NativeImage copyGrayscale(NativeImage source) {
        int w = source.getWidth(), h = source.getHeight();
        NativeImage result = new NativeImage(w, h, false);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                result.setPixelRGBA(x, y, source.getPixelRGBA(x, y));
        return result;
    }

    public static int[] extractReferenceGrays(NativeImage source) {
        Set<Integer> graySet = new LinkedHashSet<>();
        int w = source.getWidth(), h = source.getHeight();
        boolean fullImage = w <= 32 && w == h;
        int bw = fullImage ? w : Math.min(16, w);
        int bh = fullImage ? h : Math.min(16, h);
        int y0 = fullImage ? 0 : h - bh;
        for (int y = y0; y < y0 + bh; y++) {
            for (int x = 0; x < bw; x++) {
                int c = source.getPixelRGBA(x, y);
                if (((c >> 24) & 0xFF) == 0) continue;
                int r = c & 0xFF, g = (c >> 8) & 0xFF, b = (c >> 16) & 0xFF;
                if (r == 0 && g == 0 && b == 0) continue;
                graySet.add(r);
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
                int ov = overlay.getPixelRGBA(x, y);
                if ((ov >> 24 & 0xFF) > 0) base.setPixelRGBA(x, y, ov);
            }
        }
    }
}
