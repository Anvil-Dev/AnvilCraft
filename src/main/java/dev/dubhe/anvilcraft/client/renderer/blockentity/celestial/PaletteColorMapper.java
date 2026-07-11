package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import com.mojang.blaze3d.platform.NativeImage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Palette-based texture color mapper for celestial body rendering.
 * Adapted for 26.1 NativeImage API (ARGB pixel format via getPixel/setPixel).
 */
public class PaletteColorMapper {

    /**
     * Extract all non-black colors from a palette image row.
     * 26.1: getPixel returns ARGB (0xAARRGGBB).
     */
    public static int[] extractRowColors(NativeImage palette, int row) {
        if (row < 0 || row >= palette.getHeight()) return new int[0];
        int count = 0;
        for (int col = 0; col < palette.getWidth(); col++) {
            int c = palette.getPixel(col, row); // ARGB
            if ((c & 0xFF) != 0 || ((c >> 8) & 0xFF) != 0 || ((c >> 16) & 0xFF) != 0) count++;
        }
        if (count == 0) return new int[0];
        int[] colors = new int[count];
        int idx = 0;
        for (int col = 0; col < palette.getWidth(); col++) {
            int c = palette.getPixel(col, row); // ARGB
            if ((c & 0xFF) != 0 || ((c >> 8) & 0xFF) != 0 || ((c >> 16) & 0xFF) != 0) {
                colors[idx++] = c;
            }
        }
        return colors;
    }

    private static boolean isBlackRow(NativeImage palette, int row) {
        for (int col = 0; col < palette.getWidth(); col++) {
            int c = palette.getPixel(col, row); // ARGB
            if ((c & 0xFF) != 0 || ((c >> 8) & 0xFF) != 0 || ((c >> 16) & 0xFF) != 0) return true;
        }
        return false;
    }

    private static int findSplitRow(NativeImage palette) {
        for (int row = 1; row < palette.getHeight() - 1; row++) {
            if (isBlackRow(palette, row)) continue;
            boolean hasAbove = false;
            boolean hasBelow = false;
            for (int r = 0; r < row; r++) {
                if (isBlackRow(palette, r)) {
                    hasAbove = true;
                    break;
                }
            }
            for (int r = row + 1; r < palette.getHeight(); r++) {
                if (isBlackRow(palette, r)) {
                    hasBelow = true;
                    break;
                }
            }
            if (hasAbove && hasBelow) return row;
        }
        return -1;
    }

    public static int[] getPaletteColors(NativeImage palette, int rowIndex, boolean isBase) {
        int splitRow = findSplitRow(palette);
        int start;
        int end;
        if (splitRow > 0) {
            start = isBase ? 0 : splitRow + 1;
            end = isBase ? splitRow : palette.getHeight();
        } else {
            start = 0;
            end = palette.getHeight();
        }
        int validCount = 0;
        for (int r = start; r < end; r++) {
            if (isBlackRow(palette, r)) validCount++;
        }
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
     * Color a grayscale source texture using a palette image.
     * 26.1 adaptation: getPixel returns ARGB (AARRGGBB), setPixel takes ARGB.
     */
    public static NativeImage colorTexture(NativeImage source, NativeImage palette, int paletteRow, boolean isBase) {
        int[] paletteColors = getPaletteColors(palette, paletteRow, isBase);
        if (paletteColors.length == 0) return copyGrayscale(source);

        int[] refGrays = extractReferenceGrays(source);
        if (refGrays.length == 0) return copyGrayscale(source);

        Map<Integer, Integer> grayToIndex = new HashMap<>();
        int mapCount = Math.min(refGrays.length, paletteColors.length);
        for (int i = 0; i < mapCount; i++) grayToIndex.put(refGrays[i], i);

        int w = source.getWidth();
        int h = source.getHeight();
        NativeImage result = new NativeImage(w, h, false); // RGBA, zero-init
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int src = source.getPixel(x, y); // ARGB
                int alpha = (src >> 24) & 0xFF;
                if (alpha == 0) continue;
                // 26.1 ARGB: B=bits0-7, G=bits8-15, R=bits16-23
                int blue = src & 0xFF;
                int green = (src >> 8) & 0xFF;
                int red = (src >> 16) & 0xFF;
                if (red == 0 && green == 0 && blue == 0) continue;

                // Use red channel as grayscale reference value
                int gray = red;
                Integer idx = grayToIndex.get(gray);
                if (idx == null) idx = findClosestGrayIndex(gray, refGrays);
                idx = Math.clamp(idx, 0, paletteColors.length - 1);

                int pc = paletteColors[idx]; // ARGB
                int pr = (pc >> 16) & 0xFF;
                int pg = (pc >> 8) & 0xFF;
                int pb = pc & 0xFF;
                // Store as ARGB: A|R|G|B
                result.setPixel(x, y, (alpha << 24) | (pr << 16) | (pg << 8) | pb);
            }
        }
        return result;
    }

    private static NativeImage copyGrayscale(NativeImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        NativeImage result = new NativeImage(w, h, false); // RGBA
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                result.setPixel(x, y, source.getPixel(x, y));
            }
        }
        return result;
    }

    /**
     * Extract reference grayscale values from source texture.
     * 26.1: getPixel returns ARGB — red channel is at bits 16-23.
     */
    public static int[] extractReferenceGrays(NativeImage source) {
        Set<Integer> graySet = new LinkedHashSet<>();
        int w = source.getWidth();
        int h = source.getHeight();
        boolean fullImage = w <= 32 && w == h;
        int bw = fullImage ? w : Math.min(16, w);
        int bh = fullImage ? h : Math.min(16, h);
        int y0 = fullImage ? 0 : h - bh;
        for (int y = y0; y < y0 + bh; y++) {
            for (int x = 0; x < bw; x++) {
                int c = source.getPixel(x, y); // ARGB
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
        int bestIdx = 0;
        int bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < refGrays.length; i++) {
            int dist = Math.abs(gray - refGrays[i]);
            if (dist < bestDist) {
                bestDist = dist;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    public static void composite(NativeImage base, NativeImage overlay) {
        int w = base.getWidth();
        int h = base.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int ov = overlay.getPixel(x, y); // ARGB
                if ((ov >> 24 & 0xFF) > 0) base.setPixel(x, y, ov);
            }
        }
    }
}
