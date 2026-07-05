package dev.dubhe.anvilcraft.client.init;

import dev.anvilcraft.lib.v2.rendering.extension.ALRRenderTypeExtension;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

public class ModRenderTypes {

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

    public static final RenderType SUPERNOVA_BEAM = RenderType.create(
        "anvilcraft:supernova_beam",
        RenderSetup.builder(ModRenderPipelines.SUPERNOVA_BEAM)
            .useLightmap()
            .withTexture("Sampler0", ModTextureAtlases.LOCATION_LASER)
            .createRenderSetup()
    );

    public static final RenderType STELLAR_BEAM = SUPERNOVA_BEAM;

    public static final RenderType CORRUPTED_BEACON_BEAM = RenderType.create(
        "anvilcraft:corrupted_beacon_beam",
        RenderSetup.builder(ModRenderPipelines.CORRUPTED_BEACON_BEAM)
            .useLightmap()
            .withTexture("Sampler0", ModTextureAtlases.LOCATION_LASER)
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

    public static final Function<Identifier, RenderType> CELESTIAL_RING =
        Util.memoize((Identifier tex) -> RenderTypes.entityTranslucent(tex));

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

    public static final RenderType CUTOUT_BLOCK = CUTOUT_NO_LIGHTING.apply(TextureAtlas.LOCATION_BLOCKS);
}
