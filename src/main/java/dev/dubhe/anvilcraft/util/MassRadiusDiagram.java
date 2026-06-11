package dev.dubhe.anvilcraft.util;

import com.mojang.blaze3d.platform.NativeImage;
import dev.dubhe.anvilcraft.AnvilCraft;
import lombok.Getter;
import net.minecraft.util.RandomSource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("checkstyle:MethodName")
public final class MassRadiusDiagram {

    private static final String DIAGRAM_PATH =
        "assets/anvilcraft/textures/misc/mass_radius_diagram_pixel.png";

    /**
     * Star region Y boundaries within the 64×64 diagram.
     * Y=2 is the highest-mass row containing stars (top of star region).
     * Y=29 is the lowest-mass row containing stars (bottom of star region).
     * Planets occupy rows 32-43; rows 30-31 are a clean gap.
     */
    private static final int STAR_Y_MIN = 2;
    private static final int STAR_Y_MAX = 29;
    private static final int BACKGROUND_ARGB = 0xFF000000;
    private static final int MIN_STAR_SIZE = 1;
    private static final int MAX_STAR_SIZE = 28;

    private static boolean loadAttempted = false;
    @Getter
    private static boolean loadFailed = false;
    private static final List<StarPixel> STAR_PIXELS = new ArrayList<>();

    public record StarPixel(int y, int r, int g, int b) {}

    private MassRadiusDiagram() {}

    /**
     * Lazy-load the mass-radius diagram from the classpath.
     * Thread-safe as long as called from a single game thread (server tick or client render).
     */
    public static void ensureLoaded() {
        if (loadAttempted) return;
        loadAttempted = true;

        try (InputStream is = AnvilCraft.class.getClassLoader()
            .getResourceAsStream(DIAGRAM_PATH)) {
            if (is == null) {
                loadFailed = true;
                AnvilCraft.LOGGER.warn(
                    "Mass-radius diagram not found at {}, "
                        + "falling back to hardcoded star colors",
                    DIAGRAM_PATH
                );
                return;
            }

            NativeImage image = NativeImage.read(is);
            int w = image.getWidth();

            for (int y = STAR_Y_MIN; y <= STAR_Y_MAX; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = image.getPixelRGBA(x, y);
                    // ABGR format: (alpha<<24)|(blue<<16)|(green<<8)|red
                    if (argb == BACKGROUND_ARGB) continue;
                    int r = argb & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = (argb >> 16) & 0xFF;
                    STAR_PIXELS.add(new StarPixel(y, r, g, b));
                }
            }
            image.close();

            if (STAR_PIXELS.isEmpty()) {
                loadFailed = true;
                AnvilCraft.LOGGER.warn(
                    "No star pixels found in mass-radius diagram, "
                        + "falling back to hardcoded star colors"
                );
            } else {
                AnvilCraft.LOGGER.debug(
                    "Loaded {} star pixels from mass-radius diagram",
                    STAR_PIXELS.size()
                );
            }
        } catch (Exception e) {
            loadFailed = true;
            AnvilCraft.LOGGER.warn(
                "Failed to load mass-radius diagram: {}", e.getMessage()
            );
        }
    }

    /**
     * Pick a random star pixel from the diagram's star region.
     * Caller should check {@link #isLoadFailed()} first.
     */
    public static StarPixel pickRandomStar(RandomSource random) {
        ensureLoaded();
        if (loadFailed || STAR_PIXELS.isEmpty()) {
            // Defensive fallback: white star pixel
            return new StarPixel(STAR_Y_MIN, 255, 255, 255);
        }
        return STAR_PIXELS.get(random.nextInt(STAR_PIXELS.size()));
    }

    /**
     * Map a diagram Y coordinate to a star size value (1..28).
     * Higher mass (smaller Y value) → larger star.
     */

    public static int yToSize(int y) {
        // y=STAR_Y_MIN (high mass) → MAX_STAR_SIZE
        // y=STAR_Y_MAX (low mass) → MIN_STAR_SIZE
        float fraction = (float) (y - STAR_Y_MIN) / (STAR_Y_MAX - STAR_Y_MIN);
        return Math.clamp(
            Math.round(MIN_STAR_SIZE + (1.0f - fraction) * (MAX_STAR_SIZE - MIN_STAR_SIZE)),
            MIN_STAR_SIZE,
            MAX_STAR_SIZE
        );
    }

    /**
     * Fallback: hardcoded gradient matching the old starColor(int size) behavior.
     * Used when the diagram fails to load or for backward-compatibility with old NBT saves.
     * Size 1 → reddish, Size 28 → bluish.
     */
    public static float[] starColorFallback(int size) {
        float t = (size - 1f) / 27f;
        float r;
        float g;
        float b;
        if (t < 1f / 6f) {
            float s = t * 6f;
            r = 1f;
            g = 0.15f + s * 0.50f;
            b = s * 0.05f;
        } else if (t < 2f / 6f) {
            float s = (t - 1f / 6f) * 6f;
            r = 1f;
            g = 0.65f + s * 0.35f;
            b = 0.05f + s * 0.55f;
        } else if (t < 3f / 6f) {
            float s = (t - 2f / 6f) * 6f;
            r = 1f;
            g = 1f;
            b = 0.6f + s * 0.4f;
        } else if (t < 4f / 6f) {
            float s = (t - 3f / 6f) * 6f;
            r = 1f - s * 0.3f;
            g = 1f - s * 0.15f;
            b = 1f;
        } else if (t < 5f / 6f) {
            float s = (t - 4f / 6f) * 6f;
            r = 0.7f - s * 0.25f;
            g = 0.85f - s * 0.35f;
            b = 1f;
        } else {
            float s = (t - 5f / 6f) * 6f;
            r = 0.45f - s * 0.15f;
            g = 0.5f - s * 0.2f;
            b = 1f;
        }
        return new float[]{r, g, b};
    }
}
