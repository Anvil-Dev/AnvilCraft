package dev.dubhe.anvilcraft.client.init;

import dev.anvilcraft.lib.v2.rendering.extension.ALRRenderTypeExtension;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

public class ModRenderTypes {

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

    public static final RenderType LIGHTNING = RenderType.create(
        "anvilcraft:lightning",
        RenderSetup.builder(ModRenderPipelines.LIGHTNING)
            .affectsCrumbling()
            .useLightmap()
            .createRenderSetup()
    );

    public static final RenderType SUPERNOVA_BEAM = RenderType.create(
        "anvilcraft:supernova_beam",
        RenderSetup.builder(ModRenderPipelines.SUPERNOVA_BEAM)
            .useLightmap()
            .withTexture("Sampler0", ModTextureAtlases.LOCATION_LASER)
            .createRenderSetup()
    );

    public static final RenderType STELLAR_BEAM = RenderType.create(
        "anvilcraft:stellar_beam",
        RenderSetup.builder(ModRenderPipelines.STELLAR_BEAM)
            .sortOnUpload()
            .createRenderSetup()
    );

    public static final RenderType CORRUPTED_BEACON_BEAM = RenderType.create(
            "anvilcraft:corrupted_beacon_beam",
            RenderSetup.builder(ModRenderPipelines.CORRUPTED_BEACON_BEAM)
                    .sortOnUpload()
                    .createRenderSetup()
    );

    private static final Identifier WHITE_TEXTURE = AnvilCraft.of("textures/misc/white.png");

    public static final RenderType STAR_COLOR_OVERLAY = RenderType.create(
            "anvilcraft:star_color_overlay",
            RenderSetup.builder(ModRenderPipelines.STAR_COLOR_OVERLAY)
                    .useLightmap()
                    .useOverlay()
                    .withTexture("Sampler0", WHITE_TEXTURE)
                    .createRenderSetup()
    );

    public static final RenderType CELESTIAL_ATMOSPHERE = RenderType.create(
            "anvilcraft:celestial_atmosphere",
            RenderSetup.builder(ModRenderPipelines.CELESTIAL_ATMOSPHERE)
                    .useLightmap()
                    .useOverlay()
                    .sortOnUpload()
                    .withTexture("Sampler0", WHITE_TEXTURE)
                    .createRenderSetup()
    );

    public static final Function<Identifier, RenderType> STAR_CUTOUT =
            Util.memoize((Identifier tex) -> RenderTypes.entityCutout(tex));

    /**
     * 天体环使用独立的方块半透明管线，以保持与 1.21 相同的深度和混合行为。
     * 环的正反面使用贴图中的不同象限，因此保持双面渲染。
     */
    public static final Function<Identifier, RenderType> CELESTIAL_RING = Util.memoize(
            tex -> RenderType.create(
                    "anvilcraft:celestial_ring",
                    RenderSetup.builder(ModRenderPipelines.CELESTIAL_RING)
                            .withTexture("Sampler0", tex)
                            .useLightmap()
                            .useOverlay()
                            .affectsCrumbling()
                            .createRenderSetup()
            )
    );

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

    public static final Function<Identifier, RenderType> CUTOUT_NO_LIGHTING = Util.memoize(
            tex -> RenderType.create(
                    "anvilcraft:cutout_no_lighting",
                    RenderSetup.builder(RenderPipelines.CUTOUT_BLOCK)
                            .withTexture("Sampler0", tex)
                            .useLightmap()
                            .createRenderSetup()
            )
    );

    /**
     * 使用方块图集并保留半透明混合的模型渲染类型。
     * 配合全亮光照值和显式模型提交使用，可绕过方块模型的环境光遮蔽。
     */
    public static final Function<Identifier, RenderType> TRANSLUCENT_NO_LIGHTING = Util.memoize(
            tex -> RenderType.create(
                    "anvilcraft:translucent_no_lighting",
                    RenderSetup.builder(RenderPipelines.TRANSLUCENT_BLOCK)
                            .withTexture("Sampler0", tex)
                            .useLightmap()
                            .sortOnUpload()
                            .createRenderSetup()
            )
    );

    public static final RenderType CUTOUT_BLOCK = CUTOUT_NO_LIGHTING.apply(Sheets.BLOCKS_MAPPER.sheet());
    public static final RenderType TRANSLUCENT_BLOCK = TRANSLUCENT_NO_LIGHTING.apply(Sheets.BLOCKS_MAPPER.sheet());
}
