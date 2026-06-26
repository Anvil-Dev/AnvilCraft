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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

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
    /// 解析岩石行星的贴图集。
    ///
    /// 温度、液体覆盖率、大气层与贴图的对应关系：
    ///
    /// 冰冻（FREEZING）：
    /// - 无液体、无大气层 → planet_atmosphereless / planet_mix_color_freezing（致命冰冻 Deathly Frozen）
    /// - 无液体、有大气层 → planet_arid / planet_mix_color_freezing（冰冻荒原 Desolate Frozen）
    /// - 有液体 → wet/boggy/oceanic / planet_mix_color_freezing（冰冻行星 Frozen Planet）
    ///
    /// 灼热（SCORCHED）：
    /// - 无液体、无大气层 → planet_atmosphereless / planet_mix_color_scorched（致命灼热 Deathly Scorched）
    /// - 无液体、有大气层 → planet_arid / planet_mix_color_scorched（灼热荒原 Desolate Scorched）
    /// - 有液体 → wet/boggy/oceanic / planet_mix_color_scorched（熔岩行星 Lava Planet）
    ///
    /// 寒冷/温和/炎热（COLD/MILD/HOT）：
    /// - 无大气层 → planet_atmosphereless / planet_atmosphereless_color（致命行星 Deathly Planet）
    /// - 无液体、有大气层 → planet_desert / planet_arid_color（沙漠行星 Desert Planet）
    /// - 有液体、有大气层 → wet/boggy/oceanic / planet_mix_color（9种类型）
    private static TexSet resolveRocky(RockyPlanetData rp) {
        String base, overlay = null, palette;
        boolean hasAtmos = rp.hasAtmosphere();
        boolean hasLiquid = rp.liquidCoverage() != LiquidCoverage.NONE;
        Temperature temp = rp.temperature();

        if (hasLiquid && (temp == Temperature.FREEZING || temp == Temperature.SCORCHED)) {
            /// 极端温度：液体优先于大气层（冰冻行星 / 熔岩行星）
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
            /// 无大气层：一律使用无大气层贴图（致命冰冻 / 致命灼热 / 致命行星）
            base = "planet_atmosphereless.png";
            palette = switch (temp) {
                case FREEZING -> "planet_mix_color_freezing.png";
                case SCORCHED -> "planet_mix_color_scorched.png";
                default -> "planet_atmosphereless_color.png";
            };
        } else if (hasLiquid) {
            /// 有大气层、有液体、温度适中（冰冻河岸 / 温暖河岸 / … / 温暖海洋）
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
            /// 有大气层、无液体（冰冻荒原 / 灼热荒原 / 沙漠行星）
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

    /// 从图表式恒星数据获取颜色。
    public static float[] starColor(StarData star) {
        return new float[]{
            star.colorR() / 255f,
            star.colorG() / 255f,
            star.colorB() / 255f
        };
    }

    /// 烘焙特殊天体 —— 直接加载PNG贴图，不进行调色板着色。
    @Nullable
    private static ResourceLocation bakeSpecial(String key, SpecialCelestialBodyData special) {
        String filename = special.model() + ".png";
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
