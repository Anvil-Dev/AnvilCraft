package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import com.mojang.blaze3d.platform.NativeImage;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.GiantPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.LiquidCoverage;
import dev.dubhe.anvilcraft.block.entity.celestial.RingType;
import dev.dubhe.anvilcraft.block.entity.celestial.RockyPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.celestial.Temperature;
import dev.dubhe.anvilcraft.block.entity.celestial.WindSpeed;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings(
    {
        "checkstyle:MultipleVariableDeclarations",
        "checkstyle:OneStatementPerLine",
        "checkstyle:NeedBraces",
        "checkstyle:Indentation"
    }
)
public class CelestialBodyTextureBakery {

    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();
    private static final String TEX_DIR = "textures/block/celestial_body";

    @Nullable
    private static NativeImage loadImage(String filename) {
        ResourceLocation loc = AnvilCraft.of(TEX_DIR + "/" + filename);
        try {
            Resource res = Minecraft.getInstance().getResourceManager().getResource(loc).orElse(null);
            if (res == null) return null;
            try (InputStream is = res.open()) {
                return NativeImage.read(is);
            }
        } catch (IOException e) {
            AnvilCraft.LOGGER.warn("Failed to load celestial texture: {}", loc, e);
            return null;
        }
    }

    private static ResourceLocation registerTexture(String key, NativeImage image) {
        ResourceLocation loc = AnvilCraft.of("celestial_body/" + key);
        Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(image));
        return loc;
    }

    @Nullable
    public static ResourceLocation getOrBakeBody(CelestialBodyData data) {
        return CACHE.computeIfAbsent(cacheKey(data), k -> bakeBody(data, k));
    }

    @Nullable
    public static ResourceLocation getOrBakeRing(CelestialBodyData data) {
        if (data.ringType() == RingType.NONE) return null;
        return CACHE.computeIfAbsent(ringCacheKey(data), k -> bakeRing(data, k));
    }

    private record TexSet(String base, @Nullable String overlay, String palette) {}

    private static TexSet resolve(CelestialBodyData data) {
        if (data instanceof RockyPlanetData rp) return resolveRocky(rp);
        if (data instanceof GiantPlanetData gp) return resolveGiant(gp);
        return null;
    }

    private static TexSet resolveRocky(RockyPlanetData rp) {
        String base, overlay = null, palette;
        if (!rp.hasAtmosphere() && rp.liquidCoverage() == LiquidCoverage.NONE) {
            base = "planet_atmosphereless.png";
        } else {
            base = switch (rp.liquidCoverage()) {
                case NONE -> "planet_arid.png";
                case LOW -> "planet_wet.png";
                case MEDIUM -> "planet_boggy.png";
                case HIGH -> "planet_oceanic.png";
            };
            overlay = switch (rp.liquidCoverage()) {
                case NONE -> null;
                case LOW -> "planet_wet_overlay.png";
                case MEDIUM -> "planet_boggy_overlay.png";
                case HIGH -> "planet_oceanic_overlay.png";
            };
        }
        if (!rp.hasAtmosphere()) {
            palette = rp.liquidCoverage() == LiquidCoverage.NONE
                ? (rp.temperature() == Temperature.FREEZING ? "planet_atmosphereless_color.png" : "planet_mix_color_scorched.png")
                : (rp.temperature() == Temperature.FREEZING ? "planet_mix_color_freezing.png" : "planet_mix_color_scorched.png");
        } else {
            palette = rp.liquidCoverage() == LiquidCoverage.NONE
                ? (rp.temperature() == Temperature.SCORCHED ? "planet_mix_color_scorched.png" : "planet_arid_color.png")
                : switch (rp.temperature()) {
                case FREEZING -> "planet_mix_color_freezing.png";
                case SCORCHED -> "planet_mix_color_scorched.png";
                default -> "planet_mix_color.png";
                };
        }
        return new TexSet(base, overlay, palette);
    }

    private static TexSet resolveGiant(GiantPlanetData gp) {
        return new TexSet("planet_giant.png",
            gp.windSpeed() == WindSpeed.VERY_HIGH ? "planet_giant_overlay_0.png" : "planet_giant_overlay_1.png",
            "planet_giant_color.png");
    }

    @Nullable
    private static ResourceLocation bakeBody(CelestialBodyData data, String key) {
        if (data instanceof StarData star) return bakeStar(key, star.size());

        TexSet tex = resolve(data);
        if (tex == null) return null;

        NativeImage baseImg = loadImage(tex.base());
        if (baseImg == null) return null;

        NativeImage paletteImg = loadImage(tex.palette());
        if (paletteImg == null) paletteImg = baseImg;

        int baseRow = data instanceof RockyPlanetData rp ? rp.paletteBaseRow()
            : data instanceof GiantPlanetData gp ? gp.paletteBaseRow() : 0;
        NativeImage coloredBase = PaletteColorMapper.colorTexture(baseImg, paletteImg, baseRow, true);

        if (tex.overlay() != null) {
            NativeImage overlayImg = loadImage(tex.overlay());
            if (overlayImg != null) {
                int overlayRow = data instanceof RockyPlanetData rp ? rp.paletteOverlayRow()
                    : data instanceof GiantPlanetData gp ? gp.paletteOverlayRow() : 0;
                NativeImage coloredOverlay = PaletteColorMapper.colorTexture(overlayImg, paletteImg, overlayRow, false);
                PaletteColorMapper.composite(coloredBase, coloredOverlay);
                coloredOverlay.close();
                overlayImg.close();
            }
        }

        if (paletteImg != baseImg) paletteImg.close();
        baseImg.close();
        return registerTexture(key, coloredBase);
    }

    public static float[] starColor(int size) {
        float t = (size - 1f) / 27f;
        float r, g, b;
        if (t < 1f / 6f) {
            float s = t * 6f;
            r = 1f; g = 0.15f + s * 0.50f; b = s * 0.05f;
        } else if (t < 2f / 6f) {
            float s = (t - 1f / 6f) * 6f;
            r = 1f; g = 0.65f + s * 0.35f; b = 0.05f + s * 0.55f;
        } else if (t < 3f / 6f) {
            float s = (t - 2f / 6f) * 6f;
            r = 1f; g = 1f; b = 0.6f + s * 0.4f;
        } else if (t < 4f / 6f) {
            float s = (t - 3f / 6f) * 6f;
            r = 1f - s * 0.3f; g = 1f - s * 0.15f; b = 1f;
        } else if (t < 5f / 6f) {
            float s = (t - 4f / 6f) * 6f;
            r = 0.7f - s * 0.25f; g = 0.85f - s * 0.35f; b = 1f;
        } else {
            float s = (t - 5f / 6f) * 6f;
            r = 0.45f - s * 0.15f; g = 0.5f - s * 0.2f; b = 1f;
        }
        return new float[]{r, g, b};
    }

    private static NativeImage generateStarPalette(int size, int numColors) {
        NativeImage palette = new NativeImage(numColors, 1, false);
        float[] base = starColor(size);
        for (int col = 0; col < numColors; col++) {
            float brightness = 1f - (float) col / (numColors - 1) * 0.15f;
            int ir = Math.clamp((int) (base[0] * brightness * 255), 0, 255);
            int ig = Math.clamp((int) (base[1] * brightness * 255), 0, 255);
            int ib = Math.clamp((int) (base[2] * brightness * 255), 0, 255);
            palette.setPixelRGBA(col, 0, (255 << 24) | (ib << 16) | (ig << 8) | ir);
        }
        return palette;
    }

    @SuppressWarnings("checkstyle:NeedBraces")
    @Nullable
    private static ResourceLocation bakeStar(String key, int size) {
        NativeImage starImg = loadImage("star.png");
        if (starImg == null) return null;

        int frameSize = starImg.getWidth();
        NativeImage frame = new NativeImage(frameSize, frameSize, false);
        for (int y = 0; y < frameSize; y++)
            for (int x = 0; x < frameSize; x++)
                frame.setPixelRGBA(x, y, starImg.getPixelRGBA(x, y));
        starImg.close();

        int[] refGrays = PaletteColorMapper.extractReferenceGrays(frame);
        NativeImage palette = generateStarPalette(size, refGrays.length);
        NativeImage colored = PaletteColorMapper.colorTexture(frame, palette, 0, true);
        frame.close();
        palette.close();

        return registerTexture(key, colored);
    }

    private static NativeImage cropTopLeftSquare(NativeImage image) {
        int halfW = image.getWidth() / 2, halfH = image.getHeight() / 2;
        NativeImage cropped = new NativeImage(halfW, halfH, false);
        for (int y = 0; y < halfH; y++)
            for (int x = 0; x < halfW; x++)
                cropped.setPixelRGBA(x, y, image.getPixelRGBA(x, y));
        return cropped;
    }

    @Nullable
    private static ResourceLocation bakeRing(CelestialBodyData data, String key) {
        String ringFile = switch (data.ringType()) {
            case WEAK -> "planet_giant_ring_0.png";
            case STRONG -> "planet_giant_ring_1.png";
            default -> null;
        };
        if (ringFile == null) return null;

        NativeImage ringImg = loadImage(ringFile);
        if (ringImg == null) return null;

        NativeImage cropped = cropTopLeftSquare(ringImg);
        ringImg.close();

        NativeImage paletteImg = loadImage("planet_giant_ring_color.png");
        if (paletteImg != null) {
            int ringPaletteRow = data instanceof RockyPlanetData rp ? rp.paletteBaseRow()
                : data instanceof GiantPlanetData gp ? gp.paletteBaseRow() : 0;
            NativeImage colored = PaletteColorMapper.colorTexture(cropped, paletteImg, ringPaletteRow, true);
            paletteImg.close();
            cropped.close();
            return registerTexture(key, colored);
        }

        cropped.close();
        return null;
    }

    private static String cacheKey(CelestialBodyData data) {
        if (data instanceof StarData s) return "star_" + s.size();
        if (data instanceof RockyPlanetData rp)
            return "rp_" + rp.hasAtmosphere() + "_" + rp.liquidCoverage().getSerializedName()
                + "_" + rp.temperature().getSerializedName() + "_" + rp.size()
                + "_" + rp.paletteBaseRow() + "_" + rp.paletteOverlayRow();
        if (data instanceof GiantPlanetData gp)
            return "gp_" + gp.pressureType().getSerializedName() + "_" + gp.windSpeed().getSerializedName()
                + "_" + gp.size() + "_" + gp.paletteBaseRow() + "_" + gp.paletteOverlayRow();
        return "unknown";
    }

    private static String ringCacheKey(CelestialBodyData data) {
        int row = data instanceof GiantPlanetData gp ? gp.paletteBaseRow()
            : data instanceof RockyPlanetData rp ? rp.paletteBaseRow() : 0;
        return "ring_" + data.ringType().getSerializedName() + "_" + row;
    }
}
