package dev.dubhe.anvilcraft.client.init;

import dev.anvilcraft.lib.v2.rendering.extension.ALRRenderTypeExtension;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

public class ModRenderTypes {

    // TODO: use colored overlay and uniform
    public static final RenderType TRANSLUCENT_COLORED_OVERLAY = RenderType.create(
        "anvilcraft:laser_translucent",
        RenderSetup.builder(ModRenderPipelines.COLORED_OVERLAY)
            .useLightmap()
            .sortOnUpload()
            .withTexture("Sampler0", ModTextureAtlases.LOCATION_LASER)
            .createRenderSetup()
    );

    public static final RenderType LINE_BLOOM = ALRRenderTypeExtension.copyWithBloom(RenderTypes.LINES);

    public static final RenderType LASER_TRANSLUCENT = RenderType.create(
        "anvilcraft:laser_translucent",
        RenderSetup.builder(ModRenderPipelines.LASER_TRANSLUCENT)
            .useLightmap()
            .sortOnUpload()
            .withTexture("Sampler0", ModTextureAtlases.LOCATION_LASER)
            .createRenderSetup()
    );

    public static final RenderType LASER_TRANSLUCENT_BLOOM = ALRRenderTypeExtension.copyWithBloom(LASER_TRANSLUCENT);

    public static final RenderType LASER_SOLID = RenderType.create(
        "anvilcraft:laser_solid",
        RenderSetup.builder(RenderPipelines.SOLID_BLOCK)
            .useLightmap()
            .withTexture("Sampler0", ModTextureAtlases.LOCATION_LASER)
            .createRenderSetup()
    );

    public static final RenderType LASER_SOLID_BLOOM = ALRRenderTypeExtension.copyWithBloom(LASER_SOLID);

    public static final RenderType LIGHTNING = RenderType.create(
        "anvilcraft:lightning",
        RenderSetup.builder(ModRenderPipelines.LIGHTNING)
            .affectsCrumbling()
            .useLightmap()
            .createRenderSetup()
    );

    /// 超新星爆发光束/闪光——加法混合发光。
    public static final RenderType SUPERNOVA_BEAM = RenderType.create(
        "anvilcraft:supernova_beam",
        RenderSetup.builder(ModRenderPipelines.SUPERNOVA_BEAM)
            .useLightmap()
            .withTexture("Sampler0", ModTextureAtlases.LOCATION_LASER)
            .createRenderSetup()
    );

    /// 托举光束 + 超新星放射光束共用（加法混合，顶点色）。
    public static final RenderType STELLAR_BEAM = SUPERNOVA_BEAM;

    /// 腐化信标暗色光束（普通半透明混合，顶点色，高不透明度呈"暗色遮挡"感）。
    public static final RenderType CORRUPTED_BEACON_BEAM = RenderType.create(
        "anvilcraft:corrupted_beacon_beam",
        RenderSetup.builder(ModRenderPipelines.CORRUPTED_BEACON_BEAM)
            .useLightmap()
            .withTexture("Sampler0", ModTextureAtlases.LOCATION_LASER)
            .createRenderSetup()
    );

    /// 纯白贴图，供颜色叠加/大气立方体按顶点色着色使用。
    /// 26.1 移除了原版 `minecraft:textures/misc/white.png`（1.21 有），故改用模组自带的白贴图，
    /// 否则恒星光晕/颜色叠加/大气立方体会因缺失贴图渲染成紫黑方块。
    private static final Identifier WHITE_TEXTURE = AnvilCraft.of("textures/misc/white.png");

    /// 恒星颜色叠加——乘法混合（DST_COLOR × SRC_COLOR），精确调色板着色。
    public static final RenderType STAR_COLOR_OVERLAY = RenderType.create(
        "anvilcraft:star_color_overlay",
        RenderSetup.builder(ModRenderPipelines.STAR_COLOR_OVERLAY)
            .useLightmap()
            .useOverlay()
            .withTexture("Sampler0", WHITE_TEXTURE)
            .createRenderSetup()
    );

    /// 天体大气层/光晕立方体——半透明混合，双面可见。
    public static final RenderType CELESTIAL_ATMOSPHERE = RenderType.create(
        "anvilcraft:celestial_atmosphere",
        RenderSetup.builder(ModRenderPipelines.CELESTIAL_ATMOSPHERE)
            .useLightmap()
            .useOverlay()
            .sortOnUpload()
            .withTexture("Sampler0", WHITE_TEXTURE)
            .createRenderSetup()
    );

    /// 天体贴图（cutout，按动态烘焙贴图）——行星本体。
    public static final Function<Identifier, RenderType> STAR_CUTOUT =
        Util.memoize((Identifier tex) -> RenderTypes.entityCutout(tex));

    /// 行星环（半透明，按动态烘焙贴图）。
    public static final Function<Identifier, RenderType> CELESTIAL_RING =
        Util.memoize((Identifier tex) -> RenderTypes.entityTranslucent(tex));

    /// 超新星闪光 billboard（加法混合，按逐帧贴图）。
    public static final Function<Identifier, RenderType> SUPERNOVA_FLASH = Util.memoize(
        tex -> RenderType.create(
            "anvilcraft:supernova_flash",
            RenderSetup.builder(ModRenderPipelines.SUPERNOVA_FLASH)
                .useLightmap()
                .useOverlay()
                .withTexture("Sampler0", tex)
                .createRenderSetup()
        )
    );
}
