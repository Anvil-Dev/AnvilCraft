package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import com.mojang.blaze3d.platform.NativeImage;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.GiantPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.LiquidCoverage;
import dev.dubhe.anvilcraft.block.entity.celestial.RingType;
import dev.dubhe.anvilcraft.block.entity.celestial.RockyPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
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
        if (data instanceof GiantPlanetData gp) {
            if (gp.brownDwarf()) return new TexSet("planet_giant.png",
                gp.windSpeed() == WindSpeed.VERY_HIGH ? "planet_giant_overlay_1.png" : "planet_giant_overlay_0.png",
                "planet_mix_color_scorched.png");
            return resolveGiant(gp);
        }
        return null;
    }

    @SuppressWarnings("Linelength")
    /*
      Resolve texture set for a rocky planet.

      <table>
      <caption>Texture mapping by temperature, liquid coverage, and atmosphere</caption>
      <tr><th>Temperature</th><th>Liquid</th><th>Atmosphere</th><th>Base</th><th>Palette</th><th>Class</th></tr>
      <tr><td>FREEZING</td><td>NONE</td><td>no</td><td>planet_atmosphereless</td><td>planet_mix_color_freezing</td><td>Deathly Frozen</td></tr>
      <tr><td>FREEZING</td><td>NONE</td><td>yes</td><td>planet_arid</td><td>planet_mix_color_freezing</td><td>Desolate Frozen</td></tr>
      <tr><td>FREEZING</td><td>has</td><td>any</td><td>wet/boggy/oceanic</td><td>planet_mix_color_freezing</td><td>Frozen Planet</td></tr>
      <tr><td>SCORCHED</td><td>NONE</td><td>no</td><td>planet_atmosphereless</td><td>planet_mix_color_scorched</td><td>Deathly Scorched</td></tr>
      <tr><td>SCORCHED</td><td>NONE</td><td>yes</td><td>planet_arid</td><td>planet_mix_color_scorched</td><td>Desolate Scorched</td></tr>
      <tr><td>SCORCHED</td><td>has</td><td>any</td><td>wet/boggy/oceanic</td><td>planet_mix_color_scorched</td><td>Lava Planet</td></tr>
      <tr><td>COLD/MILD/HOT</td><td>—</td><td>no</td><td>planet_atmosphereless</td><td>planet_atmosphereless_color</td><td>Deathly Planet</td></tr>
      <tr><td>COLD/MILD/HOT</td><td>NONE</td><td>yes</td><td>planet_desert</td><td>planet_arid_color</td><td>Desert Planet</td></tr>
      <tr><td>COLD/MILD/HOT</td><td>has</td><td>yes</td><td>wet/boggy/oceanic</td><td>planet_mix_color</td><td>9 types</td></tr>
      </table>
     */
    private static TexSet resolveRocky(RockyPlanetData rp) {
        String base, overlay = null, palette;
        boolean hasAtmos = rp.hasAtmosphere();
        boolean hasLiquid = rp.liquidCoverage() != LiquidCoverage.NONE;
        Temperature temp = rp.temperature();

        if (hasLiquid && (temp == Temperature.FREEZING || temp == Temperature.SCORCHED)) {
            // Extreme temp: liquid takes priority over atmosphere (Frozen Planet / Lava Planet)
            base = switch (rp.liquidCoverage()) {
                case LOW -> "planet_wet.png";
                case MEDIUM -> "planet_boggy.png";
                case HIGH -> "planet_oceanic.png";
                default -> "planet_arid.png";
            };
            overlay = switch (rp.liquidCoverage()) {
                case LOW -> "planet_wet_overlay.png";
                case MEDIUM -> "planet_boggy_overlay.png";
                case HIGH -> "planet_oceanic_overlay.png";
                default -> null;
            };
            palette = temp == Temperature.FREEZING
                ? "planet_mix_color_freezing.png"
                : "planet_mix_color_scorched.png";
        } else if (!hasAtmos) {
            // No atmosphere: always atmosphereless (Deathly Frozen / Deathly Scorched / Deathly Planet)
            base = "planet_atmosphereless.png";
            palette = switch (temp) {
                case FREEZING -> "planet_mix_color_freezing.png";
                case SCORCHED -> "planet_mix_color_scorched.png";
                default -> "planet_atmosphereless_color.png";
            };
        } else if (hasLiquid) {
            // Has atmosphere + has liquid + mild temps (Frozen Riverbank / Warm Riverbank / … / Warm Ocean)
            base = switch (rp.liquidCoverage()) {
                case LOW -> "planet_wet.png";
                case MEDIUM -> "planet_boggy.png";
                case HIGH -> "planet_oceanic.png";
                default -> "planet_arid.png";
            };
            overlay = switch (rp.liquidCoverage()) {
                case LOW -> "planet_wet_overlay.png";
                case MEDIUM -> "planet_boggy_overlay.png";
                case HIGH -> "planet_oceanic_overlay.png";
                default -> null;
            };
            palette = "planet_mix_color.png";
        } else {
            // Has atmosphere, no liquid (Desolate Frozen / Desolate Scorched / Desert Planet)
            base = (temp == Temperature.FREEZING || temp == Temperature.SCORCHED)
                ? "planet_arid.png"
                : "planet_desert.png";
            palette = switch (temp) {
                case FREEZING -> "planet_mix_color_freezing.png";
                case SCORCHED -> "planet_mix_color_scorched.png";
                default -> "planet_arid_color.png";
            };
        }
        return new TexSet(base, overlay, palette);
    }

    private static TexSet resolveGiant(GiantPlanetData gp) {
        String palette = gp.bodyClass() == CelestialBodyClass.ICE_GIANT
            ? "planet_giant_color_1.png"
            : "planet_giant_color_0.png";
        return new TexSet("planet_giant.png",
            gp.windSpeed() == WindSpeed.VERY_HIGH ? "planet_giant_overlay_1.png" : "planet_giant_overlay_0.png",
            palette);
    }

    @Nullable
    private static ResourceLocation bakeBody(CelestialBodyData data, String key) {
        if (data instanceof SpecialCelestialBodyData special) return bakeSpecial(key, special);

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

    /**
     * Get the star color from the diagram-based StarData.
     */
    public static float[] starColor(StarData star) {
        return new float[]{
            star.colorR() / 255f,
            star.colorG() / 255f,
            star.colorB() / 255f
        };
    }

    /**
     * Bake a special celestial body — just load the PNG directly without palette coloring.
     */
    @Nullable
    private static ResourceLocation bakeSpecial(String key, SpecialCelestialBodyData special) {
        String filename = special.textureName() + ".png";
        NativeImage img = loadImage(filename);
        if (img == null) return null;
        return registerTexture(key, img);
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

        NativeImage paletteImg = loadImage("planet_giant_ring_color.png");
        if (paletteImg != null) {
            int ringPaletteRow = data instanceof RockyPlanetData rp ? rp.paletteBaseRow()
                : data instanceof GiantPlanetData gp ? gp.paletteBaseRow() : 0;
            NativeImage colored = PaletteColorMapper.colorTexture(ringImg, paletteImg, ringPaletteRow, true);
            paletteImg.close();
            ringImg.close();
            return registerTexture(key, colored);
        }

        ringImg.close();
        return null;
    }

    private static String cacheKey(CelestialBodyData data) {
        if (data instanceof SpecialCelestialBodyData s)
            return "special_" + s.name();
        if (data instanceof StarData s)
            return "star_" + s.size() + "_" + s.colorR() + "_" + s.colorG() + "_" + s.colorB();
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
