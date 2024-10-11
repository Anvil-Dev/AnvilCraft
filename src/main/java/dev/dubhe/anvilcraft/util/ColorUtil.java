package dev.dubhe.anvilcraft.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ColorUtil {
    @Contract("_, _, _ -> new")
    public static float @NotNull [] rgbToHsv(int r, int g, int b) {
        float rNorm = r / 255.0f;
        float gNorm = g / 255.0f;
        float bNorm = b / 255.0f;

        float cMax = Math.max(rNorm, Math.max(gNorm, bNorm));
        float cMin = Math.min(rNorm, Math.min(gNorm, bNorm));
        float delta = cMax - cMin;

        float h;
        if (delta == 0) {
            // HSV undefined
            h = 0;
        } else if (cMax == rNorm) {
            h = 60 * (((gNorm - bNorm) / delta) % 6);
        } else if (cMax == gNorm) {
            h = 60 * (((bNorm - rNorm) / delta) + 2);
        } else {
            h = 60 * (((rNorm - gNorm) / delta) + 4);
        }

        float s = (cMax == 0) ? 0 : (delta / cMax);

        return new float[]{h, s * 100, cMax * 100};
    }

    @Contract("_, _, _ -> new")
    public static int @NotNull [] hsvToRgb(float h, float s, float v) {
        float c = v / 100 * s / 100;
        float x = c * (1 - Math.abs(((h / 60) % 2) - 1));
        float m = v / 100 - c;

        float r, g, b;

        if (h >= 0 && h < 60) {
            r = c;
            g = x;
            b = 0;
        } else if (h >= 60 && h < 120) {
            r = x;
            g = c;
            b = 0;
        } else if (h >= 120 && h < 180) {
            r = 0;
            g = c;
            b = x;
        } else if (h >= 180 && h < 240) {
            r = 0;
            g = x;
            b = c;
        } else if (h >= 240 && h < 300) {
            r = x;
            g = 0;
            b = c;
        } else {
            r = c;
            g = 0;
            b = x;
        }

        int red = Math.round((r + m) * 255);
        int green = Math.round((g + m) * 255);
        int blue = Math.round((b + m) * 255);

        return new int[]{red, green, blue};
    }
}
