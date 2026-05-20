package dev.dubhe.anvilcraft.client.init;

import dev.anvilcraft.lib.v2.rendering.extension.ALRRenderTypeExtension;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

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
            .createRenderSetup()
    );
}
